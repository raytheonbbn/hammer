package com.atakmap.android.cot_utility.receivers;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;

import utils.MiscUtils;

public abstract class ViewTableReceiver extends DropDownReceiver implements DropDown.OnStateListener {
    public static final String LIGHTER_GRAY = "#f8f8f8";
    public static final String MED_GRAY = "#888888";
    public static final String MED_DARK_GRAY = "#6f6f6f";
    public static final String DARK_GREEN = "#92a844";
    protected Context pluginContext;
    protected MapView mapView;
    protected int pxLeft;
    protected int pxTop;
    protected int pxRight;
    protected int pxBottom;

    public ViewTableReceiver(MapView mapView, Context context) {
        super(mapView);
        this.pluginContext = context;
        this.mapView = mapView;
        pxLeft = MiscUtils.dpTopx(pluginContext, 10);
        pxTop = MiscUtils.dpTopx(pluginContext, 10);
        pxRight = MiscUtils.dpTopx(pluginContext, 10);
        pxBottom = MiscUtils.dpTopx(pluginContext, 10);
    }

    @Override
    public void onDropDownSelectionRemoved() {

    }

    @Override
    public void onDropDownClose() {

    }

    @Override
    public void onDropDownSizeChanged(double v, double v1) {

    }

    @Override
    public void onDropDownVisible(boolean b) {

    }

    @Override
    protected void disposeImpl() {

    }

    protected GridLayout.LayoutParams newLayoutParams() {
        // A combination of GridLayout.spec and Gravity control the alignment and size of the grid
        // items.
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        GridLayout.LayoutParams gridLayoutParams = new GridLayout.LayoutParams(params);
        gridLayoutParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
        // 'The columns can start anywhere as long as each is positioned at 0/3, 1/3, and 2/3
        // of the horizontal screen real estate.'
        gridLayoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED,1f);
        gridLayoutParams.rightMargin = 5;
        gridLayoutParams.topMargin = 5;
        // If the column spec is set as above, then FILL_HORIZONTAL expands the column's
        // background to take up all of the available space (approx. 1/3 of the screen
        // horizontally).
        gridLayoutParams.setGravity(Gravity.FILL_HORIZONTAL| Gravity.TOP);
        return gridLayoutParams;
    }

    protected TextView createTableHeader(String name) {
        TextView header = new TextView(pluginContext);
        header.setText(name);
        header.setTextColor(Color.parseColor(LIGHTER_GRAY));
        header.setTextSize(20);
        header.setPadding(pxLeft, pxTop, pxRight, pxBottom);
        header.setBackgroundColor(Color.parseColor(MED_GRAY));
        return header;
    }

    protected TextView createTableEntry(String name, String backgroundColor) {
        TextView entry = new TextView(pluginContext);
        entry.setText(name);
        entry.setTextColor(Color.parseColor(LIGHTER_GRAY));
        entry.setTextSize(15);
        entry.setPadding(pxLeft, pxTop, pxRight, pxBottom);
        entry.setBackgroundColor(Color.parseColor(backgroundColor));
        return entry;
    }
}