package com.atakmap.android.cot_utility.receivers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.cot_utility.plugin.PluginLifecycle;
import com.atakmap.android.cot_utility.plugin.R;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.gui.PluginSpinner;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import utils.DropDownManager;
import utils.MapItems;
import utils.ModemCotUtility;

public class SendChatDropDownReceiver extends DropDownReceiver implements ModemCotUtility.ChatMessageListener {
    public static final String TAG = ViewCoTMarkersReceiver.class
            .getSimpleName();

    public static final String SEND_CHAT_RECEIVER = "com.atakmap.android.cot_utility.SEND_CHAT_RECEIVER";

    private View chatView;
    private Intent intent;
    private MapView mapView;
    private Context context;
    private TextView textView;
    private String selectedCallsign = "";

    private ConcurrentHashMap<String, String> otherCallsignToChatHistory;
    private PluginSpinner spinner;
    private ArrayAdapter<String> adp;
    private String myCallsign;
    private boolean hasBeenLoaded = false;
    private String allMessages = "";

    public SendChatDropDownReceiver(MapView mapView, Context context) {
        super(mapView);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        chatView = inflater.inflate(R.layout.send_cot_view, null);

        this.mapView = mapView;
        this.context = context;

        ImageButton backButton = chatView.findViewById(R.id.backButtonChatView);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendChatDropDownReceiver.this.onBackButtonPressed();
            }
        });

        ModemCotUtility.getInstance(mapView, context).registerChatListener(this);

        otherCallsignToChatHistory = new ConcurrentHashMap<>();
        textView = chatView.findViewById(R.id.messageHistoryText);
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

        if (intent.getAction().equals(SEND_CHAT_RECEIVER)) {
            Log.d(TAG, "showing chat receiver");
            this.intent = intent;
            showDropDown(chatView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false);

            final EditText editText = chatView.findViewById(R.id.editText);

            textView.setMovementMethod(new ScrollingMovementMethod());
            myCallsign = MapView.getMapView().getDeviceCallsign();

            Button sendChatButton = chatView.findViewById(R.id.sendChatBtn);
            sendChatButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if(selectedCallsign != null) {
                        ModemCotUtility.getInstance(mapView, context).stopListener();
                        ModemCotUtility.getInstance(mapView, context).sendChat(editText.getText().toString(), selectedCallsign);
                        CoordinatedTime coordinatedTime = new CoordinatedTime(System.currentTimeMillis());

                        String history = addToHistory(selectedCallsign, formatChatMessage(editText.getText().toString(), myCallsign, coordinatedTime, true));
                        textView.setText(history);
                    }else{
                        Toast toast = Toast.makeText(context, "Please select a callsign to send to", Toast.LENGTH_LONG);
                        toast.show();
                    }

                }
            });


            spinner = (PluginSpinner) chatView.findViewById(R.id.callsigns_spinner);
            ArrayList<String> callsigns = new ArrayList<String>();

            // extract any users
            LinkedHashSet<MapItem> cotMapItems = MapItems.getCursorOnTargetMapItems(mapView);
            for(MapItem mapItem : cotMapItems){
                android.util.Log.d(TAG, "onReceive: " + mapItem.getType());
                if(mapItem.getType().contains("-U") || mapItem.getType().startsWith("U-")) {
                    if(!mapItem.getTitle().equals(myCallsign)) {
                        callsigns.add(mapItem.getTitle());
                        SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
                        String chatHistoryForCallsign = sharedPref.getString(mapItem.getTitle(), null);
                        if (chatHistoryForCallsign != null) {
                            android.util.Log.d(TAG, "onReceive: " + chatHistoryForCallsign);
                            otherCallsignToChatHistory.put(mapItem.getTitle(), chatHistoryForCallsign);
                        }
                    }
                }
            }

            callsigns.add("ALL");
            final List<String> callsignsFinal = callsigns;

            adp = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, callsigns);
            spinner.setAdapter(adp);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    selectedCallsign = callsignsFinal.get(i);
                    android.util.Log.d(TAG, "onItemSelected: " + selectedCallsign);
                    textView.setText(R.string.chat_instructions);
                    String chatHistory = otherCallsignToChatHistory.get(selectedCallsign);
                    if (chatHistory != null) {
                        textView.setText(chatHistory);
                    }

                    if(selectedCallsign.equals("ALL") && allMessages.length() > 0){
                        textView.setText(allMessages);
                    }

                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            if (selectedCallsign != null) {
                textView.setText("");
                spinner.setSelection(adp.getPosition(selectedCallsign));
            }
        }else{
            Toast toast = Toast.makeText(context, "Please set ip address in settings", Toast.LENGTH_SHORT);
            toast.show();
        }

        hasBeenLoaded = true;


    }

    protected boolean onBackButtonPressed() {
        DropDownManager.getInstance().clearBackStack();
        DropDownManager.getInstance().removeFromBackStack();
        intent.setAction(CoTUtilityDropDownReceiver.SHOW_PLUGIN);
        AtakBroadcast.getInstance().sendBroadcast(intent);
        return true;
    }


    @Override
    public void chatReceived(String message, String callsign, String timeMillis, String callsignDestination) {
        CoordinatedTime coordinatedTime = new CoordinatedTime(Long.parseLong(timeMillis));
        Toast toast = Toast.makeText(context, formatChatMessage(message, callsign, coordinatedTime, false), Toast.LENGTH_SHORT);
        toast.show();

        String chatMessage = formatChatMessage(message, callsign, coordinatedTime, true);
        String history = addToHistory(callsign, chatMessage);

        if(!hasBeenLoaded){
            return;
        }

        myCallsign = MapView.getMapView().getDeviceCallsign();
        if(selectedCallsign.equals("ALL")){
            if(callsignDestination.equals("ALL") || callsignDestination.equals(myCallsign)){
                textView.setText(history);
            }
        }else if(callsign.equals(selectedCallsign)){
            textView.setText(history);
        }
    }

    private String addToHistory(String callsign, String message){
        // persist in memory
        String history = otherCallsignToChatHistory.get(callsign);
        if(history != null){
            history = history + message;
        }else{
            history = message;
        }
        otherCallsignToChatHistory.put(callsign, history);

        allMessages += message;

        if(selectedCallsign.equals("ALL")){
            history = allMessages;
        }

        // persist in storage
        SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(callsign, message);

        if(selectedCallsign.equals("ALL")){
            editor.putString("ALL", history);
        }

        editor.apply();

        return history;
    }

    private String formatChatMessage(String message, String callsign, CoordinatedTime coordinatedTime, boolean addSpaces){
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(coordinatedTime.getMilliseconds()));

        String timeStamp = cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ""
                + " " + cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.YEAR);

        if(!addSpaces){
            return message + "   - " + callsign + " (" + timeStamp + ")";
        }else {
            return message + "\n" + "   - " + callsign + " (" + timeStamp + ")\n\n";
        }
    }
}
