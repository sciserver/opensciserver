package sciserver.logging;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class Logger {

    public String applicationHost;
    public String applicationName;
    public String exchangeName;
    public String messagingHost;
    private int messagingPort = -1; // -1 if unused
    public boolean enabled = true;
    public boolean remoteEnabled = false;
    public boolean fileEnabled = false;
    public boolean consoleEnabled = false;
    public String logFileName;
    private String method = null;
    private FileOutputStream logFile;
    private RabbitMQPublisher rabbitMQPublisher;
    private static boolean selfDebug = false;

    protected static void setSelfDebugOn() {
        selfDebug = true;
    }

    protected static void setSelfDebugOff() {
        selfDebug = true;
    }

    protected static void selfDebug(String msg) {
        if (selfDebug)
            System.err.println(msg);
    }

    public void setLoggerPropertiesFromConfig(String configFile) {
        try {
            Properties prop = new Properties();
            String propFileName = configFile;
            FileInputStream inputStream = new FileInputStream(propFileName);
            prop.load(inputStream);

            this.applicationHost = prop.getProperty("Log.ApplicationHost");
            this.applicationName = prop.getProperty("Log.ApplicationName");
            this.messagingHost = prop.getProperty("Log.MessagingHost");
            this.messagingPort = Integer.parseInt(prop.getProperty("Log.MessagingPort", "-1"));
            this.exchangeName = prop.getProperty("Log.ExchangeName");
            this.enabled = Boolean.parseBoolean(prop.getProperty("Log.Enabled"));
            this.remoteEnabled = this.enabled;
            this.fileEnabled = Boolean.parseBoolean(prop.getProperty("Log.FileEnabled"));
            this.logFileName = prop.getProperty("Log.FileName", "sciserver.log");
            if (this.fileEnabled)
                enableFileOutput(this.logFileName);
        } catch (Exception e) {
        }
    }

    public void enableRabbitMq(String host, int port, String exchange, String queue) {
        remoteEnabled = true;
        messagingHost = host;
        messagingPort = port;
        exchangeName = exchange;
    }

    public void enableFileOutput(String filename) throws FileNotFoundException {
        fileEnabled = true;
        logFile = new FileOutputStream(filename);
    }

    public void enableConsoleOutput() {
        consoleEnabled = true;
    }

    public void setLoggerPropertiesFromConfig() {
        setLoggerPropertiesFromConfig("config.properties");
    }

    public void setLoggerProperties(
        String applicationHost, String applicationName, String messagingHost,
        String databaseQueueName, String exchangeName, boolean enabled
    ) {
        this.applicationHost = applicationHost;
        this.applicationName = applicationName;
        this.messagingHost = messagingHost;
        this.exchangeName = exchangeName;
        this.enabled = enabled;
        this.remoteEnabled = enabled;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public void setMessagingPort(int port) {
        this.messagingPort = port;
    }

    public static Logger FromConfig() {
        return new Logger();
    }

    public static Logger FromConfig(String configFile) {
        return new Logger(configFile);
    }

    public Logger() {
        setLoggerPropertiesFromConfig();
    }

    public Logger (String method, String applicationHost, String applicationName, String messagingHost, String databaseQueueName, String exchangeName, boolean enabled) {
        setMethod(method);
        setLoggerProperties(applicationHost, applicationName, messagingHost, databaseQueueName, exchangeName, enabled );
    }

    public Logger(String applicationHost, String applicationName, String messagingHost, String databaseQueueName, String exchangeName, boolean enabled ) {
        setLoggerProperties(applicationHost, applicationName, messagingHost, databaseQueueName, exchangeName, enabled );
    }

    public Logger(String configFile) {
        setLoggerPropertiesFromConfig(configFile);
    }

    public void Connect() {
    }

    private void setupMessage(Message msg) {
        if (applicationHost == null) {
            try {
                applicationHost = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                applicationHost = "unknown";
            }
        }
        msg.Host = applicationHost;
        msg.Application = applicationName;
        msg.Time = LoggingTime.now();
        msg.MessageId = UUID.randomUUID();
    }

    private Message createMessage() {
        Message msg = new Message();
        setupMessage(msg);
        return msg;
    }

    public ServiceLog createServiceLog() {
        ServiceLog log = new ServiceLog();
        setupMessage(log);
        log.MessageType = MessageType.SERVICELOG;
        return log;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return this.method;
    }

    public Message createFatalMessage(Exception ex, String content) {
        Message msg = createMessage();
        msg.MessageType = MessageType.FATAL;
        addExceptionBody(msg, ex, content);
        return msg;
    }

    public Message createErrorMessage(Exception ex, String content) {
        Message msg = createMessage();
        msg.MessageType = MessageType.ERROR;
        addExceptionBody(msg, ex, content);
        return msg;
    }

    private void addExceptionBody(Message msg, Exception ex, String content) {
        ExceptionMessageBody body = new ExceptionMessageBody();
        body.ExceptionText = ex.getMessage();
        body.ExceptionType = ex.getClass().getCanonicalName();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        body.StackTrace = sw.toString();

        body.Content = content;
        body.$type = "SciServer.Logging.ExceptionMessageBody, SciServer.Logging";
        msg.MessageBody = body;
    }

    public Message createWarnMessage(String text) {
        Message msg = createMessage();
        msg.MessageType = MessageType.WARN;
        addTextBody(msg, text);
        return msg;
    }

    public Message createInfoMessage(String text) {
        Message msg = createMessage();
        msg.MessageType = MessageType.INFO;
        addTextBody(msg, text);
        return msg;
    }

    public Message createCustomMessage(String customType, String content) {
        Message msg = createMessage();
        msg.MessageType = MessageType.CUSTOM;
        addCustomBody(msg, customType, content);
        return msg;
    }

    private void addCustomBody(Message msg, String customType, String content) {
        CustomMessageBody body = new CustomMessageBody();
        body.CustomType = customType;
        body.Content = content;
        body.$type = "SciServer.Logging.CustomMessageBody, SciServer.Logging";
        msg.MessageBody = body;
    }

    public Message createSkyserverMessage(String content, boolean doShowInUserHistory) {
        Message msg = createMessage();
        msg.MessageType = MessageType.SKYSERVER;
        addSkyserverBody(msg, content, doShowInUserHistory);
        return msg;
    }

    private void addSkyserverBody(Message msg, String content, boolean doShowInUserHistory) {
        SkyserverMessageBody body = new SkyserverMessageBody();
        body.Content = content;
        body.DoShowInUserHistory = doShowInUserHistory;
        body.$type = "SciServer.Logging.SkyserverMessageBody, SciServer.Logging";
        msg.MessageBody = body;
    }

    public Message createCasJobsMessage(String content, boolean doShowInUserHistory) {
        Message msg = createMessage();
        msg.MessageType = MessageType.CASJOBS;
        addCasJobsBody(msg, content, doShowInUserHistory);
        return msg;
    }

    private void addCasJobsBody(Message msg, String content, boolean doShowInUserHistory) {
        CasJobsMessageBody body = new CasJobsMessageBody();
        body.Content = content;
        body.DoShowInUserHistory = doShowInUserHistory;
        body.$type = "SciServer.Logging.CasJobsMessageBody, SciServer.Logging";
        msg.MessageBody = body;
    }

    public Message createSciDriveMessage(String content, boolean doShowInUserHistory) {
        Message msg = createMessage();
        msg.MessageType = MessageType.SCIDRIVE;
        addSciDriveBody(msg, content, doShowInUserHistory);
        return msg;
    }

    private void addSciDriveBody(Message msg, String content, boolean doShowInUserHistory) {
        SciDriveMessageBody body = new SciDriveMessageBody();
        body.Content = content;
        body.DoShowInUserHistory = doShowInUserHistory;
        body.$type = "SciServer.Logging.SciDriveMessageBody, SciServer.Logging";
        msg.MessageBody = body;
    }

    public Message createSkyQueryMessage(String content, boolean doShowInUserHistory) {
        Message msg = createMessage();
        msg.MessageType = MessageType.SKYQUERY;
        addSkyQueryBody(msg, content, doShowInUserHistory);
        return msg;
    }

    private void addSkyQueryBody(Message msg, String content, boolean doShowInUserHistory) {
        SkyQueryMessageBody body = new SkyQueryMessageBody();
        body.Content = content;
        body.DoShowInUserHistory = doShowInUserHistory;
        body.$type = "SciServer.Logging.SkyQueryMessageBody, SciServer.Logging";
        msg.MessageBody = body;
    }

    public Message createComputeMessage(String content, boolean doShowInUserHistory) {
        Message msg = createMessage();
        msg.MessageType = MessageType.COMPUTE;
        addComputeBody(msg, content, doShowInUserHistory);
        return msg;
    }

    private void addComputeBody(Message msg, String content, boolean doShowInUserHistory) {
        ComputeMessageBody body = new ComputeMessageBody();
        body.Content = content;
        body.DoShowInUserHistory = doShowInUserHistory;
        body.$type = "SciServer.Logging.ComputeMessageBody, SciServer.Logging";
        msg.MessageBody = body;
    }

    public Message createAuthenticationMessage(String content, boolean doShowInUserHistory) {
        Message msg = createMessage();
        msg.MessageType = MessageType.AUTHENTICATION;
        addAuthenticationBody(msg, content, doShowInUserHistory);
        return msg;
    }

    private void addAuthenticationBody(Message msg, String content, boolean doShowInUserHistory) {
        AuthenticationMessageBody body = new AuthenticationMessageBody();
        body.Content = content;
        body.DoShowInUserHistory = doShowInUserHistory;
        body.$type = "SciServer.Logging.AuthenticationMessageBody, SciServer.Logging";
        msg.MessageBody = body;
    }

    public Message createCOMPMMessage(String content) {
        Message msg = createMessage();
        msg.MessageType = MessageType.COMPM;
        addCOMPMBody(msg, content);
        return msg;
    }

    private void addCOMPMBody(Message msg, String content) {
        COMPMMessageBody body = new COMPMMessageBody();
        body.Content = content;
        body.$type = "SciServer.Logging.COMPMMessageBody, SciServer.Logging";
        msg.MessageBody = body;
    }

    public Message createJOBMMessage(String content, boolean doShowInUserHistory) {
        Message msg = createMessage();
        msg.MessageType = MessageType.JOBM;
        addJOBMBody(msg, content, doShowInUserHistory);
        return msg;
    }

    private void addJOBMBody(Message msg, String content, boolean doShowInUserHistory) {
        JOBMMessageBody body = new JOBMMessageBody();
        body.Content = content;
        body.DoShowInUserHistory = doShowInUserHistory;
        body.$type = "SciServer.Logging.JOBMMessageBody, SciServer.Logging";
        msg.MessageBody = body;
    }

    public Message createRACMMessage(String content, boolean doShowInUserHistory) {
        Message msg = createMessage();
        msg.MessageType = MessageType.RACM;
        addRACMBody(msg, content, doShowInUserHistory);
        return msg;
    }

    private void addRACMBody(Message msg, String content, boolean doShowInUserHistory) {
        RACMMessageBody body = new RACMMessageBody();
        body.Content = content;
        body.DoShowInUserHistory = doShowInUserHistory;
        body.$type = "SciServer.Logging.RACMMessageBody, SciServer.Logging";
        msg.MessageBody = body;
    }

    public Message createFileServiceMessage(String content, boolean doShowInUserHistory) {
        Message msg = createMessage();
        msg.MessageType = MessageType.FILESERVICE;
        addFileServiceBody(msg, content, doShowInUserHistory);
        return msg;
    }

    private void addFileServiceBody(Message msg, String content, boolean doShowInUserHistory) {
        FileServiceMessageBody body = new FileServiceMessageBody();
        body.Content = content;
        body.DoShowInUserHistory = doShowInUserHistory;
        body.$type = "SciServer.Logging.FileServiceMessageBody, SciServer.Logging";
        msg.MessageBody = body;
    }

    public Message createDebugMessage(String text) {
        Message msg = createMessage();
        msg.MessageType = MessageType.DEBUG;
        addTextBody(msg, text);
        return msg;
    }

    private void addTextBody(Message msg, String text) {
        TextMessageBody body = new TextMessageBody();
        body.Text = text;
        body.$type = "SciServer.Logging.TextMessageBody, SciServer.Logging";
        msg.MessageBody = body;
    }

    public void SendMessage(Message msg) throws Exception {
        String message = "";

        if (remoteEnabled || fileEnabled || consoleEnabled) {
            if(msg.getMethod() == null) {
                if(getMethod() == null) {
                    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
                    int numLevelsUp = 2;
                    msg.setMethod(stackTraceElements[numLevelsUp].getClassName() + "." +
                                  stackTraceElements[numLevelsUp].getMethodName());
                } else {
                    msg.setMethod(getMethod());
                }
                setMethod(null); //reset method name;
            }
            message = msg.asJson();
        }

        if (remoteEnabled) {
            if (rabbitMQPublisher == null)
                rabbitMQPublisher = new RabbitMQPublisher(messagingHost, messagingPort, exchangeName);
            rabbitMQPublisher.enqueue(message);
        }

        if (fileEnabled) {
            try {
                logFile.write(message.getBytes());
                logFile.write('\n');
            } catch (IOException e) {
            }
        }
        if (consoleEnabled) {
            System.out.println(message);
        }
    }

    public void Dispose() throws Exception {
    }

}
