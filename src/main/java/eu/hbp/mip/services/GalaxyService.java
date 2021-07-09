package eu.hbp.mip.services;

import com.github.jmchilton.blend4j.galaxy.GalaxyInstance;
import com.github.jmchilton.blend4j.galaxy.GalaxyInstanceFactory;
import com.github.jmchilton.blend4j.galaxy.WorkflowsClient;
import com.github.jmchilton.blend4j.galaxy.beans.Workflow;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowDetails;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowInputDefinition;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.LinkedTreeMap;
import eu.hbp.mip.controllers.galaxy.retrofit.RetroFitGalaxyClients;
import eu.hbp.mip.controllers.galaxy.retrofit.RetrofitClientInstance;
import eu.hbp.mip.models.DAOs.ExperimentDAO;
import eu.hbp.mip.models.DTOs.ExaremeAlgorithmRequestParamDTO;
import eu.hbp.mip.models.DTOs.ExperimentDTO;
import eu.hbp.mip.models.galaxy.GalaxyWorkflowResult;
import eu.hbp.mip.models.galaxy.PostWorkflowToGalaxyDtoResponse;
import eu.hbp.mip.repositories.ExperimentRepository;
import eu.hbp.mip.utils.Exceptions.BadRequestException;
import eu.hbp.mip.utils.Exceptions.InternalServerError;
import eu.hbp.mip.utils.JsonConverters;
import eu.hbp.mip.utils.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.*;

import static java.lang.Thread.sleep;

@Service
public class GalaxyService {

    private final ActiveUserService activeUserService;
    private final ExperimentRepository experimentRepository;
    public GalaxyService(
            ActiveUserService activeUserService,
            ExperimentRepository experimentRepository
    ) {
        this.activeUserService = activeUserService;
        this.experimentRepository = experimentRepository;
    }
    @Value("#{'${services.galaxy.galaxyUrl}'}")
    private String galaxyUrl;

    @Value("#{'${services.galaxy.galaxyApiKey}'}")
    private String galaxyApiKey;

    private static final Gson gson = new Gson();

    /**
     * The runWorkflow will POST the algorithm to the galaxy client
     *
     * @param experimentDTO is the request with the experiment information
     * @return the response to be returned
     */
    public ExperimentDTO runGalaxyWorkflow(ExperimentDTO experimentDTO, Logger logger) {
        logger.LogUserAction("Running a workflow...");

        ExperimentDAO experimentDAO = experimentRepository.createExperimentInTheDatabase(experimentDTO, activeUserService.getActiveUser(), logger);
        logger.LogUserAction("Created experiment with uuid :" + experimentDAO.getUuid());


        // Run the 1st algorithm from the list
        String workflowId = experimentDTO.getAlgorithm().getName();

        // Get the parameters
        List<ExaremeAlgorithmRequestParamDTO> algorithmParameters
                = experimentDTO.getAlgorithm().getParameters();

        // Convert the parameters to workflow parameters
        HashMap<String, String> algorithmParamsIncludingEmpty = new HashMap<>();
        if (algorithmParameters != null) {
            for (ExaremeAlgorithmRequestParamDTO param : algorithmParameters) {
                algorithmParamsIncludingEmpty.put(param.getName(), param.getValue());
            }
        }

        // Get all the algorithm parameters because the frontend provides only the non-null
        final GalaxyInstance instance = GalaxyInstanceFactory.get(galaxyUrl, galaxyApiKey);
        final WorkflowsClient workflowsClient = instance.getWorkflowsClient();
        Workflow workflow = null;
        for (Workflow curWorkflow : workflowsClient.getWorkflows()) {
            if (curWorkflow.getId().equals(workflowId)) {
                workflow = curWorkflow;
                break;
            }
        }
        if (workflow == null) {
            logger.LogUserAction("Could not find algorithm code: " + workflowId);
            throw new BadRequestException("Could not find galaxy algorithm.");
        }
        final WorkflowDetails workflowDetails = workflowsClient.showWorkflow(workflow.getId());
        for (Map.Entry<String, WorkflowInputDefinition> workflowParameter : workflowDetails.getInputs().entrySet()) {
            if (!(algorithmParamsIncludingEmpty.containsKey(workflowParameter.getValue().getUuid()))) {
                algorithmParamsIncludingEmpty.put(workflowParameter.getValue().getUuid(), "");
            }
        }

        // Create the body of the request
        HashMap<String, HashMap<String, String>> requestBody = new HashMap<>();
        requestBody.put("inputs", algorithmParamsIncludingEmpty);
        JsonObject requestBodyJson = new JsonParser().parse(gson.toJson(requestBody)).getAsJsonObject();

        // Create the request client
        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        logger.LogUserAction("Running Galaxy workflow with id: " + workflow.getId());

        // Call Galaxy to run the workflow
        Call<PostWorkflowToGalaxyDtoResponse> call = service.postWorkflowToGalaxy(workflow.getId(), galaxyApiKey, requestBodyJson);
        try {
            Response<PostWorkflowToGalaxyDtoResponse> response = call.execute();

            if (response.code() == 200) {       // Call succeeded
                String responseBody = gson.toJson(response.body());
                logger.LogUserAction("Response: " + responseBody);

                String historyId = (String) new JSONObject(responseBody).get("history_id");
                experimentDAO.setWorkflowHistoryId(historyId);
                experimentDAO.setStatus(ExperimentDAO.Status.success);

            } else {     // Something unexpected happened
                String msgErr = gson.toJson(response.errorBody());
                logger.LogUserAction("Error Response: " + msgErr);
                experimentDTO.setStatus((response.code() >= 400) ? ExperimentDAO.Status.error : ExperimentDAO.Status.success);
            }

        } catch (Exception e) {
            logger.LogUserAction("An exception occurred: " + e.getMessage());
            experimentDAO.setStatus(ExperimentDAO.Status.error);
        }
        experimentRepository.saveExperiment(experimentDAO, logger);

        // Start the process of fetching the status
        updateWorkflowExperiment(experimentDAO, logger);

        logger.LogUserAction("Run workflow completed!");

        experimentDTO = new ExperimentDTO(true, experimentDAO);
        return experimentDTO;
    }



