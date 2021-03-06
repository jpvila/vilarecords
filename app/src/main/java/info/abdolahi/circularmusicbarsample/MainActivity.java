package info.abdolahi.circularmusicbarsample;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import info.abdolahi.CircularMusicProgressBar;
public class MainActivity extends AppCompatActivity {
    private MediaPlayerService player;
    boolean serviceBound = false;
    CircularMusicProgressBar progressBar;
    TextView currentTime;
    Thread runner = null;
    ArrayList<Audio> audioList;
    TextView albumSong;
    TextView albumGroup;
    ImageButton playPause;
    ImageButton playResume;
    static public int audioIndex = 0;
    static public boolean isPlaying = true;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 0;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.valdioveliu.valdio.audioplayer.PlayNewAudio";
    public static final String Broadcast_PLAY_NEXT_AUDIO = "com.valdioveliu.valdio.audioplayer.PlayNextAudio";
    public static final String Broadcast_PAUSE_AUDIO = "com.valdioveliu.valdio.audioplayer.PauseAudio";
    public static final String Broadcast_RESUME_AUDIO = "com.valdioveliu.valdio.audioplayer.ResumeAudio";

    // Change to your package name

    public BroadcastReceiver myReceiverPorcentajeCurrentTime = new BroadcastReceiver() {
        //CircularMusicProgressBar pBar = (CircularMusicProgressBar) findViewById(R.id.album_art);
        @Override
        public void onReceive(Context context, Intent intent) {
            int porcentajeCurrentPosition = intent.getIntExtra("PORCENTAJE_CURRENT_POSITION", 0);
           // pBar.setValue(ola);
            progressBar.setValue(porcentajeCurrentPosition);
            currentTime.setText(intent.getStringExtra("CURRENT_POSITION") + " / " + intent.getStringExtra("DURATION"));

        }
    };

    public BroadcastReceiver myReceiverCambiarIconoPlayAndPause = new BroadcastReceiver() {
        //CircularMusicProgressBar pBar = (CircularMusicProgressBar) findViewById(R.id.album_art);
        @Override
        public void onReceive(Context context, Intent intent) {
            isPlaying = intent.getBooleanExtra("IS_PLAYING", false);
            cambiarImagenResumeToPause();
        }
    };

    public BroadcastReceiver myReceiverAccionesReproductor = new BroadcastReceiver() {
        //CircularMusicProgressBar pBar = (CircularMusicProgressBar) findViewById(R.id.album_art);
        @Override
        public void onReceive(Context context, Intent intent) {
            String accion = intent.getStringExtra("BRODCAST_ACCIONES_REPRODUCTOR");
            if("AVANZAR_SIGUIENTE_CANCION".equals(accion)) {
                inicializarProgressReproductor();
                String albumSongString = intent.getStringExtra("ALBUM_SONG");
                albumSong.setText(albumSongString);
            }
            if("RETROCEDER_CANCION".equals(accion)) {
                inicializarProgressReproductor();
                String albumSongString = intent.getStringExtra("ALBUM_SONG");
                albumSong.setText(albumSongString);
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = (CircularMusicProgressBar) findViewById(R.id.album_art);
        currentTime = (TextView) findViewById(R.id.currentTime);
        albumSong= (TextView) findViewById(R.id.album_song);
        albumGroup= (TextView) findViewById(R.id.album_group);

        // set progress to 40%

        ImageButton boton_next_album_song = (ImageButton) findViewById(R.id.boton_next_album_song);
        boton_next_album_song.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Service is active
                //Send a broadcast to the service -> PLAY_NEW_AUDIO
//                Intent broadcastIntent = new Intent(Broadcast_PLAY_NEXT_AUDIO);
//                sendBroadcast(broadcastIntent);
                audioIndex++;
                new StorageUtil(getApplicationContext()).storeAudioIndex(MainActivity.audioIndex);
                inicializarProgressReproductor();
                playAudio(audioIndex);

            }
        });

        ImageButton boton_previous_album_song = (ImageButton) findViewById(R.id.boton_previous_album_song);
        boton_previous_album_song.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioIndex--;
                new StorageUtil(getApplicationContext()).storeAudioIndex(MainActivity.audioIndex);
                inicializarProgressReproductor();
                playAudio(audioIndex);

            }
        });

        playResume = (ImageButton) findViewById(R.id.play_resume);
        playResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cambiarImagenResumeToPause();
                Intent broadcastIntent = new Intent(Broadcast_RESUME_AUDIO);
                sendBroadcast(broadcastIntent);
            }
        });

        playPause = (ImageButton) findViewById(R.id.play_pause);
        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cambiarImagenResumeToPause();
                Intent broadcastIntent = new Intent(Broadcast_PAUSE_AUDIO);
                sendBroadcast(broadcastIntent);
            }
        });

        IntentFilter filter = new IntentFilter("BRODCAST_PORCENTAJE_CURRENT_POSITION");
        registerReceiver(myReceiverPorcentajeCurrentTime, filter);

        IntentFilter filterDatosCancion = new IntentFilter("BRODCAST_ACCIONES_REPRODUCTOR");
        registerReceiver(myReceiverAccionesReproductor, filterDatosCancion);

        IntentFilter filterIsPlaying = new IntentFilter("BRODCAST_IS_PLAYING");
        registerReceiver(myReceiverCambiarIconoPlayAndPause, filterIsPlaying);

        loadAudio();


        //playAudio(audioList.get(3).getData());
        playAudio(0);
        //playAudio("http://www.salerico.com/recetas/norajones.mp3");


    }

    private void cambiarImagenResumeToPause() {
        if(isPlaying){
            playPause.setVisibility(View.GONE);
            playResume.setVisibility(View.VISIBLE);
            isPlaying = false;
        } else{
            playPause.setVisibility(View.VISIBLE);
            playResume.setVisibility(View.GONE);
            isPlaying = true;
        }
    }

    private void inicializarProgressReproductor() {
        progressBar.setValueInit(0);
        progressBar.setValue(0);
    }

    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;

            Toast.makeText(MainActivity.this, "Service Bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private void playAudio(String media) {
        //Check is service is active
        if (!serviceBound) {
            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            playerIntent.putExtra("media", media);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Service is active
            //Send media with BroadcastReceiver
        }
    }

    private void playAudio(int audioIndex) {
        //Check is service is active
        //Setear los atributos del tema en el layout
        albumSong.setText(audioList.get(audioIndex).getTitle());
        albumGroup.setText(audioList.get(audioIndex).getArtist());

        if (!serviceBound) {
            //Store Serializable audioList to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(audioList);
            storage.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Store the new audioIndex to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudioIndex(audioIndex);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }
    private void loadAudio() {
        if (Build.VERSION.SDK_INT >= 23){
            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) !=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

            } else{
                loadSongStorage();
            }
        } else{
                loadSongStorage();
        }
    }
    public void pausar(){

    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)   {

                    // permission was granted, yay! Do the
                    // write to external strage operations here
                    loadSongStorage();

                } else {
                    Log.e("PERMISO NO ASIGNADO", "permiso no asignado");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                        /*You can forcefully again ask for the permissions to the user here but it is a bad practice*/
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void loadSongStorage() {
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, null, selection, null, sortOrder);
        } catch (Exception e){
            e.printStackTrace();
        }
        if (cursor != null && cursor.getCount() > 0) {
            audioList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));

                // Save to audioList
                audioList.add(new Audio(data, title, album, artist));
            }
        }
        cursor.close();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();

            //Desregistro el receiver del porcentaje
            unregisterReceiver(myReceiverPorcentajeCurrentTime);
        }
    }


}
