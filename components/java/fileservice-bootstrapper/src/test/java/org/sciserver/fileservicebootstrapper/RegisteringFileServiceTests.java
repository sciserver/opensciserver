package org.sciserver.fileservicebootstrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

@RunWith(MockitoJUnitRunner.class)
public class RegisteringFileServiceTests {
	private static final String TOKEN = "some-token";
	@Mock
	private RACMApi racmApi;
	@Mock
	Call<JsonNode> returnedObject;
	@Captor ArgumentCaptor<Map<String, Object>> captor;

	private RACMService racmService;

	@Before
	public void setup() {
		racmService = new RACMService(racmApi);

		when(racmApi.registerFileService(any(), eq(TOKEN)))
			.thenReturn(returnedObject);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void registeringFileServiceMakeCorrectCall() throws IOException {
		when(returnedObject.execute()).thenReturn(Response.success(null));

		boolean success = racmService.registerFileService(
				"1b97b2a9-bf21-41df-a891-cfb8d6992126",
				"http://example.com", "my-serviceToken", TOKEN);

		assertThat(success).isTrue();

		verify(racmApi).registerFileService(captor.capture(), eq(TOKEN));

		assertThat(captor.getValue())
			.isNotNull()
			.containsEntry("name", "FileService")
			.containsEntry("description", "")
			.containsEntry("apiEndpoint", "http://example.com")
			.containsEntry("serviceToken", "my-serviceToken")
			.containsEntry("identifier", "1b97b2a9-bf21-41df-a891-cfb8d6992126");

		List<Map<String, String>> rootVolumes = (List<Map<String, String>>)captor.getValue().get("rootVolumes");
		assertThat(rootVolumes)
			.extracting("name", "description", "pathOnFileSystem", "containsSharedVolumes")
			.containsOnly(
					tuple("Storage", "", "/srv/Storage/", true),
					tuple("Temporary", "", "/srv/Temporary/", true));
	}

	@Test
	public void registeringFileServiceNoExceptionOnConflict() throws IOException {
		when(returnedObject.execute()).thenReturn(
				Response.error(409, ResponseBody.create(null, new byte[] {})));

		boolean success = racmService.registerFileService(
				"605f0759-28a1-4c7d-8d44-cba14efdcee5",
				"http://my.example.com", "my-other-serviceToken", TOKEN);
		assertThat(success).isFalse();
	}

	@Test(expected=IOException.class)
	public void failedRACMCallsResultsInException() throws IOException {
		when(returnedObject.execute()).thenReturn(
				Response.error(500, ResponseBody.create(null, new byte[] {})));

		racmService.registerFileService("b3de2701-d817-46b0-b399-432010df8ef1",
				"http://another.example.com", "another-serviceToken", TOKEN);
	}
}
