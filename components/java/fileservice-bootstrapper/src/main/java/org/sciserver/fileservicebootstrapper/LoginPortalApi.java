package org.sciserver.fileservicebootstrapper;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface LoginPortalApi {
	@Headers("Accept: text/plain")
	@POST("api/auth")
	Call<String> getToken(@Body Map<String, String> body);
}
