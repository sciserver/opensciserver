package org.sciserver.springapp.fileservice;

import java.util.Arrays;
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
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Configuration
@EnableScheduling
class SpringBeansConfiguration {
    private @Value("${RACM.endpoint}") String racmEndpoint;
    private @Value("${File-service.serviceId}") String fileServiceToken;
    private @Value("${quota-manager.url:#{null}}") Optional<String> managerUrl;
    private @Value("${quota-manager.username:user}") String quotaManagerUsername;
    private @Value("${quota-manager.password:}") String quotaManagerPassword;
    private @Value("${quota-manager.request.readTimeout:30}") int quotaManagerReadTimeout;

    @Bean
    Config config() throws Exception{
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

    @Qualifier("okHttpClientQuotaManagerAuthentication")
    @Bean("okHttpClientQuotaManagerAuthentication")
    OkHttpClient okHttpClientQuotaManagerAuthentication() {
        if (!managerUrl.isPresent()) {
            return null;
        }
        return new OkHttpClient().newBuilder()
                .readTimeout(quotaManagerReadTimeout, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    final Request request = chain.request()
                            .newBuilder()
                            .addHeader("Authorization",
                                    Credentials.basic(quotaManagerUsername, quotaManagerPassword))
                            .build();

                    return chain.proceed(request);
                })
                .build();
    }

    @Bean
    QuotaManagerService quotaManagerService(
            @Qualifier("okHttpClientQuotaManagerAuthentication") Optional<OkHttpClient> okHttpClient) {
        QuotaManagerService qms = managerUrl.map(s -> new Retrofit.Builder()
                .baseUrl(s)
                .addConverterFactory(JacksonConverterFactory.create())
                .client(okHttpClient.get())
                .build()
                .create(QuotaManagerService.class)).orElse((QuotaManagerService)null);
        return qms;
    }

    @Bean
    CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setAllowNullValues(false);
        cacheManager.setCacheNames(Arrays.asList(UsageInfoProvider.ID_CACHE_NAME));
        return cacheManager;
    }
}
