package sciserver.logging;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


@JsonInclude(Include.NON_NULL)
public class ServiceLog extends Message {

    private Instant timestamp;
    private Duration requestTime;
    private Map<String, String> attrs;
    private Map<String, Long> counters;
    private String error;

    public ServiceLog() {
        this.timestamp = Instant.now();
        this.counters = new HashMap<String, Long>();
        this.attrs = new HashMap<String, String>();
    }

    public void setError(String error_in) {
        error = error_in;
    }

    public void setAttr(String key, String val) {
        attrs.put(key, val);
    }

    public void setCounter(String key, Long val) {
        counters.put(key, val);
    }

    public void addToCounter(String key) {
        addToCounter(key, 1);
    }

    public void addToCounter(String key, int val) {
        addToCounter(key, Long.valueOf(val));
    }

    public void addToCounter(String key, Long val) {
        Long orig_val = counters.get(key);
        if (orig_val == null)
            counters.put(key, val);
        else
            counters.put(key, orig_val+val);
    }

    public ServiceLogTimer startTimer(String key) {
        return new ServiceLogTimer(this, key);
    }

    public void addRequestInfo(HttpServletRequest request) {
        setAttr("uri", request.getRequestURI());
        setAttr("query", request.getQueryString());
        setAttr("method", request.getMethod());
        if (request.getHeader("X-FORWARDED-FOR") != null)
            ClientIP = request.getHeader("X-FORWARDED-FOR").split(",")[0];
        else if (request.getHeader("HTTP_CLIENT_IP") != null)
            ClientIP = request.getHeader("HTTP_CLIENT_IP").split(",")[0];
        else
            ClientIP = request.getRemoteAddr();
    }

    public Long getTimestamp() {
        return timestamp.toEpochMilli();
    }

    public Long getRequestTime() {
        if (requestTime == null)
            requestTime = Duration.between(timestamp, Instant.now());
        return requestTime.toMillis();
    }

    public String getError() {
        return error;
    }

    public Map<String, Long> getCounters() {
        return counters;
    }

    public Map<String, String> getAttrs() {
        return attrs;
    }

    @JsonIgnore
    public boolean isCompleted() {
        return requestTime != null;
    }

}
