package es.uclm.esi.multimedia.fingerprinting;

import java.io.Serializable;

/**
 * Created by Ruth on 30/11/2017.
 */

public class KeyPoint implements Serializable{

    private final String songId;
    private final int timestamp;

    public KeyPoint(String songId, int timestamp){
        this.songId = songId;
        this.timestamp = timestamp;
    }

    public String getSongId() {
        return songId;
    }

    public int getTimestamp() {
        return timestamp;
    }
}
