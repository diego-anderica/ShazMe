package es.uclm.esi.multimedia.fingerprinting;

import java.util.logging.Level;
import java.util.logging.Logger;
import android.content.Context;
import android.media.MediaCasException;
import android.widget.Toast;

import com.google.firebase.storage.StorageReference;

public class AudioFingerprinting {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args, StorageReference storageRef, Context ctx) {

        boolean exit = false;

        // Controlling input params
        switch (args.length) {
            case 1:
                // If we want to run matching
                if (args[0].equals("-matching")) {
                    AudioRecognizer fingerPrintingExample = new AudioRecognizer(storageRef, ctx);
                    // For matching we provide an empty string and isMatching=true

                    try {
                        fingerPrintingExample.listening("", true, storageRef, ctx);
                        String song = fingerPrintingExample.getBest();
                        Toast.makeText(ctx, "Song is: " + song, Toast.LENGTH_LONG).show();
                    } catch (MediaCasException e) {
                        e.printStackTrace();
                    }

                    exit = true;
                }
                break;
            case 2:
                // If we want to add a new song to the song repository
                String songId = args[1];
                if (args[0].equals("-add")) {
                    try {
                        AudioRecognizer fingerPrintingExample = new AudioRecognizer(storageRef, ctx);
                        // For adding a song we provide a string with its identifier (i.e., title) and isMatching=false
                        fingerPrintingExample.listening(songId, false, storageRef, ctx);
                        exit = true;
                    } catch (MediaCasException ex) {
                        Logger.getLogger(AudioFingerprinting.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                break;
        }
        if (!exit) {
            System.err.println("\nUsage:     java AudioFingerprinting -matching | -add \"songTitle\"");
            System.err.println("Example (1): java AudioFingerprinting -add \"Wish you were here\"");
            System.err.println("Example (2): java AudioFingerprinting -matching\n\n");
        }
    }
}
