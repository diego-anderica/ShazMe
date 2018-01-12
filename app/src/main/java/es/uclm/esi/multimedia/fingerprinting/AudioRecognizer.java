package es.uclm.esi.multimedia.fingerprinting;

import es.uclm.esi.multimedia.serialization.Serialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaCasException;

import com.google.firebase.storage.StorageReference;

import es.uclm.esi.multimedia.utilities.HashingFunctions;
import es.uclm.esi.multimedia.utilities.Spectrum;

public class AudioRecognizer {

    // The main hashtable required in our interpretation of the algorithm to
    // store the song repository
    private Map<Long, List<KeyPoint>> hashMapSongRepository;

    // Variable to stop/start the listening loop
    public boolean running;

    public Context ctx;

    private String bestSongMatch;

    // Constructor
    public AudioRecognizer(StorageReference storageRef, Context ctx) {

        // Deserialize the hash table hashMapSongRepository (our song repository)
        this.hashMapSongRepository = Serialization.fillHashMap(storageRef, ctx);
        this.running = true;
        this.ctx = ctx;
    }

    // Method used to acquire audio from the microphone and to add/match a song fragment
    public void listening(String songid, boolean ismatching, StorageReference storageRef, Context context) throws MediaCasException {
        final Context contexto = context;
        final StorageReference storage = storageRef;
        final String songId = songid;
        final boolean isMatching = ismatching;

        final AudioRecord audiorecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, AudioParams.sampleRate, AudioParams.channels, AudioFormat.ENCODING_PCM_8BIT, AudioParams.bufferSize);
        audiorecorder.startRecording();

        Thread listeningThread = new Thread(new Runnable() {

            @Override
            public void run() {
                // Output stream
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                // Reader buffer
                byte[] buffer = new byte[AudioParams.bufferSize];
                int n = 0;
                try {
                    while (running) {
                        // Reading
                        int count = audiorecorder.read(buffer, 0, buffer.length);
                        // If buffer is not empty
                        if (count > 0) {
                            outStream.write(buffer, 0, count);
                        }
                    }

                    byte[] audioTimeDomain = outStream.toByteArray();

                    // Compute magnitude spectrum
                    double[][] magnitudeSpectrum = Spectrum.compute(audioTimeDomain);

                    // Determine the shazam action (add or matching) and perform it
                   shazamAction(magnitudeSpectrum, songId, isMatching);

                    // Close stream
                    outStream.close();

                    // Serialize again the hashMapSongRepository (our song repository)
                    Serialization.serializeHashMap(hashMapSongRepository, storage, contexto);
                } catch (IOException e) {
                    System.err.println("I/O exception " + e);
                    System.exit(-1);
                }
            }
        });

        // Start listening
        listeningThread.start();

