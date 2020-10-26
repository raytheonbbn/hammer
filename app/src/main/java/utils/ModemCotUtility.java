package utils;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.atakmap.android.cot_utility.CoTPositionTool;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.menu.MenuMapComponent;
import com.atakmap.android.menu.PluginMenuParser;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import audiomodem.Receiver;
import audiomodem.Result;
import audiomodem.Sender;


public class ModemCotUtility extends DropDownReceiver implements DropDown.OnStateListener, MapEventDispatcher.MapEventDispatchListener  {
    public static final String TAG = ModemCotUtility.class
            .getSimpleName();
    public static Receiver rx = null;
    private AtomicBoolean receiveCot;

    private static ModemCotUtility instance = null;
    private MapView mapView;
    private Context context;
    private boolean isReceiving = true;

    // Specify padding to prepend CoT messages with
    private final String padding = "000000000000000000000000000000000000000000000000000000000000000";

    /**
     * CotUtility utility for sending and receiving cursor on target messages via ax.25
     *
     * @param mapView
     * @param context
     * @return CotUtility instance
     */
    public static ModemCotUtility getInstance(MapView mapView, Context context){
        if(instance == null){
            instance = new ModemCotUtility(mapView, context);
        }
        return instance;
    }

    public static Collection<MapItem> getMapItemsInGroup(MapGroup mapGroup, Collection<MapItem> mapItems){
        Collection<MapGroup> childGroups = mapGroup.getChildGroups();
        if(childGroups.size() == 0){
            return mapGroup.getItems();
        }else {
            for (MapGroup childGroup : childGroups) {
                mapItems.addAll(getMapItemsInGroup(childGroup, mapItems));
            }
        }

        return mapItems;
    }

    private ModemCotUtility(MapView mapView, Context context) {
        super(mapView);
        this.mapView = mapView;
        this.context = context;


        Collection<MapItem> mapItems = getMapItemsInGroup(getMapView().getRootGroup(), new HashSet<MapItem>());
        for(MapItem mapItem : mapItems){
            mapItem.setMetaString("menu", getMenu());
        }

        getMapView().getMapEventDispatcher().addMapEventListener(MapEvent.ITEM_ADDED, this);
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

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action != null && action
                .equals("com.atakmap.android.cot_utility.receivers.cotMenu")) {
            PointMapItem temp = findTarget(intent.getStringExtra("targetUID"));
            if (temp != null) {
                stopListener();
                sendCoT(temp);
            }
        }
    }

    private PointMapItem findTarget(final String targetUID) {
        PointMapItem pointItem = null;
        if (targetUID != null) {
            MapItem item = getMapView().getMapItem(targetUID);
            if (item instanceof PointMapItem) {
                pointItem = (PointMapItem) item;
            }
        }
        return pointItem;
    }

    /**
     * Start the CoT stream listener
     */
    public void startListener(){
        isReceiving = true;
        android.util.Log.d(TAG, "startCotListener");
        receiveCot = new AtomicBoolean(false);

        rx = new Receiver(receiveCot) {
            @Override
            protected void onPostExecute(Result res) {
                android.util.Log.d(TAG, "onPostExecute: " + res.out);
                receiveCot.set(true);

                if(res.out != null)
                    parseCoT(res.out);

                if (res.err != null) {
                    Toast.makeText(MapView.getMapView().getContext(), "Error: " + res.err,
                            Toast.LENGTH_LONG).show();
                }

                startListener();
            }
        };

        rx.execute();
        android.util.Log.d(TAG, "Receiving...");
    }

    /**
     * Stop CoT stream listener
     */
    public void stopListener(){
        android.util.Log.d(TAG, "stopListener: ");
        isReceiving = false;
        if(rx == null){
            return;
        }
        rx.stop();
        rx.cancel(true);
    }

    /**
     * Query CoT Stream Listener State
     *
     * @return boolean
     */
    public boolean isReceiving(){
        return isReceiving;
    }

    /**
     * Parse message and extract CoT marker
     * @param message
     */
    private void parseCoT(String message){
        boolean foundStart = false;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < message.length(); i++){
            char c = message.charAt(i);

            if(c != '0' && !foundStart){
                foundStart = true;
            }

            if(foundStart){
                stringBuilder.append(c);
            }
        }

        CotEvent cotEvent = null;
        try {
            cotEvent = CotEvent.parse(stringBuilder.toString());
        }catch (Exception e){
            e.printStackTrace();
        }

        if(cotEvent != null){
            android.util.Log.d(TAG, "parseCoT: " + cotEvent.toString());

            GeoPoint geoPoint = cotEvent.getGeoPoint();
            Marker m = new Marker(geoPoint, cotEvent.getUID());
            m.setType(cotEvent.getType());
            m.setMetaString("start", cotEvent.getStart().toString());
            m.setMetaString("how", cotEvent.getHow());
            m.setTitle("CoT Event");
            m.setMetaString("menu", getMenu());

            String type = "Unknown";
            if(cotEvent.getHow().contains("-h-")){
                type = "Hostile";
            }else if(cotEvent.getHow().contains("-f-")){
                type = "Friendly";
            }else if(cotEvent.getHow().contains("-n-")){
                type = "Neutral";
            }

            MapGroup mapGroup = getMapView().getRootGroup()
                    .findMapGroup("Cursor on Target")
                    .findMapGroup(type);
            mapGroup.addItem(m);

            Log.d(TAG, "creating a new unit marker for: " + m.getUID());
        }
    }

    private String getMenu() {
        return PluginMenuParser.getMenu(context, "menu.xml");

    }

    @Override
    public void onMapEvent(MapEvent mapEvent) {
        mapEvent.getItem().setMetaString("menu", getMenu());
    }

    /**
     * Send CoT map marker via software modem
     * @param mapItem
     */
    public void sendCoT(MapItem mapItem){
        CotEvent cotEvent = CoTPositionTool.createCoTEvent(mapItem);
        android.util.Log.d(TAG, "sending COT: " + cotEvent.toString());
        Sender modemSender = new Sender(this);
        modemSender.execute(padding + cotEvent.toString());
    }


}
