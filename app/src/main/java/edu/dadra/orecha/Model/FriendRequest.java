package edu.dadra.orecha.Model;

public class FriendRequest {
    private String senderId;
    private String receiverId;
    private String state;
    private String senderAvatar;

    public FriendRequest() {}

    public FriendRequest(String senderId, String receiverId, String state, String senderAvatar) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.state = state;
        this.senderAvatar = senderAvatar;
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
}
