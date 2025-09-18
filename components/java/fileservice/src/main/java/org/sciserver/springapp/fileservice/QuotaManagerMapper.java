package org.sciserver.springapp.fileservice;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.springapp.fileservice.dao.QuotaFromManager;
import org.sciserver.springapp.fileservice.dao.QuotaManagerService;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;


/**
 * Maps root volumes to their respective QuotaManagerService instances.
 */
public class QuotaManagerMapper {
    private static final Logger LOG = LogManager.getLogger(QuotaManagerMapper.class);
    private Map<String, QuotaManagerService> quotaManagerServices;

    /**
     * Create QuotaManagerMapper from a configuration map.
     * The map is given in the form of
     *   rootVolume:
     *     url:
     *     username:
     *     password:
     *     readTimeout: # optional, in seconds, defaults to 30
     *   ...
     *
     * @param quotaManagerConfigMap the configuration map
     */
    public QuotaManagerMapper(Map<String, Map<String, String>> quotaManagerConfigMap) {
        quotaManagerServices = new HashMap<String, QuotaManagerService>();

        if (quotaManagerConfigMap == null) {
            return;
        }

        quotaManagerConfigMap.forEach((rootVolume, config) -> {
            String url = config.get("url");
            String user = config.get("username");
            String password = config.get("password");
            int readTimeout = Integer.parseInt(config.getOrDefault("readTimeout", "30"));

            OkHttpClient httpClient = new OkHttpClient.Builder()
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .addInterceptor(
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

    /*
     * Get the usage information from all quota manager services
     *
     * @return a collection of QuotaFromManager objects representing the usage information
     */
    public Collection<QuotaFromManager> getAllUsage() {
        Collection<QuotaFromManager> allUsage = new LinkedList<>();
        for (QuotaManagerService service : quotaManagerServices.values()) {
            try {
                Collection<QuotaFromManager> usage = service.getUsage().execute().body();
                allUsage.addAll(usage);
            } catch (IOException e) {
                LOG.error("Error fetching usage from quota manager service", e);
            }
        }
        return allUsage;
    }
}
