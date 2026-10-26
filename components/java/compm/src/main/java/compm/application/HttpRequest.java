package compm.application;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;


/**
 * The `HttpRequest` class in Java provides methods for executing HTTP requests with various request
 * methods.
 */
public class HttpRequest {


    private int responseCode;
    private String message;
    private Map<String, List<String>> responseHeader;
    private String compmId; 

    public static String POST = "POST";
    public static String GET = "GET";
    public static String PUT = "PUT";
    public static String DELETE = "DELETE";

    /**
     * Constructor of the HttpRequest class.
     */
    public HttpRequest() {
        this.responseCode  = 0;
        this.message = "";
        this.compmId = "";
        this.responseHeader = null;
    }

    /**
     * Constructor of the HttpRequest class.
     *
     * @param compmId Id fo the COMPM
     */
    public HttpRequest(String compmId) {
        this.responseCode  = 0;
        this.message = "";
        this.compmId = compmId;
        this.responseHeader = null;
    }

    public int getResponseCode() {
        return this.responseCode;
    }

    public String getMessage() {
        return this.message;
    }

    public String getCompmId() {
        return this.compmId;
    }

    public Map<String, List<String>> getResponseHeader() {
        return this.responseHeader;
    }

    /**
     * Returns true if the WebService is alive, and false if the WebService is down. 
     *
     * @param webserviceUrl url of WebService.
     * @return Boolean denoting if webservice is alive or not.
     */
    public boolean getWebServiceAliveStatus(String webserviceUrl, String requestMethod) {
        //TODO
        try {
            HttpResponseResult result = executeRequest(webserviceUrl, requestMethod, null, null);
            if (result.responseCode >= 200 & result.responseCode <= 299) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            //log that service was unreachable
            return false;
        }
    }

    /**
     * Sends a POST request to a specified URL.
     *
     * @param url URL to which the HTTP request will be sent to.
     * @return `HttpResponseResult` object containing the response code, message, response header, and the URL.
     */    
    public HttpResponseResult executePost(String url) throws Exception {
        return executeRequest(url, this.POST, null, null);
    }

    /**
     * Sends a POST request to a specified URL with custom HTTP header fields.
     *
     * @param url URL to which the HTTP request will be sent to.
     * @param httpHeaderFields Hashtable that contains key-value pairs of HTTP header fields to be included 
     *      in the request.
     * @return `HttpResponseResult` object containing the response code, message, response header, and the URL.
     */    
    public HttpResponseResult executePost(String url, Hashtable<String, String> httpHeaderFields) throws Exception {
        return executeRequest(url, this.POST, httpHeaderFields, null);
    }

    /**
     * Sends a POST request to a specified URL with custom HTTP header fields and content.
     *
     * @param url URL to which the HTTP request will be sent to.
     * @param httpHeaderFields Hashtable that contains key-value pairs of HTTP header fields to be included 
     *      in the request.
     * @param content The data that to be sent in the HTTP request body.
     * @return `HttpResponseResult` object containing the response code, message, response header, and the URL.
     */    
    public HttpResponseResult executePost(
        String url, Hashtable<String, String> httpHeaderFields, String content
    ) throws Exception {
        return executeRequest(url, this.POST, httpHeaderFields, content);
    }

    /**
     * Sends a GET request to a specified URL.
     *
     * @param url URL to which the HTTP request will be sent to.
     * @return `HttpResponseResult` object containing the response code, message, response header, and the URL.
     */    
    public HttpResponseResult executeGet(String url) throws Exception {
        return executeRequest(url, HttpRequest.GET, null, null);
    }

    /**
     * Sends a GET request to a specified URL with custom HTTP header fields.
     *
     * @param url URL to which the HTTP request will be sent to.
     * @param httpHeaderFields Hashtable that contains key-value pairs of HTTP header fields to be included 
     *      in the request.
     * @return `HttpResponseResult` object containing the response code, message, response header, and the URL.
     */    
    public HttpResponseResult executeGet(String url, Hashtable<String, String> httpHeaderFields) throws Exception {
        return executeRequest(url, HttpRequest.GET, httpHeaderFields, null);
    }

    /**
     * Sends a DELETE request to a specified URL..
     *
     * @param url URL to which the HTTP request will be sent to.
     * @return `HttpResponseResult` object containing the response code, message, response header, and the URL.
     */    
    public HttpResponseResult executeDelete(String url) throws Exception {
        return executeRequest(url, HttpRequest.DELETE, null, null);
    }

