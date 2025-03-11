package org.sciserver.fileservicebootstrapper;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

@SpringBootApplication
@EnableConfigurationProperties(Config.class)
public class FileserviceBootstrapperApplication implements ApplicationRunner {
	private static final Logger LOGGER = LogManager.getLogger();
	@Autowired
	private Config config;

	@Autowired
	RACMService racmService;

	@Autowired
	LoginPortalApi loginPortalApi;

	public static void main(String[] args) {
		SpringApplication.run(FileserviceBootstrapperApplication.class, args);
	}

	@Bean
	public RACMApi racmApi() {
		return new Retrofit.Builder()
				.addConverterFactory(JacksonConverterFactory.create())
				.baseUrl(config.getRacmUrl())
				.build()
				.create(RACMApi.class);
	}

	@Bean
	public LoginPortalApi loginPortalApi() {
		return new Retrofit.Builder()
				.addConverterFactory(ScalarsConverterFactory.create())
				.addConverterFactory(JacksonConverterFactory.create())
				.baseUrl(config.getLoginPortalUrl())
				.build()
				.create(LoginPortalApi.class);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		String adminToken = loginPortalApi.getToken(Map.of(
				"username", config.getAdmin().getName(),
				"password", config.getAdmin().getPassword()
				)).execute().body();

		LOGGER.info("Attempting to (re-)register file service");
		boolean created = racmService.registerFileService(
				config.getFileServiceIdentifier(), config.getFileServiceURL(),
				config.getFileServiceServiceToken(), adminToken);
		if (created) {
			LOGGER.info("New file service created, setting initial permissions");
			racmService.setInitialPermissions(config.getFileServiceIdentifier(), adminToken);
			LOGGER.info("Initial permissions set");
		} else {
			LOGGER.info("File service already registered");
		}
	}
}