    /**
     * @param experimentDAO The experiment of the workflow
     * @return "pending"           ->      When the workflow is still running
     * "internalError"     ->      When an exception or a bad request occurred
     * "error"             ->      When the workflow produced an error
     * "success"         ->      When the workflow completed successfully
     */
    public String getWorkflowStatus(ExperimentDAO experimentDAO) {
        String historyId = experimentDAO.getWorkflowHistoryId();
        String experimentName = experimentDAO.getName();
        UUID experimentId = experimentDAO.getUuid();

        // ATTENTION: This function is used from a Thread. Only LogExperimentAction should be used, not LogUserAction!
        Logger.LogExperimentAction(experimentName, experimentId, " History Id : " + historyId);

        // Create the request client
        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<Object> call = service.getWorkflowStatusFromGalaxy(historyId, galaxyApiKey);

        String result;
        try {
            Response<Object> response = call.execute();
            if (response.code() >= 400) {
                Logger.LogExperimentAction(experimentName, experimentId, " Response code: "
                        + response.code() + "" + " with body: " + (response.errorBody() != null ? response.errorBody().string() : " "));
                return "internalError";
            }
            result = new Gson().toJson(response.body());
            Logger.LogExperimentAction(experimentName, experimentId, " ResultDTO: " + result);

        } catch (IOException e) {
            Logger.LogExperimentAction(experimentName, experimentId, " An exception happened: " + e.getMessage());
            return "internalError";
        }

        String state;
        try {
            JSONObject resultJson = new JSONObject(result);
            state = resultJson.getString("state");
        } catch (JSONException e) {
            Logger.LogExperimentAction(experimentName, experimentId, " An exception happened: " + e.getMessage());
            return "internalError";
        }

        Logger.LogExperimentAction(experimentName, experimentId, " Completed!");
        switch (state) {
            case "ok":
                return "success";
            case "error":
                return "error";
            case "pending":
            case "new":
            case "waiting":
            case "queued":
                return "pending";
            default:
                return "internalError";
        }
    }

