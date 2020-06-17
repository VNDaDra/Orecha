package edu.dadra.orecha.Model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Message {
    private String id;
    private String roomId;
    private String senderId;
    private String message;
    private Timestamp time;
    private String type;
    private FileMessage file;

    public Message() {
    }

    public Message(String id, String roomId, String senderId, String message, Timestamp time, String type) {
        this.id = id;
        this.roomId = roomId;
        this.senderId = senderId;
        this.message = message;
        this.time = time;
        this.type = type;
    }

    public Message(String id, String roomId, String senderId, String message, Timestamp time, String type, FileMessage file) {
        this.id = id;
        this.roomId = roomId;
        this.senderId = senderId;
        this.message = message;
        this.time = time;
        this.type = type;
        this.file = file;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public FileMessage getFile() {
        return file;
    }

    public void setFile(FileMessage file) {
        this.file = file;
    }
}
