
package com.atakmap.android.cot_utility;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.cot_utility.plugin.PluginLifecycle;
import com.atakmap.android.cot_utility.plugin.PluginTool;

import utils.CotUtil;
import utils.MapItems;
import utils.ModemCotUtility;

import com.atakmap.android.cot_utility.receivers.ReadMeReceiver;
import com.atakmap.android.cot_utility.receivers.SendChatDropDownReceiver;
import com.atakmap.android.cot_utility.receivers.SettingsReceiver;
import com.atakmap.android.cot_utility.receivers.ViewCoTMarkersReceiver;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;

import com.atakmap.android.ipc.DocumentedExtra;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownMapComponent;

import com.atakmap.android.cot_utility.receivers.CoTUtilityDropDownReceiver;
import com.atakmap.android.cot_utility.plugin.R;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.comms.CotServiceRemote;
import com.atakmap.comms.app.CotPortListActivity;
import com.atakmap.comms.app.TLSUtils;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

public class CoTUtilityMapComponent extends DropDownMapComponent implements CotUtil.CotEventListener,  CotServiceRemote.CotEventListener, MapEventDispatcher.MapEventDispatchListener {

    public static final String TAG = "PluginMain";

    public Context pluginContext;

    private CoTUtilityDropDownReceiver ddr;
    private MapView mapView;

    public void onCreate(final Context context, Intent intent,
            final MapView view) {

        this.mapView = view;
        view.getMapEventDispatcher().addMapEventListener(MapEvent.ITEM_ADDED,this);

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        pluginContext = context;

        ddr = new CoTUtilityDropDownReceiver(
                view, context);

        Log.d(TAG, "registering the plugin filter");
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(CoTUtilityDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(ddr, ddFilter);

        CotUtil.setCotEventListener(this);

        CommsMapComponent.getInstance().addOnCotEventListener(this);


        ModemCotUtility modemCotUtility = ModemCotUtility.getInstance(view, context);

        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction("com.atakmap.android.cot_utility.receivers.cotMenu",
                "this intent launches the cot send utility",
                new DocumentedExtra[] {
                        new DocumentedExtra("targetUID",
                                "the map item identifier used to populate the drop down")
                });
        registerDropDownReceiver(modemCotUtility,
                filter);

        SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
        boolean enabled = sharedPref.getBoolean("cotUtilEnabled", true);
        boolean useAbbreviated = sharedPref.getBoolean("useAbbreviated", true);

        if(enabled) {
            modemCotUtility.startListener();
        }

        ModemCotUtility.useAbbreviatedCoT = useAbbreviated;


        ReadMeReceiver readMeReceiver = new ReadMeReceiver(view, context);
        registerReceiverUsingPluginContext(pluginContext, "readme receiver", readMeReceiver, ReadMeReceiver.SHOW_README);

        SendChatDropDownReceiver sendChatDropDownReceiver = new SendChatDropDownReceiver(view, context);
        registerReceiverUsingPluginContext(pluginContext, "sendchat receiver", sendChatDropDownReceiver, SendChatDropDownReceiver.SEND_CHAT_RECEIVER);

        ViewCoTMarkersReceiver viewCoTMarkersReceiver = new ViewCoTMarkersReceiver(view, context);
        registerReceiverUsingPluginContext(pluginContext, "view markers receiver", viewCoTMarkersReceiver, ViewCoTMarkersReceiver.VIEW_COT_MARKERS_RECEIVER);

        SettingsReceiver settingsReceiver = new SettingsReceiver(view, context);
        registerReceiverUsingPluginContext(pluginContext, "settings receiver", settingsReceiver, SettingsReceiver.SETTINGS_RECEIVER);

    }


    private void registerReceiverUsingPluginContext(Context pluginContext, String name, DropDownReceiver rec, String actionName) {
        android.util.Log.d(TAG, "Registering " + name + " receiver with intent filter");
        AtakBroadcast.DocumentedIntentFilter mainIntentFilter = new AtakBroadcast.DocumentedIntentFilter();
        mainIntentFilter.addAction(actionName);
        this.registerReceiver(pluginContext, rec, mainIntentFilter);
    }

    private void registerReceiverUsingAtakContext(String name, DropDownReceiver rec, String actionName) {
        android.util.Log.d(TAG, "Registering " + name + " receiver with intent filter");
        AtakBroadcast.DocumentedIntentFilter mainIntentFilter = new AtakBroadcast.DocumentedIntentFilter();
        mainIntentFilter.addAction(actionName);
        AtakBroadcast.getInstance().registerReceiver(rec, mainIntentFilter);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
    }


    @Override
    public void onReceiveCotEvent(CotEvent cotEvent) {
        android.util.Log.d(TAG, "onReceiveCotEvent: " + cotEvent);
    }

    @Override
    public void onCotEvent(CotEvent cotEvent, Bundle bundle) {
        android.util.Log.d(TAG, "onReceiveMapEvent: " + cotEvent.toString());
    }

    @Override
    public void onMapEvent(MapEvent mapEvent) {
        android.util.Log.d(TAG, "onReceiveMapEvent: " + mapEvent.getType());
    }
}
