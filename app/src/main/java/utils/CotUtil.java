package utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.importexport.AbstractCotEventMarshal;
import com.atakmap.android.importexport.MarshalManager;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.statesaver.StateSaverPublisher;
import com.atakmap.coremap.cot.event.CotEvent;

public class CotUtil {
    private static final String TAG = CotUtil.class.getSimpleName();
    private static AbstractCotEventMarshal acem;

    public static void sendCotMessage(CotEvent cotEvent){
        CotMapComponent.getExternalDispatcher().dispatch(cotEvent);
    }

    public interface CotEventListener{
        void onReceiveCotEvent(CotEvent cotEvent);
    }

    public static void setCotEventListener(final CotEventListener cotEventListener){
        AtakBroadcast.DocumentedIntentFilter intentFilter = new AtakBroadcast.DocumentedIntentFilter(
                "com.atakmap.android.statesaver.statesaver_complete_load");

        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "received action: " + intent.getAction());
                initialize(cotEventListener, null);
            }
        };

        if (StateSaverPublisher.isFinished()) {
            initialize(cotEventListener, null);
        } else {
            AtakBroadcast.getInstance().registerReceiver(br, intentFilter);
        }
    }

    public static void setCotEventListener(final CotEventListener cotEventListener, final String cotEventType){
        AtakBroadcast.DocumentedIntentFilter intentFilter = new AtakBroadcast.DocumentedIntentFilter(
                "com.atakmap.android.statesaver.statesaver_complete_load");

        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "received action: " + intent.getAction());
                initialize(cotEventListener, cotEventType);
            }
        };

        if (StateSaverPublisher.isFinished()) {
            initialize(cotEventListener, null);
        } else {
            AtakBroadcast.getInstance().registerReceiver(br, intentFilter);
        }
    }

    private static void initialize(final CotEventListener cotEventListener, final String cotEventType){
        MarshalManager.registerMarshal(
                acem = new AbstractCotEventMarshal("CasPopup") {
                    @Override
                    protected boolean accept(final CotEvent event) {
                        if(cotEventType != null){
                            if (event.getType().equals(cotEventType)){
                                Log.d(TAG, "Accept: " + event.toString());
                                cotEventListener.onReceiveCotEvent(event);
                            }
                        }else {
                            Log.d(TAG, "Accept: " + event.toString());
                            cotEventListener.onReceiveCotEvent(event);
                        }
                        return false;
                    }

                    @Override
                    public int getPriorityLevel() {
                        return 2;
                    }
                });
    }
}
