
package com.atakmap.android.cot_utility.receivers;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.atakmap.android.cot_utility.plugin.PluginTool;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDown.OnStateListener;

import com.atakmap.android.cot_utility.plugin.R;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.Collection;

import audiomodem.Sender;
import utils.CotUtil;
import utils.MapItems;
import utils.ModemCotUtility;

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
public class CoTUtilityDropDownReceiver extends ViewTableReceiver implements
        OnStateListener, CotUtil.CotEventListener{

    public static final String TAG = CoTUtilityDropDownReceiver.class
            .getSimpleName();

    public static final String SHOW_PLUGIN = "com.atakmap.android.cot_utility.SHOW_PLUGIN";

    private final View cotView;
    private final Context pluginContext;

    private GridLayout table;
    private MapView mapView;
    private Sender modemSender;

    private Switch enableReceiveButton;
    private ModemCotUtility modemCotUtility;

    /**************************** CONSTRUCTOR *****************************/

    public CoTUtilityDropDownReceiver(final MapView mapView,
                                      final Context context) {
        super(mapView, context);
        this.pluginContext = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        cotView = inflater.inflate(R.layout.main_layout, null);
        this.mapView = mapView;
        enableReceiveButton = cotView.findViewById(R.id.enableReceiveCoTFromModem);

    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "showing plugin drop down");
        if (intent.getAction().equals(SHOW_PLUGIN)) {

            showDropDown(cotView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false);


            MapGroup cotMapGroup = mapView.getRootGroup().findMapGroup("Cursor on Target");
            Collection<MapItem> cotMapItems = MapItems.getMapItemsInGroup(cotMapGroup, new ArrayList<MapItem>());
            updateList(cotMapItems);

            CotUtil.setCotEventListener(this);

            /*
                Start listener
             */
            if (ContextCompat.checkSelfPermission(PluginTool.activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(PluginTool.activity, new String[]{Manifest.permission.RECORD_AUDIO}, 1234);
            }

            modemCotUtility = ModemCotUtility.getInstance(mapView, pluginContext);

            enableReceiveButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if(b && !modemCotUtility.isReceiving()){
                        modemCotUtility.startListener();
                    }else if(modemCotUtility.isReceiving()){
                        modemCotUtility.stopListener();
                    }

                    SharedPreferences sharedPref = CoTUtilityDropDownReceiver.this.pluginContext.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("cotUtilEnabled", b);
                    editor.apply();
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

    public void updateList(final Collection<MapItem> list){
        GridLayout.LayoutParams nameParams = newLayoutParams();
        GridLayout.LayoutParams typeParams = newLayoutParams();

        TextView nameColumnHeader = createTableHeader("Name");
        TextView typeColumnHeader = createTableHeader("Type");

        table = (GridLayout) cotView.findViewById(R.id.table);
        table.setColumnCount(2);

        // remove any residuals from the last time we displayed the table
        table.removeAllViews();
        table.addView(nameColumnHeader, nameParams);
        table.addView(typeColumnHeader, typeParams);

        int i = 0;
        for(final MapItem mapItem : list) {
            String backgroundColor = (i % 2 != 0) ? MED_DARK_GRAY : MED_GRAY;
            nameParams = newLayoutParams();
            typeParams = newLayoutParams();

            TextView mapItemName = createTableEntry(mapItem.getTitle(), backgroundColor);

            TextView mapItemInfo;
            mapItemInfo = createTableEntry(mapItem.getType(), backgroundColor);

            table.addView(mapItemName, nameParams);
            table.addView(mapItemInfo, typeParams);

            mapItemName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ModemCotUtility.getInstance(mapView, pluginContext).stopListener();
                    ModemCotUtility.getInstance(mapView, pluginContext).sendCoT(mapItem);
                }
            });

            i++;
        }
    }

    @Override
    public void onReceiveCotEvent(CotEvent cotEvent) {

    }

}
