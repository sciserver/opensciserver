package org.sciserver.springapp.sciquery;

import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.authentication.client.AuthenticationClient;
import org.sciserver.casjobs.client.CasJobsClient;
import org.sciserver.clientutils.Client;
import org.sciserver.racm.client.RACMClient;
import org.sciserver.sciquery.metadata.MetadataManager;
import org.sciserver.springapp.sciquery.model.SciQueryRACMClient2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableCaching
@EnableScheduling
@EnableJpaRepositories("org.sciserver.springapp.sciquery.model")
class SpringBeansConfiguration {
    private static final Logger LOG = LogManager.getLogger();

    private @Value("${auth.portal.url}") String authPortalUrl;
    private @Value("${RACM.endpoint}") String racmEndpoint;
    private @Value("${RACM.resourceContext.serviceToken}") String racmServiceToken;
    private @Value("${CasJobs.endpoint}") String casJobsEndpoint;
    private @Value("${CasJobs.UUID}") String casJobsUUID;
    private @Value("${File-service.serviceId}") String fileServiceToken;

    @Autowired
    private DataSource dataSource;

    @Bean
    Config config() throws Exception {
        LOG.info("endpoint=" + racmEndpoint);
        Config config = new Config();
        config.load();
        return config;
    }

    @Bean
    AuthenticationClient authenticationClient() {
        return new AuthenticationClient(authPortalUrl);
    }    

    @Bean
    RACMClient racmClient() {
        return new RACMClient(racmEndpoint);
    }

    @Bean
    CasJobsClient casJobsClient() {
        return new CasJobsClient(casJobsEndpoint);
    }

    @Bean
    SciQueryRACMClient2 sciQueryRACMClient2() {
        return new SciQueryRACMClient2(racmEndpoint);
    }
    /*
    @Bean
    SciQueryRACMClient sciQueryRACMClient() {
        return new SciQueryRACMClient(racmEndpoint);
    }
    */
    ////////////////////////////////////////////////////////////////////////////////

    @Qualifier("metadataManager")
    @Bean
    MetadataManager metadataManager() {
        try {
            return new MetadataManager(racmEndpoint, racmServiceToken, casJobsUUID, dataSource);
        } catch (Exception e) {
            // log exception
            // kill the construction of the webapp?
            return null;
        }
    }

    @Qualifier("okHttpClientWithFileServiceId")
    @Bean
    OkHttpClient okHttpClientWithFileServiceId() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.connectTimeout(Config.CONNECTION_TIMEOUT, TimeUnit.SECONDS);
        httpClient.readTimeout(Config.READ_TIMEOUT, TimeUnit.SECONDS);
        httpClient.writeTimeout(Config.WRITE_TIMEOUT, TimeUnit.SECONDS);

        httpClient.addInterceptor(chain -> {
            Request original = chain.request();
            return chain.proceed(original.newBuilder()
                    .header(Client.SERVICE_TOKEN_HEADER, fileServiceToken)
                    .method(original.method(), original.body())
                    .build()
            );
        });

        httpClient.addInterceptor(logging);
        return httpClient.build();
    }
}
