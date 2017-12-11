package es.uclm.esi.multimedia.serialization;

import android.content.Context;
import android.content.res.Resources;

import es.uclm.esi.multimedia.fingerprinting.*;
import es.uclm.esi.multimedia.shazam.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by Ruth on 30/11/2017.
 */

public class Serialization {

    // The serialized <songId,keypoints> hash table is inside the "serialized"
    // directory in a file called "hashmap.ser"
    public static void serializeHashMap(Map<Long, List<KeyPoint>> hashMap) {

        try {
            File f = new File("res/raw");
            if (!f.exists()) {
                f.mkdir();
            }
            f = new File("raw/hashmap.ser");
            OutputStream fos = new FileOutputStream(f);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(hashMap);
            oos.close();
            fos.close();
        } catch (IOException ex) {
            System.out.println("Input/output error " + ex);
        }
    }

    public static Map<Long, List<KeyPoint>>  deserializeHashMap(Context ctx) {

        Map<Long, List<KeyPoint>> hashMap =  new HashMap<Long, List<KeyPoint>>();
        try {
            InputStream fis = ctx.getResources().openRawResource (R.raw.hashmap);
            ObjectInputStream ois = new ObjectInputStream(fis);
            hashMap = (Map<Long, List<KeyPoint>>) ois.readObject();
            ois.close();
            fis.close();
        } catch (FileNotFoundException ex) {
            System.out.println("\"hashmap.ser\" not found");
        } catch (IOException ex) {
            System.out.println("Input/output error " + ex);
        } catch (ClassNotFoundException ex) {
            System.out.println("Serialization error: class not found " + ex);
        }
        return hashMap;
    }
}