        try {
            Thread.sleep(10000);
        } catch (Exception ex) {
            Logger.getLogger(AudioRecognizer.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.running = false;

    }

    // Determine the shazam action (add or matching a song) and perform it
    private void shazamAction(double[][] magnitudeSpectrum, String songId, boolean isMatching) {

        // Hash table used for matching (Map<songId, Map<offset,count>>)
        Map<String, Map<Integer, Integer>> matchMap = new HashMap<String, Map<Integer, Integer>>();

        // Iterate over all the chunks/ventanas from the magnitude spectrum
        for (int c = 0; c < magnitudeSpectrum.length; c++) {

            // Compute the hash entry for the current chunk/ventana (magnitudeSpectrum[c])
            long hashentry = computeHashEntry(magnitudeSpectrum[c]);

            // In the case of adding the song to the repository
            if (!isMatching) {

                // Adding keypoint to the list in its relative hash entry which has been computed before
                KeyPoint point = new KeyPoint(songId, c);

                if (!this.hashMapSongRepository.containsKey(hashentry)) {
                    // Create a new list of key points for this song
                    List<KeyPoint> listofkeys = new ArrayList<KeyPoint>();
                    listofkeys.add(point);
                    this.hashMapSongRepository.put(hashentry, listofkeys);
                } else {
                    // Update the list with the new key point
                    List<KeyPoint> songlist = this.hashMapSongRepository.get(hashentry);
                    songlist.add(point);
                }

            }
            // In the case of matching a song fragment
            else {
                // Iterate over the list of keypoints that matches the hash entry
                // in the the current chunk
                // For each keypoint:
                // Compute the time offset (Math.abs(point.getTimestamp() - c))

                List<KeyPoint> listn = this.hashMapSongRepository.get(hashentry);
                if (listn != null) {
                    for (KeyPoint kp : listn) {
                        int time = kp.getTimestamp();
                        String id = kp.getSongId();
                        // Compute the offset
                        int offset = Math.abs(time - c);

                        // If the song does not exist in the match map then we create an entry for this song
                        // and add the specific offset
                        if (!matchMap.containsKey(id)) {
                            Map<Integer, Integer> smap = new HashMap<Integer, Integer>();
                            smap.put(offset, 1);
                            matchMap.put(id, smap);

                            // If the song already exists in the match map, then we increase the counter for
                            // the specific offset if it is already registered, or we create an entry for it
                        } else {
                            Map<Integer, Integer> offsetmap = matchMap.get(id);

                            if (!offsetmap.containsKey(offset)) {
                                offsetmap.put(offset, 1);
                            } else {
                                Integer cont = offsetmap.get(offset);
                                offsetmap.put(offset, cont + 1);
                            }
                        }

                    }
                }
            }
        } // End iterating over the chunks/ventanas of the magnitude spectrum
        // If we chose matching, we show the best song matching
        if (isMatching) {
            showBestMatching(matchMap);
        }
    }

    // Find out in which range the frequency is
    private int getIndex(int freq) {

        int i = 0;
        while (AudioParams.range[i] < freq) {
            i++;
        }
        return i;
    }

    // Compute hash entry for the chunk/ventana spectra
    private long computeHashEntry(double[] chunk) {

        // Variables to determine the hash entry for this chunk/window spectra
        double highscores[] = new double[AudioParams.range.length];
        int frequencyPoints[] = new int[AudioParams.range.length];

        for (int freq = AudioParams.lowerLimit; freq < AudioParams.unpperLimit - 1; freq++) {
            // Get the magnitude
            double mag = chunk[freq];
            // Find out which range we are in
            int index = getIndex(freq);
            // Save the highest magnitude and corresponding frequency:
            if (mag > highscores[index]) {
                highscores[index] = mag;
                frequencyPoints[index] = freq;
            }
        }
        // Hash function
        return HashingFunctions.hash1(frequencyPoints[0], frequencyPoints[1],
                frequencyPoints[2], frequencyPoints[3], AudioParams.fuzzFactor);
    }

    // Method to find the songId with the most frequently/repeated time offset
    private void showBestMatching(Map<String, Map<Integer, Integer>> matchMap) {

        //String bestsong = "";
        int bestmatch = 0;

        // Iterate over the hash map to compare the counter of all the offset
        for (Map.Entry<String, Map<Integer, Integer>> entry : matchMap.entrySet()) { // Every song
            String idsong = entry.getKey();
            Map<Integer, Integer> offsetmap = entry.getValue();
            int biggestoffset_of_a_song = 0;

            for (Map.Entry<Integer, Integer> entry2 : offsetmap.entrySet()) { // Every offset of the song
                int current_cont = entry2.getValue();

                if (current_cont > biggestoffset_of_a_song) {
                    biggestoffset_of_a_song = current_cont;
                }

            }

            if (biggestoffset_of_a_song > bestmatch) {
                bestmatch = biggestoffset_of_a_song;
                setBest(idsong);
            }
        }

        // Print the songId string which represents the best matching
        System.out.println("Best song: " + getBest());
    }

    public String getBest(){
        return this.bestSongMatch;
    }

    public void setBest(String best){
        this.bestSongMatch = best;
    }
}