    /**
     * @param experimentDAO The experiment of the workflow
     * @return a List<GalaxyWorkflowResult>   or null when an error occurred
     */
    public List<GalaxyWorkflowResult> getWorkflowResults(ExperimentDAO experimentDAO) {

        String historyId = experimentDAO.getWorkflowHistoryId();
        String experimentName = experimentDAO.getName();
        UUID experimentId = experimentDAO.getUuid();
        Logger.LogExperimentAction(experimentName, experimentId, " historyId : " + historyId);

        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<List<GalaxyWorkflowResult>> call = service.getWorkflowResultsFromGalaxy(historyId, galaxyApiKey);

        List<GalaxyWorkflowResult> getGalaxyWorkflowResultList;
        try {
            Response<List<GalaxyWorkflowResult>> response = call.execute();
            if (response.code() >= 400) {
                Logger.LogExperimentAction(experimentName, experimentId, " Response code: "
                        + response.code() + "" + " with body: " + (response.errorBody() != null ? response.errorBody().string() : " "));
                return null;
            }
            getGalaxyWorkflowResultList = response.body();
            Logger.LogExperimentAction(experimentName, experimentId, " ResultDTO: " + response.body());

        } catch (IOException e) {
            Logger.LogExperimentAction(experimentName, experimentId, " An exception happened: " + e.getMessage());
            return null;
        }

        Logger.LogExperimentAction(experimentName, experimentId, " Completed!");
        return getGalaxyWorkflowResultList;

    }

    /**
     * @param experimentDAO The experiment of the workflow
     * @param contentId     the id of the job result that we want
     * @return the result of the specific workflow job, null if there was an error
     */
    public String getWorkflowResultBody(ExperimentDAO experimentDAO, String contentId) {

        String historyId = experimentDAO.getWorkflowHistoryId();
        String experimentName = experimentDAO.getName();
        UUID experimentId = experimentDAO.getUuid();

        Logger.LogExperimentAction(experimentName, experimentId, " historyId : " + historyId);

        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<Object> call =
                service.getWorkflowResultsBodyFromGalaxy(historyId, contentId, galaxyApiKey);

        String resultJson;
        try {
            Response<Object> response = call.execute();
            if (response.code() >= 400) {
                Logger.LogExperimentAction(experimentName, experimentId, " Response code: "
                        + response.code() + "" + " with body: " + (response.errorBody() != null ? response.errorBody().string() : " "));
                return null;
            }
            resultJson = new Gson().toJson(response.body());
            Logger.LogExperimentAction(experimentName, experimentId, " ResultDTO: " + resultJson);

        } catch (IOException e) {
            Logger.LogExperimentAction(experimentName, experimentId,
                    " An exception happened: " + e.getMessage());
            return null;
        }

        Logger.LogExperimentAction(experimentName, experimentId, " Completed!");
        return formattingGalaxyResult(resultJson);
    }

    private String formattingGalaxyResult(String result) {
        List<LinkedTreeMap<String,Object>> jsonObject = JsonConverters.convertJsonStringToObject(result, new ArrayList<ArrayList<Object>>().getClass());
        LinkedTreeMap<String,Object> firstResult = jsonObject.get(0);
        jsonObject = (List<LinkedTreeMap<String, Object>>) firstResult.get("result");
        List<LinkedTreeMap<String,Object>> finalJsonObject = new ArrayList<>();
        finalJsonObject.add(jsonObject.get(0));
        return JsonConverters.convertObjectToJsonString(finalJsonObject);
    }


    /**
     * @param jobId the id of the workflow job that failed
     * @return the error that was produced or null if an error occurred
     */
    public String getWorkflowJobError(String jobId, ExperimentDAO experimentDAO) {
        String experimentName = experimentDAO.getName();
        UUID experimentId = experimentDAO.getUuid();

        Logger.LogExperimentAction(experimentName, experimentId, " jobId : " + jobId);
        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<Object> callError = service.getErrorMessageOfWorkflowFromGalaxy(jobId, galaxyApiKey);

        String fullError;
        String returnError;
        try {
            Response<Object> response = callError.execute();
            if (response.code() >= 400) {
                Logger.LogExperimentAction(experimentName, experimentId, "Response code: "
                        + response.code() + " with body: " + (response.errorBody() != null ? response.errorBody().string() : " "));
                return null;
            }

            // Parsing the stderr of the job that failed
            String jsonString = new Gson().toJson(response.body());
            JsonElement jsonElement = new JsonParser().parse(jsonString);
            JsonObject rootObject = jsonElement.getAsJsonObject();
            fullError = rootObject.get("stderr").getAsString();
            Logger.LogExperimentAction(experimentName, experimentId, "Error: " + fullError);

            String[] arrOfStr = fullError.split("ValueError", 0);
            String specError = arrOfStr[arrOfStr.length - 1];
            returnError = specError.substring(1);
            Logger.LogExperimentAction(experimentName, experimentId, "Parsed Error: " + returnError);

        } catch (IOException e) {
            Logger.LogExperimentAction(experimentName, experimentId, "Exception: " + e.getMessage());
            return null;
        }

        Logger.LogExperimentAction(experimentName, experimentId, "Completed successfully!");

        return returnError;
    }

