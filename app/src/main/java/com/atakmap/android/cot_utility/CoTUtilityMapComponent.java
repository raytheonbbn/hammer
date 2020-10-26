
package com.atakmap.android.cot_utility;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.atakmap.android.cot_utility.plugin.PluginTool;

import utils.CotUtil;
import utils.ModemCotUtility;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;

import com.atakmap.android.ipc.DocumentedExtra;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownMapComponent;

import com.atakmap.android.cot_utility.receivers.CoTUtilityDropDownReceiver;
import com.atakmap.android.cot_utility.plugin.R;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.comms.CotServiceRemote;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;

/*
    Copyright 2020 Raytheon BBN Technologies

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
public class CoTUtilityMapComponent extends DropDownMapComponent implements CotUtil.CotEventListener, MapEventDispatcher.MapEventDispatchListener, CotServiceRemote.CotEventListener {

    public static final String TAG = "PluginMain";

    public Context pluginContext;

    private CoTUtilityDropDownReceiver ddr;

    public void onCreate(final Context context, Intent intent,
            final MapView view) {

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
        view.getMapEventDispatcher().addMapEventListener(MapEvent.ITEM_ADDED,this);

        //mil.arl.atak.CONTACT_LIST

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

        SharedPreferences sharedPref = this.pluginContext.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
        boolean enabled = sharedPref.getBoolean("cotUtilEnabled", true);

        if(enabled) {
            modemCotUtility.startListener();
        }
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
    public void onMapEvent(MapEvent mapEvent) {
        MapItem mapItem = mapEvent.getItem();
        if(mapItem.getTitle() != null){
            android.util.Log.d(TAG, "onReceiveMapEvent: " + mapEvent.getType());
            android.util.Log.d(TAG, "onReceiveMapEvent: " + mapEvent.getItem().toString());

        }

    }

    @Override
    public void onCotEvent(CotEvent cotEvent, Bundle bundle) {
        android.util.Log.d(TAG, "onReceiveMapEvent: " + cotEvent.toString());
    }
}
