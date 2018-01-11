package es.uclm.esi.multimedia.serialization;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StorageReference;

import es.uclm.esi.multimedia.fingerprinting.*;
import es.uclm.esi.multimedia.shazam.R;
import es.uclm.esi.multimedia.utilities.Song;

import java.io.EOFException;
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

import static android.content.ContentValues.TAG;


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

    public static void downloadFile(StorageReference storageRef, Context ctx) {
        File fileNameOnDevice = new File(ctx.getExternalFilesDir(null) + "/" + "hashmap.ser");

        storageRef.getFile(fileNameOnDevice).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                return;
            }
        });

        System.out.println("Archivo: " + fileNameOnDevice.getAbsolutePath());
        //return fileNameOnDevice;

    }

    public static Map<Long, List<KeyPoint>> fillHashMap(StorageReference storageRef, Context ctx) {
        Map<Long, List<KeyPoint>> hashMap = new HashMap<Long, List<KeyPoint>>();

        downloadFile(storageRef, ctx);
        File readFile = new File(ctx.getExternalFilesDir(null), "hashmap.ser");
        String location = ctx.getExternalFilesDir(null) + "/" + "hashmap.ser";

        FileInputStream fis = null;
        ObjectInputStream ois = null;

        try {
            fis = new FileInputStream(location);
            ois = new ObjectInputStream(fis);

            try {
                while (true) {
                    hashMap = (Map<Long, List<KeyPoint>>) ois.readObject();
                }
            } catch (EOFException eof) {
                //EOF reached, do nothing
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Este es el hashmap " + hashMap);

        return hashMap;
    }

    public static Map<Long, List<KeyPoint>> retrieveDatabase(Context ctx, FirebaseFirestore db) {
        Map<Long, List<KeyPoint>> hashMap = new HashMap<Long, List<KeyPoint>>();

        return hashMap;
    }

    public static void countSongs(FirebaseFirestore db) {
        db.collection("Canciones")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int count = 0;
                            for (DocumentSnapshot document : task.getResult()) {
                                count++;
                            }
                            System.out.println("Contador: " + count);
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }
}