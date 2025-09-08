package org.sciserver.springapp.fileservice;

import java.util.HashMap;
import java.util.Map;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.sciserver.springapp.fileservice.dao.QuotaManagerService;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;


/**
 * Maps root volumes to their respective QuotaManagerService instances.
 */
public class QuotaManagerMapper {
    private Map<String, QuotaManagerService> quotaManagerServices;

    /**
     * Create QuotaManagerMapper from a configuration map.
     * The map is given in the form of
     *   rootVolume:
     *     url:
     *     username:
     *     password:
     *   ...
     *
     * @param quotaManagerConfigMap the configuration map
     */
    public QuotaManagerMapper(Map<String, Map<String, String>> quotaManagerConfigMap) {
        quotaManagerServices = new HashMap<String, QuotaManagerService>();

        quotaManagerConfigMap.forEach((rootVolume, config) -> {
            String url = config.get("url");
            String user = config.get("username");
            String password = config.get("password");

            OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(
                chain -> {
                    return chain.proceed(chain.request().newBuilder()
                        .header("Authorization", Credentials.basic(user, password))
                        .build());
                }).build();

            QuotaManagerService quotaManagerService = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(JacksonConverterFactory.create())
                .client(httpClient)
                .build()
                .create(QuotaManagerService.class);

            quotaManagerServices.put(rootVolume, quotaManagerService);
        });
    }

    /*
     * Get the QuotaManagerService for a specific root volume
     *
     * @param rootVolume the root volume
     * @return the QuotaManagerService for the specified root volume, or null if not found
     */
    public QuotaManagerService getQuotaManagerService(String rootVolume) {
        return quotaManagerServices.get(rootVolume);
    }
}
