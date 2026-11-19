package compm.application;

import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * The `HttpResponseResult` class represents the result and metadata of an HTTP response.
 */
public class HttpResponseResult {

    int responseCode;
    String message;
    Exception exception;
    Map<String, List<String>> responseHeader;
    URL requestUrl; 

    HttpResponseResult(int responseCode, String message, Exception exception, 
                       Map<String, List<String>> responseHeader, URL requestUrl) {
        this.responseCode = responseCode;
        this.message = message;
        this.exception = exception;
        this.responseHeader = responseHeader;
        this.requestUrl = requestUrl;
    }

    HttpResponseResult(int responseCode, String message, Exception exception) {
        this.responseCode = responseCode;
        this.message = message;
        this.exception = exception;
        this.responseHeader = null;
        this.requestUrl = null;
    }


    HttpResponseResult() {
        this.responseCode = 0;
        this.message = null;
        this.exception = null;
        this.responseHeader = null;
        this.requestUrl = null;
    }

}
