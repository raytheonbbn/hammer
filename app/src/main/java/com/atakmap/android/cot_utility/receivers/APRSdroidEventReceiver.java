package com.atakmap.android.cot_utility.receivers;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.cot_utility.plugin.R;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.coremap.log.Log;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownReceiver;
import utils.ModemCotUtility;

public class APRSdroidEventReceiver extends DropDownReceiver implements DropDown.OnStateListener {

	public static final String TAG = APRSdroidEventReceiver.class.getSimpleName();

	private final View mainView;
	private final Context pluginContext;
	private MapView mapView;


	public APRSdroidEventReceiver(final MapView mapView, final Context context) {
		super(mapView);

		this.pluginContext = context;
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.mainView = inflater.inflate(R.layout.main_layout, null);
		this.mapView = mapView;
	}

	public void disposeImpl() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		if(intent == null) {
			android.util.Log.w(TAG, "Doing nothing, because intent was null");
			return;
		}

		if (intent.getAction() == null) {
			android.util.Log.w(TAG, "Doing nothing, because intent action was null");
			return;
		}

		String a = intent.getAction().replace("org.aprsdroid.app.", "");

		switch (a) {
			case "SERVICE_STARTED":
				ModemCotUtility.aprsdroid_running = true;
				com.atakmap.coremap.log.Log.i(TAG,"APRSdroid is running");
				break;
			case "SERVICE_STOPPED":
				ModemCotUtility.aprsdroid_running = false;
				com.atakmap.coremap.log.Log.i(TAG,"APRSdroid is not running");
				break;
			case "MESSAGE":
				try {
					String source = intent.getStringExtra("source");
					String dest = intent.getStringExtra("dest");
					String message = intent.getStringExtra("body");
					com.atakmap.coremap.log.Log.i(TAG, String.format("Message from: %s\nMessage to: %s\nMessage Body: %s", source, dest, message));
					ModemCotUtility.getInstance(MapView.getMapView(), context).parseCoT(message);
				} catch (Exception e) {}
				break;
			case "POSITION":
				try {
					String callsign = intent.getStringExtra("callsign");
					String packet = intent.getStringExtra("packet");
					Location location = intent.getParcelableExtra("location");
					com.atakmap.coremap.log.Log.i(TAG, String.format("Position callsign: %s\nLocation: %s\nPacket: %s", callsign, location, packet));
					//ModemCotUtility.parseCoT(location);
				} catch (Exception e) {}
				break;
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