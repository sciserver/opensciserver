package sciserver.logging;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.testng.Assert.*;
import org.testng.annotations.*;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;


public class LoggingTest {

    private Logger logger;
    private BufferedReader reader;

    @BeforeClass
    private void setUp() throws IOException {
        Logger.setSelfDebugOn();
        logger = Logger.FromConfig("src/test/resources/config.properties");
        reader = new BufferedReader(new FileReader("build/tmp/test/sciserver.log"));
    }

    @AfterMethod
    private void seekFile() throws IOException {
        while (reader.readLine() != null) {}
    }

    private JsonNode getLastLogMessageJsonNode () throws Exception {
        String line = reader.readLine();
        if (line == null)
            throw new Exception("no log line available to read!");
        ObjectMapper objectMapper = new ObjectMapper();
        JsonParser jsonParser = objectMapper.getFactory().createParser(line);
        JsonNode node = objectMapper.readTree(jsonParser);
        System.out.println(node.toString());
        return node;
    }

    @Test
    private void logMessageToFileNoExceptionTest() throws Exception {
        Message msg = logger.createInfoMessage("some info");
        logger.SendMessage(msg);
    }

    @Test
    private void logMessageUpdatesMethodOnSendingTest() throws Exception {
        Message msg = logger.createInfoMessage("some more info");
        assertNull(msg.Method);
        logger.SendMessage(msg);
        JsonNode node = getLastLogMessageJsonNode();
        assertEquals(node.get("Method").textValue(),
                     "sciserver.logging.LoggingTest.logMessageUpdatesMethodOnSendingTest");
    }

    @Test
    private void withFileEnabledAndNonexistentPathIgnoresTest() throws Exception {
        Logger.FromConfig("src/test/resources/config.properties.nologdir");
        Message msg = logger.createInfoMessage("some info");
        logger.SendMessage(msg);
    }

    @Test
    private void withFileDisabledDoesNotTryToOpenLogFileTest() throws Exception {
        Logger.FromConfig("src/test/resources/config.properties.filedisabled");
    }

    @Test
    private void logServiceLogMessageSerializes() throws Exception {
        ServiceLog log = logger.createServiceLog();
        log.setAttr("testattr", "value");
        logger.SendMessage(log);
        JsonNode node = getLastLogMessageJsonNode();
        assertEquals(node.get("attrs").get("testattr").textValue(), "value");
    }

    @Test
    private void logServiceLogSerializationCompletesMessage() throws Exception {
        ServiceLog log = logger.createServiceLog();
        logger.SendMessage(log);
        assert(log.isCompleted());
        JsonNode node = getLastLogMessageJsonNode();
        assert(node.get("requestTime").asDouble(-1) > 0);
    }

    @Test
    private void logServiceLogCounterAdd() throws Exception {
        ServiceLog log = logger.createServiceLog();
        log.addToCounter("counter", 1);
        log.addToCounter("counter", 2);
        log.addToCounter("counter");
        logger.SendMessage(log);
        JsonNode node = getLastLogMessageJsonNode();
        assertEquals(node.get("counters").get("counter").asLong(), 4);
    }

    @Test
    private void logServiceLogTimer() throws Exception {
        ServiceLog log = logger.createServiceLog();
        ServiceLogTimer timer = log.startTimer("timer");
        Thread.sleep(100);
        timer.stop();
        Long timer_time = timer.getTime();
        logger.SendMessage(log);
        JsonNode node = getLastLogMessageJsonNode();
        assert(node.get("counters").get("timer").asLong() == timer_time);
    }

    @Test
    private void logServiceLogTimerUnclosedNoFail() throws Exception {
        ServiceLog log = logger.createServiceLog();
        ServiceLogTimer timer = log.startTimer("timer");
        Thread.sleep(100);
        logger.SendMessage(log);
        JsonNode node = getLastLogMessageJsonNode();
        assertNull(node.get("counters").get("timer"));
    }

    @Test
    private void remoteLoggerDoesNotFailOnNoRabbitMQ() throws Exception {
        Logger rlogger = new Logger();
        rlogger.enableRabbitMq("host", -1, "exchange", "queue");
        rlogger.SendMessage(rlogger.createInfoMessage("test message"));
    }
}
