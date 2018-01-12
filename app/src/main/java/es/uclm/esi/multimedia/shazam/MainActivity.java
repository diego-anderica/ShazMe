package es.uclm.esi.multimedia.shazam;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.support.v4.app.ActivityCompat;
import android.support.annotation.NonNull;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import es.uclm.esi.multimedia.fingerprinting.AudioFingerprinting;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener, ActivityCompat.OnRequestPermissionsResultCallback{

    private Context ctx = this;

    private static final String TAG = "MainActivity";
    public static final String ANONYMOUS = "anonymous";

    private SharedPreferences mSharedPreferences;
    private String mUsername;
    private String mPhotoUrl;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    private GoogleApiClient mGoogleApiClient;

    private Button btnAddCancion;
    private ImageButton btnMatching;

    private EditText txtNombrecancion;

    // Access a Cloud Firestore instance from the Activity
    private FirebaseFirestore mFirestore;

    //Firebase Storage instance
    private FirebaseStorage storage;

    // Create a storage reference from our app
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mUsername = ANONYMOUS;

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        // Enable Firestore logging
        FirebaseFirestore.setLoggingEnabled(true);

        // Initialize Firestore and the main RecyclerView
        initFirestore();

        //Initialize instance Firebase Storage
        storage = FirebaseStorage.getInstance();

        // Create a storage reference from our app to the serialized file
        storageRef = storage.getReference();

        btnAddCancion = findViewById(R.id.btnAddCancion);
        btnMatching = findViewById(R.id.btnMatching);
        txtNombrecancion = findViewById(R.id.txtNombreCancion);

        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mUsername = mFirebaseUser.getDisplayName();
            if (mFirebaseUser.getPhotoUrl() != null) {
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            }
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnMatching.setOnClickListener(new View.OnClickListener() {
            String[] cad = {"-matching"};

            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(getCtx(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getCtx(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&ActivityCompat.checkSelfPermission(getCtx(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Escuchando canción (10 seg.)...", Toast.LENGTH_LONG).show();
                    AudioFingerprinting.main(cad, storageRef, getCtx());
                } else {
                    requestAudioPermission();
                }
            }
        });

        btnAddCancion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(getCtx(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    if (txtNombrecancion.getText().toString().equalsIgnoreCase("")){
                        Toast.makeText(MainActivity.this, "Escribe un título de canción", Toast.LENGTH_LONG).show();
                    }else {
                        String[] cad = {"-add", txtNombrecancion.getText().toString()};

                        Toast.makeText(MainActivity.this, "Escuchando nueva canción (10 seg.)...", Toast.LENGTH_LONG).show();

                        txtNombrecancion.setText("");

                        AudioFingerprinting.main(cad, storageRef, getCtx());
                    }
                }
            }
        });
    }

    public void requestAudioPermission(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // El código 0 significa el permiso de grabar audio.
        if (requestCode == 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Los permisos para grabar se han concedido.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Los permisos para grabar NO se han concedido.", Toast.LENGTH_LONG).show();
            }
        }else if(requestCode == 1) {
            if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Los permisos para escribir archivos se han concedido.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Los permisos para escribir archivos NO se han concedido.", Toast.LENGTH_LONG).show();
            }
        }else if(requestCode == 2){
            if (grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Los permisos para leer archivos se han concedido.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Los permisos para leer archivos NO se han concedido.", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void initFirestore() {
        mFirestore = FirebaseFirestore.getInstance();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnuBtnSignOut:
                mFirebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                mFirebaseUser = null;
                mUsername = ANONYMOUS;
                mPhotoUrl = null;
                startActivity(new Intent(this, SignInActivity.class));
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    public Context getCtx() {
        return ctx;
    }

}