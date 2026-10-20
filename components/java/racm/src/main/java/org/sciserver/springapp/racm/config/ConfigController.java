package org.sciserver.springapp.racm.config;

import org.sciserver.springapp.racm.utils.controller.RACMController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@CrossOrigin
@RequestMapping("config")
@Tag(name = "Configuration",
     description = "Public configuration values needed by SciServer front ends.")
public class ConfigController extends RACMController {
	@Autowired
	private ConfigURLs configUrls;

	@Operation(
	    summary = "Get the public configuration of this SciServer deployment.",
	    description = "Returns the URLs of the SciServer components this RACM is configured "
	                  + "against, so front ends can discover them rather than hard-coding them. "
	                  + "Requires no authentication.")
	@ApiResponses({
	    @ApiResponse(responseCode = "200",
	                 description = "The URLs of the SciServer components this deployment is configured against."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping
	public ResponseEntity<JsonNode> config() {
		return jsonAPIHelper.success(configUrls.getUrls());
	}
}
