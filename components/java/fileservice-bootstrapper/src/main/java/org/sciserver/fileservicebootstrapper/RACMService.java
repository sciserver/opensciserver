package org.sciserver.fileservicebootstrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import retrofit2.Response;

@Service
public class RACMService {
	private final RACMApi racmApi;

	public RACMService(RACMApi racmApi) {
		this.racmApi = racmApi;
	}

	public boolean registerFileService(String identifier, String apiEndpoint,
			String serviceToken, String token) throws IOException {
		Map<String, Object> fileServiceObject = Map.of(
				"name", "FileService",
				"description", "",
				"apiEndpoint", apiEndpoint,
				"serviceToken", serviceToken,
				"identifier", identifier,
				"rootVolumes", List.of(
						Map.of(
								"name", "Storage",
								"description", "",
								"pathOnFileSystem", "/srv/Storage/",
								"containsSharedVolumes", Boolean.TRUE),
						Map.of(
								"name", "Temporary",
								"description", "",
								"pathOnFileSystem", "/srv/Temporary/",
								"containsSharedVolumes", Boolean.TRUE))
				);
		Response<JsonNode> response = racmApi.registerFileService(fileServiceObject, token).execute();
		if (!response.isSuccessful() && response.code() != 409) {
			throw new IOException("RACM returned: " + response.toString());
		}
		return response.code() != 409;
	}

	@SuppressWarnings("unchecked")
	public void setInitialPermissions(String identifier, String adminToken) throws IOException {
		Response<JsonNode> userResourcesResponse = racmApi.getResources(adminToken).execute();
		String rootContextUUID = extractResourceUUIDs(
				identifier, userResourcesResponse.body(), "__rootcontext__").get(0);
		List<String> rootVolumeUUIDs = extractResourceUUIDs(
				identifier, userResourcesResponse.body(), "FileService.RootVolume");

		Map<String, Object> rootContextObj = racmApi.getResource(adminToken, rootContextUUID).execute().body();
		((List<Map<String, String>>) rootContextObj.get("roles")).add(Map.of(
				"roleName", "fs_admin",
				"scisName", "admin",
				"scisType", "G"
				));
		racmApi.updateResource(adminToken, rootContextObj).execute();

		for (String rootVolumeUUID : rootVolumeUUIDs) {
			Map<String, Object> rootVolume = racmApi.getResource(adminToken, rootVolumeUUID).execute().body();
			((List<Map<String, String>>) rootVolume.get("privileges")).add(Map.of(
					"actionName", "create",
					"scisName", "public",
					"scisType", "G"
					));
			((List<Map<String, String>>) rootVolume.get("privileges")).add(Map.of(
					"actionName", "grant",
					"scisName", "admin",
					"scisType", "G"
					));
			racmApi.updateResource(adminToken, rootVolume).execute();
		}
	}

	private List<String> extractResourceUUIDs(String resourceContextUUID, JsonNode resourcesContent, String resourceType) {
		var columns = resourcesContent.get("columns");
		int columnForResourceContextUUID = getColumnIndex("resourceContextUUID", columns);
		int columnForResourceUUID = getColumnIndex("resourceuuid", columns);
		int columnForResourceType = getColumnIndex("resourceType", columns);

		Set<String> output = new HashSet<>();
		for (JsonNode row : resourcesContent.get("rows")) {
			if (resourceContextUUID.equals(row.get(columnForResourceContextUUID).asText()) &&
					resourceType.equals(row.get(columnForResourceType).asText())) {
				output.add(row.get(columnForResourceUUID).asText());
			}
		}
		return new ArrayList<>(output);
	}

	private int getColumnIndex(String columnNameToFind, JsonNode columns) {
		for (int i = 0; i < columns.size(); i++) {
			if (columnNameToFind.equals(columns.get(i).textValue()))
				return i;
		}
		throw new IllegalStateException("Could not find column " + columnNameToFind);
	}
}
