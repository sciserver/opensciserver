package org.sciserver.fileservicebootstrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class SharingFileServiceTests {
	private final MockWebServer server = new MockWebServer();
	private RACMService racmService;

	private static final String IDENTIFIER = "fa501e02-50de-45a4-8651-54ea236586c5";
	private static final String ROOT_CONTEXT_UUID = "74b2267a-0885-4f52-888c-eb3e99304a0d";
	private static final String ROOT_VOLUME_1 = "4bffee04-73d7-4bdf-9b45-57e136a8da1a";
	private static final String ROOT_VOLUME_2 = "8c3c1da2-c331-49a1-b679-604408385ab4";
	private static final String ADMIN_TOKEN = "admin-token";

	@Before
	public void setup() throws IOException {
		server.start();
		RACMApi racmApi = new Retrofit.Builder()
				.addConverterFactory(JacksonConverterFactory.create())
				.baseUrl(server.url("/"))
				.build()
				.create(RACMApi.class);
		racmService = new RACMService(racmApi);
	}

	@After
	public void tearDown() throws IOException {
		server.shutdown();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPermissionsSet() throws IOException, InterruptedException {
		server.enqueue(new MockResponse()
				.setBody(getFile("get-resources.json"))); // Get All resources
		server.enqueue(new MockResponse()
				.setBody(getFile("get-root-context.json"))); // Get root context
		server.enqueue(new MockResponse().setBody("{}")); // update root context
		server.enqueue(new MockResponse()
				.setBody(getFile("get-root-volume.json"))); // get root volume 1
		server.enqueue(new MockResponse().setBody("{}")); // update root volume 1
		server.enqueue(new MockResponse()
				.setBody(getFile("get-root-volume.json"))); // get root volume 2
		server.enqueue(new MockResponse().setBody("{}")); // update root volume 2
		racmService.setInitialPermissions(IDENTIFIER, ADMIN_TOKEN);

		assertThat(server.takeRequest(100, TimeUnit.MILLISECONDS)) // Get All resources
			.isNotNull();

		RecordedRequest rootContextGet = server.takeRequest(100, TimeUnit.MILLISECONDS);
		assertThat(rootContextGet).isNotNull();
		assertThat(rootContextGet.getRequestUrl().queryParameter("resourceuuid"))
			.isEqualTo(ROOT_CONTEXT_UUID);

		RecordedRequest rootContextUpdate = server.takeRequest(100, TimeUnit.MILLISECONDS);
		assertThat(rootContextUpdate).isNotNull();
		DocumentContext updateToRootContext = JsonPath.parse(rootContextUpdate.getBody().readUtf8());
		assertThat(updateToRootContext.read("$.roles[1]", Map.class))
			.containsEntry("roleName", "fs_admin")
			.containsEntry("scisName", "admin")
			.containsEntry("scisType", "G");

		RecordedRequest rootVolume1Get = server.takeRequest(100, TimeUnit.MILLISECONDS);
		assertThat(rootVolume1Get.getRequestUrl().queryParameter("resourceuuid"))
			.isIn(ROOT_VOLUME_1, ROOT_VOLUME_2);

		RecordedRequest rootVolume1Update = server.takeRequest(100, TimeUnit.MILLISECONDS);
		assertThat(rootVolume1Update).isNotNull();
		DocumentContext updateToRootVolume1 = JsonPath.parse(rootVolume1Update.getBody().readUtf8());
		assertThat((List<Map<String, String>>)updateToRootVolume1.read("$.privileges", List.class))
			.extracting("actionName", "scisName", "scisType")
			.contains(
					tuple("create", "public", "G"),
					tuple("grant", "admin", "G")
					);

		RecordedRequest rootVolume2Get = server.takeRequest(100, TimeUnit.MILLISECONDS);
		assertThat(rootVolume2Get.getRequestUrl().queryParameter("resourceuuid"))
			.isIn(ROOT_VOLUME_1, ROOT_VOLUME_2);

		RecordedRequest rootVolume2Update = server.takeRequest(100, TimeUnit.MILLISECONDS);
		assertThat(rootVolume2Update).isNotNull();
		DocumentContext updateToRootVolume2 = JsonPath.parse(rootVolume2Update.getBody().readUtf8());
		assertThat((List<Map<String, String>>)updateToRootVolume2.read("$.privileges", List.class))
			.extracting("actionName", "scisName", "scisType")
			.contains(
					tuple("create", "public", "G"),
					tuple("grant", "admin", "G")
					);
	}

	private String getFile(String filename) throws IOException {
		return StreamUtils.copyToString(
				new ClassPathResource(filename).getInputStream(),
				Charset.defaultCharset());
	}
}
