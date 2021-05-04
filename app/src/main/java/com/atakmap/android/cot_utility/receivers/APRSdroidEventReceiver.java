package com.atakmap.android.cot_utility.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Base64;

import com.atakmap.android.cot_utility.plugin.PluginLifecycle;
import com.atakmap.coremap.log.Log;
import com.atakmap.android.maps.MapView;

import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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
			case "POSITION":
				try {
					String packet = intent.getStringExtra("packet");
					// HACK: don't process our own messages
					if (packet.contains(MapView.getMapView().getDeviceCallsign()))
						break;
					String callsign = intent.getStringExtra("callsign");
					Location location = intent.getParcelableExtra("location");
					Log.i(TAG, String.format("Position callsign: %s\nLocation: %s\nPacket: %s", callsign, location, packet));
					if (ModemCotUtility.getInstance(MapView.getMapView(), context).usePSK) {
						Log.i(TAG, "PSK enabled");
						byte[] PSKhash;
						SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
						String psk = sharedPref.getString("PSKText", "atakatak");
						try {
							MessageDigest digest = MessageDigest.getInstance("MD5");
							PSKhash = digest.digest(psk.getBytes("UTF-8"));
						} catch (Exception e) {
							Log.d(TAG, "Decrypt PSK Hashing problem: " + e);
							return;
						}
						try {
							// strip off the cruft that made APRSDroid happy
							packet = packet.substring(packet.indexOf("!")+1);
							byte[] cipherText = Base64.decode(packet, Base64.NO_WRAP);
							Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
							SecretKeySpec key = new SecretKeySpec(PSKhash, "AES");
							// first 16 bytes are IV
							byte[] iv = Arrays.copyOf(cipherText,16);
							cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
							// don't decrypt the IV
							packet = new String(cipher.doFinal(Arrays.copyOfRange(cipherText, 16, cipherText.length)), "UTF-8");
							ModemCotUtility.getInstance(MapView.getMapView(), context).parseCoT(packet);
							break;
						} catch (Exception e) {
							Log.d(TAG, "Decrypt PSK problem: " + e);
							e.printStackTrace();
							return;
						}
					}

					// strip off the cruft that made APRSDroid happy
					packet = packet.substring(packet.indexOf("!")+1);
					Log.i(TAG, String.format("split packet %s", packet));
					// B64 decode it and pass it on
					packet = new String(Base64.decode(packet, Base64.NO_WRAP), "UTF-8");
					Log.i(TAG, String.format("decoded packet %s", packet));
					ModemCotUtility.getInstance(MapView.getMapView(), context).parseCoT(packet);
				} catch (Exception e) {
				}
				break;
		}

	}
}
