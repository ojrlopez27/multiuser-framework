package communication;

/**
 * Created by oscarr on 3/3/17.
 */
public class SessionMessage {
    private String requestType = "";
    private String sessionId = "";
    private String payload = "";
    private String messageId;

    public SessionMessage(String messageId, String payload) {
        this.messageId = messageId;
        this.payload = payload;
    }

    public SessionMessage(String requestType, String sessionId, String payload) {
        this.requestType = requestType;
        this.sessionId = sessionId;
        this.payload = payload;
    }

    public SessionMessage(String requestType){
        this.requestType = requestType;
    }

    public SessionMessage() {
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}