    /**
     * This method creates a thread that will fetch the workflow result when it is ready
     *
     * @param experimentDAO will be used to fetch it's workflow status, it should have the workflowHistoryId initialized
     *                      and the result should not already be fetched
     */
    public void updateWorkflowExperiment(ExperimentDAO experimentDAO, Logger logger) {

        if (experimentDAO == null) {
            logger.LogUserAction("The experiment does not exist.");
            return;
        }

        logger.LogUserAction(" Experiment id : " + experimentDAO.getUuid());
        if (experimentDAO.getWorkflowHistoryId() == null) {
            logger.LogUserAction("History Id does not exist.");
            return;
        }

        logger.LogUserAction("Starting Thread...");
        new Thread(() -> {
            while (true) {
                // ATTENTION: Inside the Thread only LogExperimentAction should be used, not LogExperimentAction!
                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Thread is running...");

                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Sleep was disrupted: " + e.getMessage());
                    throw new InternalServerError(e.getMessage());
                }

                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Fetching status for experiment Id: " + experimentDAO.getUuid());

                String state = getWorkflowStatus(experimentDAO);
                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "State is: " + state);

                switch (state) {
                    case "pending":
                        // Do nothing, when the experiment is created the status is set to running
                        Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Workflow is still running.");
                        break;

                    case "success":
                        // Get only the job result that is visible
                        List<GalaxyWorkflowResult> workflowJobsResults = getWorkflowResults(experimentDAO);
                        Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Results are: " + workflowJobsResults.toString());

                        boolean resultFound = false;
                        for (GalaxyWorkflowResult jobResult : workflowJobsResults) {
                            if (jobResult.getVisible()) {
                                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Visible result are: " + jobResult.getId());

                                String result = getWorkflowResultBody(experimentDAO, jobResult.getId());

                                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "ResultDTO: " + result);
                                if (result == null) {
                                    experimentDAO.setStatus(ExperimentDAO.Status.error);
                                } else {
                                    experimentDAO.setResult(result);
                                    experimentDAO.setStatus(ExperimentDAO.Status.success);
                                    resultFound = true;
                                }
                            }
                        }

                        if (!resultFound) {      // If there is no visible result
                            Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "No visible result");
                            experimentDAO.setStatus(ExperimentDAO.Status.error);
                        }

                        experimentRepository.finishExperiment(experimentDAO, logger);
                        break;

                    case "error":
                        // Get the job result that failed
                        workflowJobsResults = getWorkflowResults(experimentDAO);
                        Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Error results are: " + workflowJobsResults.toString());

                        boolean failedJobFound = false;
                        for (GalaxyWorkflowResult jobResult : workflowJobsResults) {
                            if (jobResult.getState().equals("error")) {
                                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Failed job is: " + jobResult.getId());

                                String result = getWorkflowJobError(jobResult.getId(), experimentDAO);

                                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Job result: " + result);
                                if (result == null) {
                                    experimentDAO.setStatus(ExperimentDAO.Status.error);
                                }
                                experimentDAO.setStatus(ExperimentDAO.Status.error);
                                failedJobFound = true;
                            }
                        }

                        if (!failedJobFound) {      // If there is no visible failed job
                            Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "No failed result");
                            experimentDAO.setStatus(ExperimentDAO.Status.error);
                        }
                        experimentRepository.finishExperiment(experimentDAO, logger);
                        break;

                    default:        // InternalError or unexpected result
                        Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "An unexpected error occurred.");
                        experimentDAO.setStatus(ExperimentDAO.Status.error);
                        experimentRepository.finishExperiment(experimentDAO, logger);
                        break;
                }

                // If result exists return
                if (experimentDAO.getResult() != null) {
                    Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "ResultDTO exists: " + experimentDAO.getResult());
                    return;
                }
            }
        }).start();
    }
}
