package eu.hbp.mip.controllers;

import com.github.jmchilton.blend4j.galaxy.GalaxyInstance;
import com.github.jmchilton.blend4j.galaxy.GalaxyInstanceFactory;
import com.github.jmchilton.blend4j.galaxy.WorkflowsClient;
import com.github.jmchilton.blend4j.galaxy.beans.Workflow;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowDetails;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.hbp.mip.controllers.retrofit.RetroFitGalaxyClients;
import eu.hbp.mip.controllers.retrofit.RetrofitClientInstance;
import eu.hbp.mip.dto.ErrorResponse;
import eu.hbp.mip.dto.GetWorkflowResultsFromGalaxyDtoResponse;
import eu.hbp.mip.dto.PostWorkflowToGalaxyDtoResponse;
import eu.hbp.mip.dto.StringDtoResponse;
import eu.hbp.mip.helpers.GenParameters;
import eu.hbp.mip.helpers.LogHelper;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/galaxyApi/")
class GalaxyAPI {

    private static final Logger logger = LoggerFactory.getLogger(GalaxyAPI.class);

    //The galaxy URL
    private final String url = GenParameters.getGenParamInstance().getGalaxyURL();

    //The galaxy ApiKey
    private final String apiKey = GenParameters.getGenParamInstance().getGalaxyApiKey();

