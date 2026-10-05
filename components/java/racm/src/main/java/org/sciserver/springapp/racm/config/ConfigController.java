package org.sciserver.springapp.racm.config;

import org.sciserver.springapp.racm.utils.controller.RACMController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@CrossOrigin
@RequestMapping("config")
@Tag(name = "Configuration", description = "Public configuration values needed by SciServer front ends.")
public class ConfigController extends RACMController {
	@Autowired
	private ConfigURLs configUrls;

	@GetMapping
	public ResponseEntity<JsonNode> config() {
		return jsonAPIHelper.success(configUrls.getUrls());
	}
}
