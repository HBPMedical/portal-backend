package eu.hbp.mip.controllers.retrofit;

import com.google.gson.JsonObject;
import eu.hbp.mip.dto.GalaxyWorkflowResult;
import eu.hbp.mip.dto.PostWorkflowToGalaxyDtoResponse;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface RetroFitGalaxyClients {

    @POST("workflows/{workflowId}/invocations")
    Call<PostWorkflowToGalaxyDtoResponse> postWorkflowToGalaxy(@Path("workflowId") String workflowId, @Query("key") String key, @Body JsonObject body);

    @GET("histories/{historyId}")
    Call<Object> getWorkflowStatusFromGalaxy(@Path("historyId") String historyId, @Query("key") String key);

    @GET("histories/{historyId}/contents")
    Call<List<GalaxyWorkflowResult>> getWorkflowResultsFromGalaxy(@Path("historyId") String historyId, @Query("key") String key);

    @GET("histories/{historyId}/contents/{contentId}/display")
    Call<Object> getWorkflowResultsBodyFromGalaxy(@Path("historyId") String historyId, @Path("contentId") String contentId, @Query("key") String key);

    @GET("jobs/{jobId}?full=true")
    Call<Object> getErrorMessageOfWorkflowFromGalaxy(@Path("jobId") String jobId, @Query("key") String key);
}