    /**
     * Sends a DELETE request to a specified URL with custom HTTP header fields.
     *
     * @param url URL to which the HTTP request will be sent to.
     * @param httpHeaderFields Hashtable that contains key-value pairs of HTTP header fields to be included 
     *      in the request.
     * @return `HttpResponseResult` object containing the response code, message, response header, and the URL.
     */    
    public HttpResponseResult executeDelete(String url, Hashtable<String, String> httpHeaderFields) throws Exception {
        return executeRequest(url, HttpRequest.DELETE, httpHeaderFields, null);
    }

    /**
     * Sends a PUT request to a specified URL.
     *
     * @param url URL to which the HTTP request will be sent to.
     * @return `HttpResponseResult` object containing the response code, message, response header, and the URL.
     */
    public HttpResponseResult executePut(String url) throws Exception {
        return executeRequest(url, HttpRequest.PUT, null, null);
    }

    /**
     * Sends a PUT request to a specified URL with custom HTTP header fields.
     *
     * @param url URL to which the HTTP request will be sent to.
     * @param httpHeaderFields Hashtable that contains key-value pairs of HTTP header fields to be included 
     *      in the request.
     * @return `HttpResponseResult` object containing the response code, message, response header, and the URL.
     */    
    public HttpResponseResult executePut(String url, Hashtable<String, String> httpHeaderFields) throws Exception {
        return executeRequest(url, HttpRequest.PUT, httpHeaderFields, null);
    }

    /**
     * Sends a PUT request to a specified URL with custom HTTP header fields and content.
     *
     * @param url URL to which the HTTP request will be sent to.
     * @param httpHeaderFields Hashtable that contains key-value pairs of HTTP header fields to be included 
     *      in the request.
     * @param content The data that to be sent in the HTTP request body.
     * @return `HttpResponseResult` object containing the response code, message, response header, and the URL.
     */
    public HttpResponseResult executePut(String url, Hashtable<String, String> httpHeaderFields, String content
    ) throws Exception {
        return executeRequest(url, HttpRequest.PUT, httpHeaderFields, content);
    }

    /**
     * Sends an HTTP request based on the specified URL, method, headers, and
     * content, and returns the response details.
     *
     * @param requestUrl URL to which the HTTP request will be sent to.
     * @param requestMethod HTTP method to be used for the request.
     * @param httpHeaderFields Hashtable that contains key-value pairs of HTTP header fields to be included 
     *      in the request.
     * @param content The data that to be sent in the HTTP request body.
     * @return `HttpResponseResult` object containing the response code, message, response header, and the URL.
     */
    private HttpResponseResult executeRequest(String requestUrl, String requestMethod, 
                                              Hashtable<String, String> httpHeaderFields, String content
    
    ) throws Exception {
        
        HttpURLConnection connection = null;

        try {

            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();
            if (requestMethod == HttpRequest.POST) {
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
            } else if (requestMethod == HttpRequest.PUT) {
                connection.setDoOutput(true);
                connection.setRequestMethod("PUT");
            } else if (requestMethod == HttpRequest.DELETE) {
                connection.setRequestMethod("DELETE");
            } else {
                connection.setDoOutput(false);
                connection.setRequestMethod("GET");
            }
            
            connection.setUseCaches(false);
            connection.setDoOutput(true);

            connection.setRequestProperty("Content-Language", "en-US");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            connection.setRequestProperty("X-Service-Auth-ID", this.compmId);

            if (httpHeaderFields != null) {
                for (String key : httpHeaderFields.keySet()) {
                    connection.setRequestProperty(key, httpHeaderFields.get(key));
                }
            }
            
            if (content != null) {
                if (httpHeaderFields != null) {
                    if (!httpHeaderFields.contains("Content-Type")) {
                        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    }
                    if (!httpHeaderFields.contains("Content-Length")) {
                        connection.setRequestProperty("Content-Length", Integer.toString(
                            content.getBytes("UTF-8").length
                        ));
                    }
                        
                }
                DataOutputStream stream = new DataOutputStream(connection.getOutputStream());
                stream.write(content.getBytes("UTF-8"));
                stream.flush();
                stream.close();
            }

            //Get Response
            java.io.InputStream is = null;
            if (connection.getResponseCode() >= 200 & connection.getResponseCode() < 400) {
                is = connection.getInputStream();
            } else {
                is = connection.getErrorStream();
            }
            Object o = connection.getContentType();
            
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line = rd.readLine();
            while (line != null) {
                response.append(line);
                response.append('\r');
                line = rd.readLine();
            }
            rd.close();
            is.close();
            this.message = response.toString();
            this.responseCode = connection.getResponseCode();
            this.responseHeader = connection.getHeaderFields();
            connection.disconnect();
            return new HttpResponseResult(
                this.responseCode, this.message, null, this.responseHeader == null ? null : this.responseHeader, url
            );
        } catch (Exception e) {
            throw(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
