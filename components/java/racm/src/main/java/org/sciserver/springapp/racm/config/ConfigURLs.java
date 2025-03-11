package org.sciserver.springapp.racm.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "org.sciserver.racm.config")
public class ConfigURLs {
	private Map<String, String> urls = new HashMap<>();

	public Map<String, String> getUrls() {
		return urls;
	}

	public void setUrls(Map<String, String> urls) {
		this.urls = urls;
	}

	@PostConstruct
	public void fillFromBuiltIn() {
		if (urls.isEmpty()) {
			Properties props = new Properties();
			try {
				props.load(getClass().getResourceAsStream("/config.properties"));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			props.forEach((key, value) ->
				urls.put(key.toString(), value.toString())
			);
		}
	}
}
