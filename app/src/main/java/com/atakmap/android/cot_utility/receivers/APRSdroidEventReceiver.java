package com.atakmap.android.cot_utility.receivers;

import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.atakmap.coremap.log.Log;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownReceiver;
import utils.ModemCotUtility;

public class APRSdroidEventReceiver extends DropDownReceiver {

	public static final String TAG = APRSdroidEventReceiver.class.getSimpleName();

	public static final String APRSDROID_RECEIVER = "com.atakmap.android.cot_utility.APRSDROID_RECEIVER";
	//private final View templateView;
	private final Context pluginContext;


	public APRSdroidEventReceiver(final MapView mapView, final Context context) {
		super(mapView);
		this.pluginContext = context;

		//templateView = PluginLayoutInflater.inflate(context, R.layout.main_layout, null);
	}

	public void disposeImpl() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		if (!intent.getAction().startsWith("org.aprsdroid.app"))
			return;

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
}