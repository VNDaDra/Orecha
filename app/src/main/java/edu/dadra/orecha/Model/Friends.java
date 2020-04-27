package edu.dadra.orecha.Model;


import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Friends {
    private String id;
    private String email;
    private String displayName;
    private String phone;
    private String photoUrl;
    private String roomId;
    private Boolean hasChat;
    private Timestamp lastMessageTime;

    public Friends() {
    }

    public Friends(String id, String email, String displayName, String phone, String photoUrl,
                   String roomId, Boolean hasChat, Timestamp lastMessageTime) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.phone = phone;
        this.photoUrl = photoUrl;
        this.roomId = roomId;
        this.hasChat = hasChat;
        this.lastMessageTime = lastMessageTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Boolean getHasChat() {
        return hasChat;
    }

    public void setHasChat(Boolean hasChat) {
        this.hasChat = hasChat;
    }

    public Timestamp getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Timestamp lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
}
