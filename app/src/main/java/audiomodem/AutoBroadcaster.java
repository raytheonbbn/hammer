package audiomodem;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;

import utils.ModemCotUtility;

import com.atakmap.android.cot_utility.plugin.PluginLifecycle;
import com.atakmap.android.maps.MapView;

import java.util.Random;

public class AutoBroadcaster implements Runnable {
    private ModemCotUtility modemCotUtility;
    private MapView mapView;
    private Thread thread;
    private boolean running;

    final static String TAG = "AutoBroadcaster";

    public AutoBroadcaster(ModemCotUtility modemCotUtility, MapView mapView){
        this.modemCotUtility = modemCotUtility;
        this.mapView = mapView;
        this.running = true;
        this.thread = new Thread(this);
    }
    @Override
    public void run() {
        Looper.prepare();

        int delay, r;
        SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
        Random rand = new Random();
        try {
            while(running) {
                r = rand.nextInt(10); // introduce a skew to help prevent a group's sync from all TX at the same time
                Thread.sleep(r*1000);
                delay = sharedPref.getInt("autoBroadcastInterval", 60000); // default 60 seconds delay
                Log.i(TAG, String.format("Delaying broadcast for : %d minutes", delay));
                Thread.sleep(delay*60*1000); // minutes
                modemCotUtility.stopListener();
                modemCotUtility.sendCoT(mapView.getSelfMarker());
            }
        } catch(Exception e) {
            Log.i(TAG, "AutoBroadcaster thread error: " + e);
        }
    }
    public void start()  {
        thread.start();
    }

    public void stop() {
        running = false;
        thread.interrupt();
    }

}