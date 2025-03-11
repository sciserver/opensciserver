package org.sciserver.springapp.racm.login;

import com.fasterxml.jackson.databind.JsonNode;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface LoginPortalService {
	@GET("api/tokens/{token}")
	Call<JsonNode> getTokenInfo(@Path("token") String token);
}
