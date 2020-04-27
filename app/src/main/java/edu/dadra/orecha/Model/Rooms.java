package edu.dadra.orecha.Model;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Rooms {
    private String id;
    private String lastMessageId;

    public Rooms() {}

    public Rooms(String id, String lastMessageId) {
        this.id = id;
        this.lastMessageId = lastMessageId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(String lastMessageId) {
        this.lastMessageId = lastMessageId;
    }
}
