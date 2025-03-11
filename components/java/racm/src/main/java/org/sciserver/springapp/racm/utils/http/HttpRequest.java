package org.sciserver.springapp.racm.utils.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

public class HttpRequest {
	private static final String GET = "GET";

	public HttpResponseResult executeGet(String requestUrl, Map<String, String> httpHeaderFieldsttpHeaderFields)
			throws IOException {
		Objects.requireNonNull(httpHeaderFieldsttpHeaderFields);
		HttpURLConnection connection = null;
		try {
			URL url = new URL(requestUrl);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(false);
			connection.setRequestMethod(GET);

			connection.setUseCaches(false);
			connection.setDoOutput(true);

			connection.setRequestProperty("Content-Language", "en-US");
			connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

			for (Map.Entry<String, String> entry : httpHeaderFieldsttpHeaderFields.entrySet()) {
				connection.setRequestProperty(entry.getKey(), entry.getValue());
			}

			// Get Response
			java.io.InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
			String line = rd.readLine();
			while (line != null) {
				response.append(line);
				response.append('\r');
				line = rd.readLine();
			}
			rd.close();
			is.close();
			connection.disconnect();
			return new HttpResponseResult(connection.getResponseCode(), response.toString());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

}
