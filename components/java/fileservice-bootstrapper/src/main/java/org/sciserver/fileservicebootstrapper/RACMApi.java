package org.sciserver.fileservicebootstrapper;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface RACMApi {
	@POST("storem/fileservices")
	Call<JsonNode> registerFileService(
			@Body Map<String, Object> requestBody,
			@Header("X-Auth-Token") String token);

	@GET("rest/resources")
	Call<JsonNode> getResources(@Header("X-Auth-Token") String token);

	@GET("rest/privileges/")
	Call<Map<String, Object>> getResource(@Header("X-Auth-Token") String token,
			@Query("resourceuuid") String uuid);

	@POST("rest/resources")
	Call<JsonNode> updateResource(@Header("X-Auth-Token") String token,
			@Body Map<String, Object> requestBody);
}
