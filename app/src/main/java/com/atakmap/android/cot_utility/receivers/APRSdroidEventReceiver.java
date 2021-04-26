package com.atakmap.android.cot_utility.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.atakmap.coremap.log.Log;
import com.atakmap.android.maps.MapView;

import utils.ModemCotUtility;

public class APRSdroidEventReceiver extends BroadcastReceiver {

	public static final String TAG = APRSdroidEventReceiver.class.getSimpleName();

	public APRSdroidEventReceiver() {
		super();
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		if (intent == null) {
			Log.w(TAG, "Doing nothing, because intent was null");
			return;
		}

		if (intent.getAction() == null) {
			Log.w(TAG, "Doing nothing, because intent action was null");
			return;
		}

		String a = intent.getAction().replace("org.aprsdroid.app.", "");

		switch (a) {
			case "SERVICE_STARTED":
				ModemCotUtility.aprsdroid_running = true;
				Log.i(TAG, "APRSdroid is running");
				break;
			case "SERVICE_STOPPED":
				ModemCotUtility.aprsdroid_running = false;
				Log.i(TAG, "APRSdroid is not running");
				break;
			case "MESSAGE":
				try {
					String message = intent.getStringExtra("body");
					// HACK: don't process our own messages
					if (message.contains(MapView.getMapView().getDeviceCallsign()))
						break;
					String source = intent.getStringExtra("source");
					String dest = intent.getStringExtra("dest");
					Log.i(TAG, String.format("Message from: %s\nMessage to: %s\nMessage Body: %s", source, dest, message));
					ModemCotUtility.getInstance(MapView.getMapView(), context).parseCoT(message.split("\\!")[1]);
				} catch (Exception e) {
				}
				break;
			case "POSITION":
				try {
					String packet = intent.getStringExtra("packet");
					// HACK: don't process our own messages
					if (packet.contains(MapView.getMapView().getDeviceCallsign()))
						break;
					String callsign = intent.getStringExtra("callsign");
					Location location = intent.getParcelableExtra("location");
					Log.i(TAG, String.format("Position callsign: %s\nLocation: %s\nPacket: %s", callsign, location, packet));
					ModemCotUtility.getInstance(MapView.getMapView(), context).parseCoT(packet.split("\\!")[1]);
				} catch (Exception e) {
				}
				break;
		}

	}
}