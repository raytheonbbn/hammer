package com.atakmap.android.cot_utility.receivers;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.cot_utility.plugin.R;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import utils.DropDownManager;
import utils.MapItems;
import utils.ModemCotUtility;

public class ViewCoTMarkersReceiver extends ViewTableReceiver implements
        DropDown.OnStateListener {

    public static final String TAG = ViewCoTMarkersReceiver.class
            .getSimpleName();

    public static final String VIEW_COT_MARKERS_RECEIVER = "com.atakmap.android.cot_utility.VIEW_COT_MARKERS_RECEIVER";

    private final View cotView;
    private final Context pluginContext;

    private GridLayout table;
    private MapView mapView;
    private LinkedHashSet<MapItem> cotMapItems;
    private Intent intent;

    public ViewCoTMarkersReceiver(MapView mapView, Context context) {
        super(mapView, context);
        this.pluginContext = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        cotView = inflater.inflate(R.layout.map_cot_items, null);
        this.mapView = mapView;
        table = (GridLayout) cotView.findViewById(R.id.table);
        cotMapItems = new LinkedHashSet<>();
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        if(intent == null) {
            android.util.Log.w(TAG, "Doing nothing, because intent was null");
            return;
        }

        if (intent.getAction() == null) {
            android.util.Log.w(TAG, "Doing nothing, because intent action was null");
            return;
        }

        if (intent.getAction().equals(VIEW_COT_MARKERS_RECEIVER)) {
            Log.d(TAG, "showing view cot markers receiver");
            this.intent = intent;

            showDropDown(cotView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false);

            ImageButton backButton = cotView.findViewById(R.id.backButtonMapItemsView);
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewCoTMarkersReceiver.this.onBackButtonPressed();
                }
            });

            Button selfLocationButton = cotView.findViewById(R.id.selfLocationBtn);
            selfLocationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    android.util.Log.d(TAG, "sending self position");
                    ModemCotUtility.getInstance(mapView, pluginContext).stopListener();
                    ModemCotUtility.getInstance(mapView, pluginContext).sendCoT(mapView.getSelfMarker());
                    Toast toast = Toast.makeText(context, "sending self marker", Toast.LENGTH_SHORT);
                    toast.show();
                }
            });

            cotMapItems = MapItems.getCursorOnTargetMapItems(mapView);

            updateList(cotMapItems);
        }
    }

    public void updateList(final Collection<MapItem> list) {
        android.util.Log.d(TAG, "updateList: " + list.toString());
        GridLayout.LayoutParams nameParams = newLayoutParams();
        GridLayout.LayoutParams typeParams = newLayoutParams();

        TextView nameColumnHeader = createTableHeader("Name");
        TextView typeColumnHeader = createTableHeader("Type");

        table.setColumnCount(2);

        // remove any residuals from the last time we displayed the table
        table.removeAllViews();
        table.addView(nameColumnHeader, nameParams);
        table.addView(typeColumnHeader, typeParams);

        int i = 0;
        for (final MapItem mapItem : list) {
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

    protected boolean onBackButtonPressed() {
        DropDownManager.getInstance().clearBackStack();
        DropDownManager.getInstance().removeFromBackStack();
        intent.setAction(CoTUtilityDropDownReceiver.SHOW_PLUGIN);
        AtakBroadcast.getInstance().sendBroadcast(intent);
        return true;
    }
}
