package sciserver.logging;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Message {

    public String $type = "SciServer.Logging.Message, SciServer.Logging";
    public UUID MessageId;
    public MessageType MessageType;
    public String Time;
    public String Host;
    public String Application;
    public String Method;
    public String UserId;
    public String ClientIP;
    public String TaskName;
    public String UserName;
    public String UserToken;
    public MessageBody MessageBody;

    @JsonGetter("MessageId")
    public UUID getMessageId() {
        return MessageId;
    }

    @JsonSetter("MessageId")
    public void setMessageId(UUID messageId) {
        MessageId = messageId;
    }

    @JsonGetter("MessageType")
    public MessageType getMessageType() {
        return MessageType;
    }

    @JsonSetter("MessageType")
    public void setMessageType(MessageType messageType) {
        MessageType = messageType;
    }

    @JsonGetter("Time")
    public String getTime() {
        return Time;
    }

    @JsonSetter("Time")
    public void setTime(String time) {
        Time = time;
    }

    @JsonGetter("Host")
    public String getHost() {
        return Host;
    }

    @JsonSetter("Host")
    public void setHost(String host) {
        Host = host;
    }

    @JsonGetter("Application")
    public String getApplication() {
        return Application;
    }

    @JsonSetter("Application")
    public void setApplication(String application) {
        Application = application;
    }


    @JsonGetter("Method")
    public String getMethod() {
        return Method;
    }

    @JsonSetter("Method")
    public void setMethod(String method) {
        Method = method;
    }

    @JsonGetter("UserId")
    public String getUserId() {
        return UserId;
    }

    @JsonSetter("UserId")
    public void setUserId(String userId) {
        UserId = userId;
    }

    @JsonGetter("ClientIP")
    public String getClientIP() {
        return ClientIP;
    }

    @JsonSetter("ClientIP")
    public void setClientIP(String clientIP) {
        ClientIP = clientIP;
    }

    @JsonGetter("TaskName")
    public String getTaskName() {
        return TaskName;
    }

    @JsonSetter("TaskName")
    public void setTaskName(String taskName) {
        TaskName = taskName;
    }

    @JsonGetter("UserName")
    public String getUserName() {
        return UserName;
    }

    @JsonSetter("UserName")
    public void setUserName(String userName) {
        UserName = userName;
    }

    @JsonGetter("UserToken")
    public String getUserToken() {
        return UserToken;
    }

    @JsonSetter("UserToken")
    public void setUserToken(String userToken) {
        UserToken = userToken;
    }

    @JsonGetter("MessageBody")
    public MessageBody getMessageBody() {
        return MessageBody;
    }

    @JsonSetter("MessageBody")
    public void setMessageBody(MessageBody messageBody) {
        MessageBody = messageBody;
    }

    @JsonGetter("TypeName")
    public String getTypeName() {
        if (this.MessageType != sciserver.logging.MessageType.CUSTOM)
            return this.MessageType.toString();
        else{
            if(MessageBody == null )
                return null;
            else
                return ((CustomMessageBody)MessageBody).CustomType;
        }
    }

    public String asJson() throws JsonProcessingException {
        ObjectMapper serializer = new ObjectMapper();
        return serializer.writeValueAsString(this);
    }

}
