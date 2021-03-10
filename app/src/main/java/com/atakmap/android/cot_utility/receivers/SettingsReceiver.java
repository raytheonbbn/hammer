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
    private Switch slowVoxSwitch;
    private Switch autoBroadcastSwitch;
    private Switch sharedSecretSwitch;
    private Switch readReceiptSwitch;

    private NumberPicker autoBroadcastNP;
    private TextView sharedSecretTV;

    private ModemCotUtility modemCotUtility;
    private Context context;

    private EditText ipEditText, portEditText;

    public SettingsReceiver(MapView mapView, Context context) {
        super(mapView);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        settingsView = inflater.inflate(R.layout.settings, null);
        this.mapView = mapView;
        this.context = context;

        enableReceiveButton = settingsView.findViewById(R.id.enableReceiveCoTFromModem);
        abbreviateCotSwitch = settingsView.findViewById(R.id.abbreviateCot);
        slowVoxSwitch       = settingsView.findViewById(R.id.slowVox);
        autoBroadcastSwitch = settingsView.findViewById(R.id.autoBroadcast);
        autoBroadcastNP     = settingsView.findViewById(R.id.autoBroadcastNP);
        sharedSecretSwitch  = settingsView.findViewById(R.id.sharedSecret);
        sharedSecretTV      = settingsView.findViewById(R.id.sharedSecretText);
        readReceiptSwitch   = settingsView.findViewById(R.id.readReceipt);

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
        if(intent == null) {
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

            if(ModemCotUtility.usePSK){
                sharedSecretSwitch.setChecked(true);
            }else{
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

            readReceiptSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    ModemCotUtility.useReadReceipt = b;

                    SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("useReadReceipt", b);
                    editor.apply();
                }
            });

            if(ModemCotUtility.useReadReceipt){
                readReceiptSwitch.setChecked(true);
            }else{
                readReceiptSwitch.setChecked(false);
            }

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

            if(ModemCotUtility.useAbbreviatedCoT){
                abbreviateCotSwitch.setChecked(true);
            }else{
                abbreviateCotSwitch.setChecked(false);
            }

            enableReceiveButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if(b && !modemCotUtility.isReceiving()){
                        modemCotUtility.startListener();
                    }else if(modemCotUtility.isReceiving()){
                        modemCotUtility.stopListener();
                    }

                    SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("cotUtilEnabled", b);
                    editor.apply();
                }
            });

            if(modemCotUtility.isReceiving()){
                enableReceiveButton.setChecked(true);
            }else{
                enableReceiveButton.setChecked(false);
            }

            slowVoxSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    ModemCotUtility.useSlowVox = b;

                    SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("useSlowVox", b);
                    editor.apply();

                    if (sharedPref.getString("b64_beep_bytes", "").isEmpty()) {
                        Log.i(TAG, "slowVox: loading beep.wav");
                        // TODO: figure out how to get the plugin's context
                        //try {
                        //   byte[] beep = new byte[4096];
                        //   InputStream stream = PluginLifecycle.activity.getAssets().open("beep.wav");
                        //   final int n = stream.read(beep);
                        //   Log.i(TAG, String.format("beep.wav length: %d", n));
                        //   if (n != 3424) { // on disk byte length of assets/beep.wav
                        //       Log.w(TAG, String.format("slowVox: reading assets/beep.wav mismatch length"));
                        //   }
                        //   stream.close();
                        //   editor.putString("b64_beep_bytes", Base64.encodeToString(beep, Base64.DEFAULT));
                        //} catch(IOException e) {
                        //    Log.e(TAG, "failed to open beep.wav: " + e);
                        //}

                        // HACK: I couldn't get the plugin's context to access beep.wav from assets/ so here is beep.wav in a base64 encoded string
                        editor.putString("b64_beep_bytes", "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");
                        editor.apply();
                    }
                }
             });

             if(modemCotUtility.useSlowVox){
                 slowVoxSwitch.setChecked(true);
             }else{
                 slowVoxSwitch.setChecked(false);
                 SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
                 SharedPreferences.Editor editor = sharedPref.edit();
                 editor = sharedPref.edit();
                 editor.remove("b64_beep_bytes");
                 editor.apply();
                 Log.i(TAG, "slowVox: unloading beep.wav");
             }

             final String[] delays = new String[] {"1","5","10","15","30","60","90","120","240","480","960","1440"};
             autoBroadcastNP.setDisplayedValues(null);
             autoBroadcastNP.setMinValue(0);
             autoBroadcastNP.setMaxValue(delays.length-1);
             autoBroadcastNP.setDisplayedValues(delays);
             autoBroadcastNP.setValue(3);

             autoBroadcastSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                 @Override
                 public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                 if(b && !modemCotUtility.isAutoBroadcasting()){
                     modemCotUtility.startABListener();
                     autoBroadcastNP.setEnabled(false);
                 }else if(modemCotUtility.isAutoBroadcasting()){
                     modemCotUtility.stopABListener();
                     autoBroadcastNP.setEnabled(true);
                 }
                 }
             });


             if(modemCotUtility.isAutoBroadcasting()){
                 autoBroadcastSwitch.setChecked(true);
                 autoBroadcastNP.setEnabled(false);
             }else{
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
/*
             autoBroadcastNP.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                 SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
                 SharedPreferences.Editor editor = sharedPref.edit();
                 @Override
                 public void onValueChange(NumberPicker picker, int oldVal, int newVal){
                     Log.d(TAG, String.format("newVal = %s", newVal));
                     Log.d(TAG, String.format("AutoBroadcast Interval: %s", delays[newVal]));
                     editor.putInt("autoBroadcastInterval", Integer.parseInt(delays[newVal])*60000); // convert to minutes for Thread.sleep()
                     editor.apply();
                 }
             });
*/
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
