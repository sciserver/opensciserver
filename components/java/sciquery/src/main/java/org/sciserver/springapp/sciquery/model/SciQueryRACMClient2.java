package org.sciserver.springapp.sciquery.model;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.clientutils.Client;
import org.sciserver.clientutils.SciServerClientException;
import org.sciserver.racm.jobm.model.RDBJobModel;
import org.springframework.http.HttpStatus;

/**
 * Client for interacting with the RACM service.
 */
public class SciQueryRACMClient2 {
    public static final MediaType JSON_UTF8 = MediaType.parse("application/json; charset=utf-8");
    private static final Logger LOG = LogManager.getLogger();
    private ObjectMapper objectMapper;
    private OkHttpClient client;
    private String racmEndpoint;

    public SciQueryRACMClient2(String racmEndpoint, String racmServiceToken) {
        clientSetup(racmEndpoint, racmServiceToken);
    }
    
    public SciQueryRACMClient2(String racmEndpoint) {
        clientSetup(racmEndpoint, null);
    }

    private void clientSetup(String racmEndpoint, String racmServiceToken) {
        objectMapper = JsonMapper.builder().enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                    .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();
        objectMapper.setSerializationInclusion(Include.NON_NULL);

        this.racmEndpoint = racmEndpoint;

        OkHttpClient.Builder clientBuilder = new OkHttpClient().newBuilder();
        if (racmServiceToken != null) {
            clientBuilder.addInterceptor(chain -> {
                    Request request = chain.request().newBuilder()
                        .addHeader(Client.SERVICE_TOKEN_HEADER, racmServiceToken)
                        .build();
                    return chain.proceed(request);
                }
            );
        }
        client = clientBuilder.build();
    }

    /**
     * Converts an object to its JSON string representation.
     *
     * @param objectToMap the object to convert to JSON.
     * @return the JSON representation of the object, or null if conversion fails.
     */
    public <T> String objectToJson(T objectToMap) {
        String objectToMapAsStr = null;
        try {
            objectToMapAsStr = objectMapper.writeValueAsString(objectToMap);
        } catch (JsonProcessingException jpe) {
            jpe.printStackTrace();
        }
        
        return objectToMapAsStr;
    }

    /**
     * Submits an RDB job to RACM.
     *
     * @param userToken the user token for authentication.
     * @param serviceToken the SciQuery service token for authentication.
     * @param rdbJobModel the RDB job model to submit.
     * @return the submitted RDB job model response.
     * @throws SciServerClientException if an error occurs during submission to RACM.
     */
    public RDBJobModel submitRdbJob(String userToken, String serviceToken, 
                                    RDBJobModel rdbJobModel) throws SciServerClientException {
        LOG.debug("submitRdbJob:  user=" + userToken + ", service=" + serviceToken);

        String rdbJobModelAsStr = objectToJson(rdbJobModel);
        if (rdbJobModelAsStr == null) {
            throw new SciServerClientException("Unable to convert RDBJobModel to JSON!", 
                                               HttpStatus.BAD_REQUEST.value());
        }
        LOG.debug("submitRdbJob:  racm=" + racmEndpoint + "\nbody=\n" + rdbJobModelAsStr);

        RequestBody body = RequestBody.create(JSON_UTF8, rdbJobModelAsStr);
        Request request = new Request.Builder()
                                     .url(racmEndpoint + "jobm/rest/jobs/rdb")
                                     .header(Client.AUTH_TOKEN_HEADER, userToken)
                                     .header(Client.SERVICE_TOKEN_HEADER, serviceToken)
                                     .post(body)
                                     .build();

        RDBJobModel rdbJobModelResponse = null;
        try {
            okhttp3.Response response = client.newCall(request).execute();
            rdbJobModelResponse = objectMapper.readValue(response.body().string(), RDBJobModel.class);
        } catch (IOException ioe) {
            throw new SciServerClientException(ioe.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return rdbJobModelResponse;
    }
}
