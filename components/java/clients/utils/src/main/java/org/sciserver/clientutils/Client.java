package org.sciserver.clientutils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public abstract class Client<T> {

    public static final String AUTH_TOKEN_HEADER = "X-Auth-Token";
    public static final String SERVICE_TOKEN_HEADER = "X-Service-Auth-ID";
    private static final int SOCKET_TIMEOUT_SECS = 60;

    private Retrofit retrofit;
    protected T retrofitAdapter; // TODO protected so we don't need a getter

    public Client(String endpoint, String serviceToken, Class<T> t) {
        retrofitSetup(endpoint, serviceToken, t);
    }

    public Client(String endpoint, Class<T> t) {
        retrofitSetup(endpoint, null, t);
    }

    protected <V> V getSyncResponse(Call<V> call) throws SciServerClientException {
        Response<V> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            throw new SciServerClientException("IO error in call to endpoint:" + e.toString(), 502);
        }
        if (!response.isSuccessful()) {
            // in case of error, either store the http response message or the error field (if present) of body assuming
            // a JSON response of form {"status": "error", "error": "error message"}
            String message = response.message() == null ? "unknown" : response.message();
            try {
                Converter<ResponseBody, JsonNode> converter = retrofit.responseBodyConverter(
                    JsonNode.class, new Annotation[0]);
                message = converter.convert(response.errorBody()).get("error").asText();
            } catch (Exception e) {
                // Fallback to either unkown or the http response message, then rolled into client exception
            }
            throw new SciServerClientException(message, response.code());
        }
        return response.body();
    }

    private void retrofitSetup(String racmEndpoint, String racmServiceToken, Class<T> t) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient().newBuilder();
        if (racmServiceToken != null) {
            clientBuilder.addInterceptor(chain -> {
                    Request request = chain.request().newBuilder()
                        .addHeader(SERVICE_TOKEN_HEADER, racmServiceToken)
                        .build();
                    return chain.proceed(request);
                }
            );
        }
        OkHttpClient client = clientBuilder
            .connectTimeout(SOCKET_TIMEOUT_SECS, TimeUnit.SECONDS)
            .readTimeout(SOCKET_TIMEOUT_SECS, TimeUnit.SECONDS)
            .writeTimeout(SOCKET_TIMEOUT_SECS, TimeUnit.SECONDS)
            .build();
        this.retrofit = new Retrofit.Builder()
            .baseUrl(racmEndpoint)
            .client(client)
            .addConverterFactory(JacksonConverterFactory.create(new ObjectMapper().registerModule(new Jdk8Module())))
            .build();
        this.retrofitAdapter = retrofit.create(t);
    }

}
