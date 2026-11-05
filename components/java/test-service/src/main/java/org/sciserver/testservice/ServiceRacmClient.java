package org.sciserver.testservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.sciserver.racm.client.RACMClient;
import org.sciserver.racm.client.RACMClientInterface;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/** Service-local transport configuration for the existing RACM client. */
class ServiceRacmClient extends RACMClient {
    ServiceRacmClient(String endpoint) {
        super(endpoint);
        OkHttpClient transport = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
        // Client exposes this protected hook. Inherited RACM operations and response
        // handling stay intact; only this service's request transport is replaced.
        // Match the parent client's JSON configuration, including Optional support.
        ObjectMapper mapper = new ObjectMapper().registerModule(new Jdk8Module());
        retrofitAdapter = new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(transport)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .build()
                .create(RACMClientInterface.class);
    }
}