    /**
     * Test if integration works.
     *
     * @return Return a @{@link ResponseEntity}.
     */
    @RequestMapping(method = RequestMethod.GET, value = "getTestIntegration", produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity getTestIntegration(@AuthenticationPrincipal UserDetails userDetails){
        logger.info(LogHelper.logUser(userDetails) + "Get Test Integration called");
        return ResponseEntity.ok(new StringDtoResponse("success"));
    }

    /**
     * Get Galaxy Reverse Proxy basic access token.
     *
     * @return Return a @{@link ResponseEntity} with the token.
     */
    @RequestMapping(method = RequestMethod.GET, value = "getGalaxyBasicAccessToken", produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity getGalaxyBasicAccessToken(@AuthenticationPrincipal UserDetails userDetails){
        logger.info(LogHelper.logUser(userDetails) + "Get Galaxy Basic Access Token called");
        String username = GenParameters.getGenParamInstance().getGalaxyReverseProxyUsername();
        String password = GenParameters.getGenParamInstance().getGalaxyReverseProxyPassword();

        String stringEncoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        logger.info(LogHelper.logUser(userDetails) + "Get Galaxy Basic Access Token completed");
        return ResponseEntity.ok(username + ":" + password);
        //        return ResponseEntity.ok(new StringDtoResponse(stringEncoded));
    }

    /**
     * Get all the workflows with few details.
     *
     * @return Return a @{@link ResponseEntity}.
     */
    @RequestMapping(method = RequestMethod.GET, value = "getWorkflows", produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity getWorkflows(@AuthenticationPrincipal UserDetails userDetails) {
        logger.info(LogHelper.logUser(userDetails) + "Get workflows called");

        final GalaxyInstance instance = GalaxyInstanceFactory.get(url, apiKey);
        final WorkflowsClient workflowsClient = instance.getWorkflowsClient();

        ArrayList<Workflow> workflowArrayList = new ArrayList<>();
        workflowArrayList.addAll(workflowsClient.getWorkflows());
        logger.info(LogHelper.logUser(userDetails) + "Get workflows completed");

        return ResponseEntity.ok(workflowArrayList);
    }

    /**
     * Get details for a specific workflow.
     *
     * @param id : The id as @{@link String} for the specific workflow.
     * @return Return a @{@link ResponseEntity}.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/getDetailWorkflow/{id}", produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity getDetailWorkflow(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String id) {
        logger.info(LogHelper.logUser(userDetails) + "Get detail workflow called");

        final GalaxyInstance instance = GalaxyInstanceFactory.get(url, apiKey);
        final WorkflowsClient workflowsClient = instance.getWorkflowsClient();

        Workflow matchingWorkflow = null;
        for(Workflow workflow : workflowsClient.getWorkflows()) {
            if(workflow.getId().equals(id)) {
                matchingWorkflow = workflow;
            }
        }
        if(matchingWorkflow == null){
            logger.error(LogHelper.logUser(userDetails) + "Get detail workflow could not find workflow with id : " + id);
            return ResponseEntity.notFound().build();
        }
        final WorkflowDetails workflowDetails = workflowsClient.showWorkflow(matchingWorkflow.getId());
        logger.info(LogHelper.logUser(userDetails) + "Get detail workflow completed");

        return ResponseEntity.ok(workflowDetails);
    }

    /**
     * Get all the workflows with details.
     *
     * @return Return a @{@link ResponseEntity}.
     */
    @RequestMapping(method = RequestMethod.GET, value = "getAllWorkflowWithDetails", produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity getAllWorkflowWithDetails(@AuthenticationPrincipal UserDetails userDetails) {
        logger.info(LogHelper.logUser(userDetails) + "Get all workflow with details called");

        final GalaxyInstance instance = GalaxyInstanceFactory.get(url, apiKey);
        final WorkflowsClient workflowsClient = instance.getWorkflowsClient();

        List<Workflow> workflowList = new ArrayList<Workflow>();
        for(Workflow workflow : workflowsClient.getWorkflows()) {
            workflowList.add(workflow);
        }

        List<WorkflowDetails> workflowDetailsList = new ArrayList<>();
        for(Workflow workflow : workflowList){
            workflowDetailsList.add(workflowsClient.showWorkflow(workflow.getId()));
        }
        logger.info(LogHelper.logUser(userDetails) + "Get all workflow with details completed");

        return ResponseEntity.ok(workflowDetailsList);
    }

    /**
     * Invoke a workflow.
     *
     * @param id : The id as @{@link String} of the workflow.
     * @param httpEntity : The @{@link HttpEntity} to get the body of the request which is the parameter of the workflow.
     * @return Return a @{@link ResponseEntity}.
     */
    @RequestMapping(method = RequestMethod.POST, value = "/runWorkflow/{id}", produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity runWorkflow(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String id, HttpEntity<String> httpEntity) {
        logger.info(LogHelper.logUser(userDetails) + "Run workflow called");

        //In order to parse Json with undefined number of value/key
        String json = httpEntity.getBody();
        JSONObject jObject  = null;
        try {
            jObject = new JSONObject(json);
        } catch (JSONException e) {
            logger.error(LogHelper.logUser(userDetails) + "Cannot parse JSON", e);
        }
        Map<String,String> allJsonParams = new HashMap<String,String>();
        Iterator iter = jObject.keys();
        while(iter.hasNext()){
            String key = (String)iter.next();
            String value = null;
            try {
                value = jObject.getString(key);
            } catch (JSONException e) {
                logger.error(LogHelper.logUser(userDetails) + "Cannot parse JSON", e);
            }
            logger.info(LogHelper.logUser(userDetails) + "Put to map: " + key + " : " + value);
            allJsonParams.put(key,value);
        }

        StringBuffer stringBuffer = new StringBuffer("{\n" +
                "\t\"inputs\": {\n");
        for (Map.Entry<String, String> entry : allJsonParams.entrySet()) {
            stringBuffer.append("\t\t\"" + entry.getKey() + "\" " + " : \"" + entry.getValue() + "\",\n");
            logger.debug(LogHelper.logUser(userDetails) + entry.getKey() + "/" + entry.getValue());
        }
        //Remove Last Comma
        stringBuffer.deleteCharAt(stringBuffer.length() - 2);
        stringBuffer.append("\t}\n");
        stringBuffer.append("}");
        logger.info(LogHelper.logUser(userDetails) + stringBuffer.toString());

        JsonObject jsonObject = new JsonParser().parse(stringBuffer.toString()).getAsJsonObject();

        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<PostWorkflowToGalaxyDtoResponse> call = service.postWorkflowToGalaxy(id, apiKey, jsonObject);

        PostWorkflowToGalaxyDtoResponse postWorkflowToGalaxyDtoResponse = null;
        try {
            Response<PostWorkflowToGalaxyDtoResponse> response = call.execute();
            if(response.code() >= 400){
                //Value are read it from streams.
                Integer codeErr = response.code();
                String msgErr = response.errorBody().string();
                logger.error(LogHelper.logUser(userDetails) + "Resonse code: " + codeErr + "" + " with body: " + msgErr);
                logger.info("---" + msgErr);
                JSONObject jObjectError  = null;
                try {
                    jObjectError = new JSONObject(msgErr);
                } catch (JSONException e) {
                    logger.error(LogHelper.logUser(userDetails) + "Cannot parse Error JSON", e);
                }
                logger.info(jObjectError.toString());
                String errMsg = jObjectError.get("err_msg").toString();
                String errCode = jObjectError.get("err_code").toString();

                response.errorBody();
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse(errMsg,errCode));
            }
            postWorkflowToGalaxyDtoResponse = response.body();
            logger.info(LogHelper.logUser(userDetails) + "----" + response.body() + "----" + response.code());
        } catch (IOException e) {
            logger.error(LogHelper.logUser(userDetails) + "Cannot make the call to Galaxy API", e);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("An error has been occurred","99"));
        } catch (JSONException e) {
            logger.error(LogHelper.logUser(userDetails) + "Cannot find field in Error Json", e);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("An error has been occurred","99"));
        }
        logger.info(LogHelper.logUser(userDetails) + "Run workflow completed");

        return ResponseEntity.ok(postWorkflowToGalaxyDtoResponse);
    }

    /**
     * Get the status of a specific workflow.
     *
     * @param id : The id as @{@link String} of the workflow.
     * @return Return a @{@link ResponseEntity}.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/getWorkflowStatus/{id}", produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity getWorkflowStatus(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String id) {
        logger.info(LogHelper.logUser(userDetails) + "Get workflow status called");

        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<Object> call = service.getWorkflowStatusFromGalaxy(id,apiKey);

        String jsonString = null;
        try {
            Response<Object> response = call.execute();
            if(response.code() >= 400){
                logger.error(LogHelper.logUser(userDetails) + "Resonse code: " + response.code() + "" + " with body: " + response.errorBody().string());
                return ResponseEntity.badRequest().build();
            }
            jsonString = new Gson().toJson(response.body());
            logger.info(LogHelper.logUser(userDetails) + jsonString);

            logger.info(LogHelper.logUser(userDetails) + "----" + response.body() + "----" + response.code());
        } catch (IOException e) {
            logger.error(LogHelper.logUser(userDetails) + "Cannot make the call to Galaxy API", e);
        }
        logger.info(LogHelper.logUser(userDetails) + "Get workflow status completed");

        return ResponseEntity.ok(jsonString);
    }

    /**
     * Get the status of a multiple workflows.
     * @param httpEntity : The @{@link HttpEntity} to get the body of the request which is the workflow ID.
     * @return Return a @{@link ResponseEntity}.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/getMultipleWorkflowStatus", produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity getMultipleWorkflowStatus(@AuthenticationPrincipal UserDetails userDetails, HttpEntity<String> httpEntity) {
        logger.info(LogHelper.logUser(userDetails) + "Get Multiple workflow status called");

        //In order to parse Json with undefined number of value/key
        String json = httpEntity.getBody();
        JSONObject jObject  = null;
        try {
            jObject = new JSONObject(json);
        } catch (JSONException e) {
            logger.error(LogHelper.logUser(userDetails) + "Cannot parse JSON", e);
        }
        Map<String,String> allJsonParams = new HashMap<String,String>();
        Iterator iter = jObject.keys();

        while(iter.hasNext()){
            String key = (String)iter.next();
            String value = null;
            try {
                value = jObject.getString(key);
                System.out.println(key + ":" + value);
            } catch (JSONException e) {
                logger.error(LogHelper.logUser(userDetails) + "Cannot parse JSON", e);
            }
            logger.info(LogHelper.logUser(userDetails) + "Put to map: " + key + " : " + value);
            allJsonParams.put(key,value);
        }

        StringBuffer stringBuffer = new StringBuffer();
        for (Map.Entry<String, String> entry : allJsonParams.entrySet()) {
            RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
            Call<Object> call = service.getWorkflowStatusFromGalaxy(entry.getValue(),apiKey);

            String jsonString = null;
            try {
                Response<Object> response = call.execute();
                if(response.code() >= 400){
                    logger.error(LogHelper.logUser(userDetails) + "Resonse code: " + response.code() + "" + " with body: " + response.errorBody().string());
                    return ResponseEntity.badRequest().build();
                }
                jsonString = new Gson().toJson(response.body());
                logger.info(LogHelper.logUser(userDetails) + jsonString);
                stringBuffer.append(jsonString + ",");
                logger.info(LogHelper.logUser(userDetails) + "----" + response.body() + "----" + response.code());
            } catch (IOException e) {
                logger.error(LogHelper.logUser(userDetails) + "Cannot make the call to Galaxy API", e);
            }
        }
        stringBuffer.deleteCharAt(stringBuffer.length() - 1);
        logger.info(LogHelper.logUser(userDetails) + "Get Multiple workflow status completed");
        return ResponseEntity.ok(stringBuffer.toString());
    }

    /**
     * Get the result of a specific workflow.
     *
     * @param id : The id as @{@link String} of the workflow.
     * @return Return a @{@link ResponseEntity}.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/getWorkflowResults/{id}", produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity getWorkflowResults(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String id) {
        logger.info(LogHelper.logUser(userDetails) + "Get workflow results called");

        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<List<GetWorkflowResultsFromGalaxyDtoResponse>> call = service.getWorkflowResultsFromGalaxy(id,apiKey);

        List<GetWorkflowResultsFromGalaxyDtoResponse> getWorkflowResultsFromGalaxyDtoResponsesList = null;
        try {
            Response<List<GetWorkflowResultsFromGalaxyDtoResponse>> response = call.execute();
            if(response.code() >= 400){
                logger.error(LogHelper.logUser(userDetails) + "Resonse code: " + response.code() + "" + " with body: " + response.errorBody().string());
                return ResponseEntity.badRequest().build();
            }
            getWorkflowResultsFromGalaxyDtoResponsesList = response.body();
        } catch (IOException e) {
            logger.error(LogHelper.logUser(userDetails) + "Cannot make the call to Galaxy API", e);
        }
        logger.info(LogHelper.logUser(userDetails) + "Get workflow results completed");

        return ResponseEntity.ok(getWorkflowResultsFromGalaxyDtoResponsesList);
    }

    /**
     * Get the result body of a specific workflow.
     *
     * @param id : The id as @{@link String} of the workflow.
     * @param contentId : The content id as @{@link String}.
     * @return Return a @{@link ResponseEntity}.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/getWorkflowResultsBody/{id}/contents/{contentId}", produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity getWorkflowResultsBody(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String id, @PathVariable String contentId) {
        logger.info(LogHelper.logUser(userDetails) + "Get workflow results body called");

        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<Object> call = service.getWorkflowResultsBodyFromGalaxy(id,contentId,apiKey);

        String jsonString = null;
        try {
            Response<Object> response = call.execute();
            if(response.code() >= 400){
                logger.error(LogHelper.logUser(userDetails) + "Resonse code: " + response.code() + "" + " with body: " + response.errorBody().string());
                return ResponseEntity.badRequest().build();
            }
            jsonString = new Gson().toJson(response.body());
            logger.info(LogHelper.logUser(userDetails) + "----" + response.body() + "----" + response.code());
        } catch (IOException e) {
            logger.error(LogHelper.logUser(userDetails) + "Cannot make the call to Galaxy API", e);
        }
        logger.info(LogHelper.logUser(userDetails) + "Get workflow results body completed");

        return ResponseEntity.ok(jsonString);
    }

    /**
     * Get the result body of a specific workflow with details.
     *
     * @param id : The id as @{@link String} of the workflow.
     * @param contentId : The content id as @{@link String}.
     * @return Return a @{@link ResponseEntity}.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/getWorkflowResultsDetails/{id}/contents/{contentId}", produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity getWorkflowResultsDetails(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String id, @PathVariable String contentId) {
        logger.info(LogHelper.logUser(userDetails) + "Get workflow results details called");

        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<Object> call = service.getWorkflowResultsDetailsFromGalaxy(id,contentId,apiKey);

        String jsonString = null;
        try {
            Response<Object> response = call.execute();
            if(response.code() >= 400){
                logger.error(LogHelper.logUser(userDetails) + "Resonse code: " + response.code() + "" + " with body: " + response.errorBody().string());
                return ResponseEntity.badRequest().build();
            }
            jsonString = new Gson().toJson(response.body());
            System.out.println(jsonString);
            logger.info(LogHelper.logUser(userDetails) + "----" + response.body() + "----" + response.code());
        } catch (IOException e) {
            logger.error(LogHelper.logUser(userDetails) + "Cannot make the call to Galaxy API", e);
        }
        logger.info(LogHelper.logUser(userDetails) + "Get workflow results details completed");

        return ResponseEntity.ok(jsonString);
    }

    @RequestMapping(method = RequestMethod.GET, value = "getErrorMessageOfWorkflow/{id}", produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity getErrorMessageOfWorkflow(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String id) {
        logger.info(LogHelper.logUser(userDetails) + "Get error message of workflow called");

        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<Object> call = service.getErrorMessageOfWorkflowFromGalaxy(id,apiKey);

        String fullError = null;
        String returnError = null;
        try {
            Response<Object> response = call.execute();
            if(response.code() >= 400){
                logger.error(LogHelper.logUser(userDetails) + "Resonse code: " + response.code() + "" + " with body: " + response.errorBody().string());
                return ResponseEntity.badRequest().build();
            }
            JsonParser parser = new JsonParser();
            String jsonString = new Gson().toJson(response.body());
            System.out.println(jsonString);
            JsonElement jsonElement = parser.parse(jsonString);
            JsonObject rootObject = jsonElement.getAsJsonObject();
            fullError = rootObject.get("stderr").getAsString();
            String[] arrOfStr = fullError.split("ValueError", 0);
            String specError = arrOfStr[arrOfStr.length-1];
            returnError = specError.substring(1);

            logger.info(LogHelper.logUser(userDetails) + "----" + response.body() + "----" + response.code());
        } catch (IOException e) {
            logger.error(LogHelper.logUser(userDetails) + "Cannot make the call to Galaxy API", e);
        }

        logger.info(LogHelper.logUser(userDetails) + "Get error message of workflow completed");

        return ResponseEntity.ok(returnError);
    }
}