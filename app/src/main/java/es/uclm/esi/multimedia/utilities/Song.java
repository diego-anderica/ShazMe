package es.uclm.esi.multimedia.utilities;

import java.util.List;
import java.util.Map;

import es.uclm.esi.multimedia.fingerprinting.KeyPoint;

/**
 * Created by diego on 31/12/17.
 */

public class Song {
    Map<Long, List<KeyPoint>> songObject;

    public Song (Map<Long, List<KeyPoint>> songObject){
        this.songObject = songObject;
    }

    public Song(){

    }

    public Map<Long, List<KeyPoint>> getSongObject() {
        return songObject;
    }

    public void setSongObject(Map<Long, List<KeyPoint>> songObject) {
        this.songObject = songObject;
    }
}
