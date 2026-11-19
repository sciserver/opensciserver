package compm.application.rdb.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;


/**
 * Has methods for sending a stream of output content through an HTTP request.
 */
public class StreamChannelForHTTPRequest extends StreamChannel {

    private String url;
    private String requestMethod;
    private HttpURLConnection httpCon;
    private Integer responseCode;
    private String responseMessage;
    private int httpConnectionTimeout;
    private HttpHeaders httpRequestHeaders;

    /**
     * Constructor of `StreamChannelForHTTPRequest`, which represents a channel for sending 
     * an output as streamed content on the body of an HTTP request to an external url.
     *
     * @param url external url where the HTTP request is sent.
     * @param requestMethod HTTP request method.
     * @param httpRequestHeaders HTTP request headers.
     * @param httpConnectionTimeout connection timeout of the HTTP request.
     */
    public StreamChannelForHTTPRequest(String url, String requestMethod, HttpHeaders httpRequestHeaders,
                                       int httpConnectionTimeout) {
        this.url = url;
        this.requestMethod = requestMethod;
        this.httpConnectionTimeout = httpConnectionTimeout;
        this.httpRequestHeaders = httpRequestHeaders;
    }

    /**
     * Opens/establishes an HTTP connection, sets request headers, and prepares the
     * stream for sending data.
     */
    public void open() throws MalformedURLException, IOException {
        URL connUrl = new URL(this.url);
        httpCon = (HttpURLConnection) connUrl.openConnection();
        httpCon.setRequestMethod(requestMethod);
        for (String headerKey : httpRequestHeaders.keySet()) {
            httpCon.setRequestProperty(headerKey, httpRequestHeaders.getFirst(headerKey));
        }
        httpCon.setDoOutput(true);
        httpCon.setDoInput(true);
        httpCon.setConnectTimeout(httpConnectionTimeout);
        this.outputStream = httpCon.getOutputStream();
    }

    /**
     * Closes the output stream, gets the HTTP response, and disconnects the HTTP
     * connection.
     */
    public void close() throws IOException {
        outputStream.close();
        getHttpResponse();
        httpCon.disconnect();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("Error when closing connection to " + this.url + " with HTTP response code "
                    + responseCode + " and content: " + responseMessage + " .");
        }
    }

    /**
     * Gets the HTTP response code and message from the HTTP connection.
     */
    private void getHttpResponse() throws IOException {
        responseCode = httpCon.getResponseCode();
        InputStream inputStream = httpCon.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        responseMessage = reader.lines().collect(Collectors.joining("\n"));
    }

}
