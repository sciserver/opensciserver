package org.sciserver.casjobs.client;

import com.fasterxml.jackson.databind.JsonNode;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

import org.sciserver.clientutils.Client;

public interface CasJobsClientInterface {

    @GET("users/{userid}")
    Call<JsonNode> getCasJobsUserInfo(@Path("userid") String userid, @Header(Client.AUTH_TOKEN_HEADER) String userToken);

}
