package sciserver.logging;

import java.time.Duration;
import java.time.Instant;

public class ServiceLogTimer {

    private ServiceLog log;
    private String key;
    private Instant start;
    private Long time;

    public ServiceLogTimer(ServiceLog log, String key) {
        this.log = log;
        this.key = key;
        this.start = Instant.now();
    }

    public void stop() {
        Duration duration = Duration.between(start, Instant.now());
        time = duration.toMillis();
        log.setCounter(key, time);
    }

    public Long getTime() {
        return time;
    }

}
