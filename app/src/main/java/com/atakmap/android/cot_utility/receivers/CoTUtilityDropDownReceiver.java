
package com.atakmap.android.cot_utility.receivers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.atakmap.android.cot_utility.plugin.PluginTool;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDown.OnStateListener;

import com.atakmap.android.cot_utility.plugin.R;
import com.atakmap.coremap.log.Log;

import utils.ModemCotUtility;

public class CoTUtilityDropDownReceiver extends DropDownReceiver implements
        OnStateListener{

    public static final String TAG = CoTUtilityDropDownReceiver.class
            .getSimpleName();

    public static final String SHOW_PLUGIN = "com.atakmap.android.cot_utility.SHOW_PLUGIN";

    private final View mainView;
    private final Context pluginContext;
    private MapView mapView;
    private ModemCotUtility modemCotUtility;

    private Button viewCoTMarkersButton, sendChatButton;

    /**************************** CONSTRUCTOR *****************************/

    public CoTUtilityDropDownReceiver(final MapView mapView,
                                      final Context context) {
        super(mapView);
        this.pluginContext = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mainView = inflater.inflate(R.layout.main_layout, null);
        this.mapView = mapView;
    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(final Context context, Intent intent) {

        Log.d(TAG, "showing plugin drop down");
        if (intent.getAction().equals(SHOW_PLUGIN)) {
            showDropDown(mainView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false);

            viewCoTMarkersButton = mainView.findViewById(R.id.viewCotItemsBtn);
            viewCoTMarkersButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    android.util.Log.d(TAG, "onClick: ");
                    Intent intent = new Intent();
                    intent.setAction(ViewCoTMarkersReceiver.VIEW_COT_MARKERS_RECEIVER);
                    AtakBroadcast.getInstance().sendBroadcast(intent);
                }
            });

            sendChatButton = mainView.findViewById(R.id.sendChatBtn);
            sendChatButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent();
                    intent.setAction(SendChatDropDownReceiver.SEND_CHAT_RECEIVER);
                    AtakBroadcast.getInstance().sendBroadcast(intent);
                }
            });

            /*
                Start listener
             */
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.RECORD_AUDIO}, 1234);
            }

            modemCotUtility = ModemCotUtility.getInstance(mapView, pluginContext);

            // display connection information
            ImageButton infoButton = mainView.findViewById(R.id.infoButton);
            infoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent();
                    intent.setAction(ReadMeReceiver.SHOW_README);
                    AtakBroadcast.getInstance().sendBroadcast(intent);
                }
            });

            Button settingsButton = mainView.findViewById(R.id.settingsButton);
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent();
                    intent.setAction(SettingsReceiver.SETTINGS_RECEIVER);
                    AtakBroadcast.getInstance().sendBroadcast(intent);
                }
            });
        }
    }


    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

}
