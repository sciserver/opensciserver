package org.sciserver.springapp.fileservice;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.sciserver.racm.client.RACMClient;
import org.sciserver.springapp.fileservice.dao.QuotaManagerService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
class SpringBeansConfiguration {
    private @Value("${RACM.endpoint}") String racmEndpoint;
    private @Value("${File-service.serviceId}") String fileServiceToken;
    private @Value("${quota-manager.url:#{null}}") Optional<String> managerUrl;
    private @Value("${quota-manager.username:user}") String quotaManagerUsername;
    private @Value("${quota-manager.password:}") String quotaManagerPassword;
    private @Value("${quota-manager.request.readTimeout:30}") int quotaManagerReadTimeout;
    private @Value("${quota-manager-map:#{null}}") Optional<Map<String, Map<String, String>>> quotaManagerMap;

    @Bean
    Config config() throws Exception {
        Config config = new Config();
        config.load();
        return config;
    }

    @Qualifier("okHttpClientWithFileServiceId")
    @Bean
    OkHttpClient okHttpClientWithFileServiceId() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

        httpClient.addInterceptor(chain -> {
            Request original = chain.request();
            return chain.proceed(original.newBuilder()
                    .header("X-Service-Auth-ID", fileServiceToken)
                    .method(original.method(), original.body())
                    .build()
            );
        });

        httpClient.addInterceptor(logging);
        return httpClient.build();
    }

    @Bean
    RACMClient racmClient() {
        return new RACMClient(racmEndpoint, fileServiceToken);
    }

    @Bean
    QuotaManagerMapper quotaManagerMapper() {
        return new QuotaManagerMapper(quotaManagerMap.orElse(Collections.emptyMap()));
    }

    @Bean
    CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setAllowNullValues(false);
        cacheManager.setCacheNames(Arrays.asList(UsageInfoProvider.ID_CACHE_NAME));
        return cacheManager;
    }
}
