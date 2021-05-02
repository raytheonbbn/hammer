package com.atakmap.android.cot_utility.receivers;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;



import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;

import com.atakmap.android.cot_utility.plugin.PluginLifecycle;
import com.atakmap.android.cot_utility.plugin.R;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import utils.DropDownManager;
import utils.ModemCotUtility;

public class SettingsReceiver extends DropDownReceiver {
    public static final String TAG = SettingsReceiver.class
            .getSimpleName();
    public static final String SETTINGS_RECEIVER = "com.atakmap.android.cot_utility.SETTINGS_RECEIVER";

    private View settingsView;
    private MapView mapView;
    private Intent intent;

    private Switch enableReceiveButton;
    private Switch abbreviateCotSwitch;
    private Switch autoBroadcastSwitch;
    private Switch enableTNCSwitch;
    private Switch sharedSecretSwitch;
    private TextView sharedSecretTV;

    private NumberPicker autoBroadcastNP;

    private ModemCotUtility modemCotUtility;
    private Context context;

    private EditText ipEditText, portEditText, frequencyText;

    public SettingsReceiver(MapView mapView, Context context) {
        super(mapView);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        settingsView = inflater.inflate(R.layout.settings, null);
        this.mapView = mapView;
        this.context = context;

        enableReceiveButton = settingsView.findViewById(R.id.enableReceiveCoTFromModem);
        abbreviateCotSwitch = settingsView.findViewById(R.id.abbreviateCot);
        autoBroadcastSwitch = settingsView.findViewById(R.id.autoBroadcast);
        autoBroadcastNP = settingsView.findViewById(R.id.autoBroadcastNP);

        // TNC
        enableTNCSwitch = settingsView.findViewById(R.id.TNC);
        //frequencyText = settingsView.findViewById(R.id.frequency);

        // PSK
        sharedSecretSwitch = settingsView.findViewById(R.id.sharedSecret);
        sharedSecretTV = settingsView.findViewById(R.id.sharedSecretText);

        ImageButton backButton = settingsView.findViewById(R.id.backButtonSettingsView);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsReceiver.this.onBackButtonPressed();
            }
        });
    }

    @Override
    protected void disposeImpl() {

    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent == null) {
            android.util.Log.w(TAG, "Doing nothing, because intent was null");
            return;
        }

        if (intent.getAction() == null) {
            android.util.Log.w(TAG, "Doing nothing, because intent action was null");
            return;
        }

        if (intent.getAction().equals(SETTINGS_RECEIVER)) {
            Log.d(TAG, "showing settings receiver");
            this.intent = intent;

            showDropDown(settingsView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    FULL_HEIGHT, false);

            modemCotUtility = ModemCotUtility.getInstance(mapView, context);

            sharedSecretSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    ModemCotUtility.usePSK = b;

                    SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("usePSK", b);
                    editor.apply();
                }
            });

            if (ModemCotUtility.usePSK) {
                sharedSecretSwitch.setChecked(true);
            } else {
                sharedSecretSwitch.setChecked(false);
            }

            sharedSecretTV.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    Log.d(TAG, String.format("PSK Text: %s", s.toString()));
                    SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("PSKText", s.toString());
                    editor.apply();
                }
            });

            abbreviateCotSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    ModemCotUtility.useAbbreviatedCoT = b;

                    SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("useAbbreviated", b);
                    editor.apply();
                }
            });

            if (ModemCotUtility.useAbbreviatedCoT) {
                abbreviateCotSwitch.setChecked(true);
            } else {
                abbreviateCotSwitch.setChecked(false);
            }

            enableReceiveButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b && !modemCotUtility.isReceiving()) {
                        modemCotUtility.startListener();
                    } else if (modemCotUtility.isReceiving()) {
                        modemCotUtility.stopListener();
                    }

                    SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("cotUtilEnabled", b);
                    editor.apply();
                }
            });

            if (modemCotUtility.isReceiving()) {
                enableReceiveButton.setChecked(true);
            } else {
                enableReceiveButton.setChecked(false);
            }

            enableTNCSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    ModemCotUtility.useTNC = b;

                    SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("useTNC", b);
                    editor.apply();
                }
            });

            if (ModemCotUtility.useTNC) {
                enableTNCSwitch.setChecked(true);

                if (!ModemCotUtility.aprsdroid_running) {
                    // make sure APRSDroid is running
                    Intent i = new Intent("org.aprsdroid.app.SERVICE").setPackage("org.aprsdroid.app");
                    PluginLifecycle.activity.getApplicationContext().startForegroundService(i);
                }
            } else {
                enableTNCSwitch.setChecked(false);
                /*
                if (ModemCotUtility.aprsdroid_running) {
                    // make sure APRSDroid is stopped
                    Intent i = new Intent("org.aprsdroid.app.SERVICE_STOP").setPackage("org.aprsdroid.app");
                    PluginLifecycle.activity.getApplicationContext().startForegroundService(i);
                }
                */
            }
/*
            frequencyText.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (ModemCotUtility.aprsdroid_running) {
                        Log.d(TAG, String.format("APRS Frequency: %s", s.toString()));
                        Intent i = new Intent("org.aprsdroid.app.FREQUENCY").setPackage("org.aprsdroid.app");
                        i.putExtra("frequency", s.toString());
                        PluginLifecycle.activity.getApplicationContext().startForegroundService(i);
                    }
                }
           });
 */
            final String[] delays = new String[]{"1", "5", "10", "15", "30", "60", "90", "120", "240", "480", "960", "1440"};
            autoBroadcastNP.setDisplayedValues(null);
            autoBroadcastNP.setMinValue(0);
            autoBroadcastNP.setMaxValue(delays.length - 1);
            autoBroadcastNP.setDisplayedValues(delays);
            autoBroadcastNP.setValue(3);

            autoBroadcastSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b && !modemCotUtility.isAutoBroadcasting()) {
                        modemCotUtility.startABListener();
                        autoBroadcastNP.setEnabled(false);
                    } else if (modemCotUtility.isAutoBroadcasting()) {
                        modemCotUtility.stopABListener();
                        autoBroadcastNP.setEnabled(true);
                    }
                }
            });

            if (modemCotUtility.isAutoBroadcasting()) {
                autoBroadcastSwitch.setChecked(true);
                autoBroadcastNP.setEnabled(false);
            } else {
                autoBroadcastSwitch.setChecked(false);
                autoBroadcastNP.setEnabled(true);
            }

            autoBroadcastNP.setOnScrollListener(new NumberPicker.OnScrollListener() {
                SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                @Override
                public void onScrollStateChange(NumberPicker picker, int scrollState) {
                    int newVal = picker.getValue();
                    if (scrollState == NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                        Log.d(TAG, "IDLE");
                        newVal = picker.getValue();
                        Log.d(TAG, String.format("AutoBroadcast Interval: %s", delays[newVal]));
                        editor.putInt("autoBroadcastInterval", Integer.parseInt(delays[newVal])); // convert to minutes for Thread.sleep()
                        editor.apply();
                    }
                }
            });
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
