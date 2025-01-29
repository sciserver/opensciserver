package org.sciserver.authentication.client;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AuthenticationClientInterface {

    @GET("api/validate/{token}")
    Call<AuthenticatedUser> getAuthenticatedUser(@Path("token") String token);

    @GET("api/users/{userid}")
    Call<User> getUserById(@Path("userid") String userid);

    @GET("api/users")
    Call<List<User>> getUserByName(@Query("name") String username);

}
