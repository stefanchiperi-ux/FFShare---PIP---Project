package model;


import java.io.Serializable;

public class ProtocolMessage implements Serializable {
    private static final long serialVersionUID = 1L;
	private MessageType type;
    private String sender;
    private String text;

    public ProtocolMessage(MessageType type, String sender, String text) {
        this.type = type;
        this.sender = sender;
        this.text = text;
    }

    public MessageType getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }
}


