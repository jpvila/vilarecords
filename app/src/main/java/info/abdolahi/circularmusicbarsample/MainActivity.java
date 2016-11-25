package info.abdolahi.circularmusicbarsample;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import info.abdolahi.CircularMusicProgressBar;
public class MainActivity extends AppCompatActivity {
    private MediaPlayerService player;
    boolean serviceBound = false;
    CircularMusicProgressBar progressBar;
    TextView currentTime;
    Thread runner = null;

    public BroadcastReceiver myReceiver = new BroadcastReceiver() {
        //CircularMusicProgressBar pBar = (CircularMusicProgressBar) findViewById(R.id.album_art);
        @Override
        public void onReceive(Context context, Intent intent) {
            int ola = intent.getIntExtra("PORCENTAJE_CURRENT_POSITION", 0);

           // pBar.setValue(ola);
            progressBar.setValue(ola);
            currentTime.setText(intent.getStringExtra("CURRENT_POSITION") + " / " + intent.getStringExtra("DURATION"));
            Log.d("dddReceiver" ,  String.valueOf(ola) );   //can't see
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = (CircularMusicProgressBar) findViewById(R.id.album_art);
        currentTime = (TextView) findViewById(R.id.currentTime);
        // set progress to 40%
        progressBar.setValue(0);

        IntentFilter filter = new IntentFilter("MY_ACTION");
        registerReceiver(myReceiver, filter);
        playAudio("http://www.salerico.com/recetas/norajones.mp3");

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
        }
    }
}
