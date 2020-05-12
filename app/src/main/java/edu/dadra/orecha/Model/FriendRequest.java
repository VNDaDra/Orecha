package edu.dadra.orecha.Model;

import com.google.firebase.Timestamp;

public class FriendRequest {
    private String senderId;
    private String senderName;
    private String receiverId;
    private String state;
    private String senderAvatar;
    private Timestamp time;

    public FriendRequest() {}

    public FriendRequest(String senderId, String senderName, String receiverId, String state, String senderAvatar, Timestamp time) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.state = state;
        this.senderAvatar = senderAvatar;
        this.time = time;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSenderAvatar() {
        return senderAvatar;
    }

    public void setSenderAvatar(String senderAvatar) {
        this.senderAvatar = senderAvatar;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
}
