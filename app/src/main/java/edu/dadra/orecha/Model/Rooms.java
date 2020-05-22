package edu.dadra.orecha.Model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Rooms {
    private String roomId;
    private String friendId;
    private String displayName;
    private String photoUrl;
    private Timestamp lastMessageTime;

    public Rooms() {}

    public Rooms(String roomId, String friendId, String displayName, String photoUrl, Timestamp lastMessageTime) {
        this.roomId = roomId;
        this.friendId = friendId;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.lastMessageTime = lastMessageTime;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getFriendId() {
        return friendId;
    }

    public void setFriendId(String friendId) {
        this.friendId = friendId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Timestamp getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Timestamp lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
}
