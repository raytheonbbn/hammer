package audiomodem;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;

import utils.ModemCotUtility;

import com.atakmap.android.cot_utility.plugin.PluginLifecycle;
import com.atakmap.android.maps.MapView;

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

        int delay;
        SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);

        try {
            while(running) {
                modemCotUtility.stopListener();
                modemCotUtility.sendCoT(mapView.getSelfMarker());
                delay = Integer.parseInt(sharedPref.getString("autoBroadcastInterval", "60000")); // default 60 seconds delay
                Thread.sleep(delay);
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
