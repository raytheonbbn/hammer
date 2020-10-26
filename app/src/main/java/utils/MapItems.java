package utils;

import android.util.Log;

import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;

import java.util.Collection;
import java.util.Iterator;

public class MapItems {
    private static final String TAG = MapItems.class.getName();

    // Utility to print all map groups and sub map items
    public static void printMapGroupsMapItems(MapGroup mapGroup, int counter){
        printMapGroupsMapItemsSub(mapGroup,0);
    }
    private static void printMapGroupsMapItemsSub(MapGroup mapGroup, int counter){
        int j = 0;
        String test = "";
        while(j <= counter){
            j++;
            test += "-";
        }

        Log.d(TAG, "printMapGroups: " + test + mapGroup.getFriendlyName());
        Collection<MapGroup> collection = mapGroup.getChildGroups();
        Iterator<MapGroup> iterator = collection.iterator();

        if(mapGroup.getItems().size() > 0){
            Collection<MapItem> items = mapGroup.getItems();
            Iterator<MapItem> itemsIt = items.iterator();
            while(itemsIt.hasNext()){
                Log.d(TAG, "printMapGroups: " + test + " " + itemsIt.next().toString());
            }
        }

        while(iterator.hasNext()){
            printMapGroupsMapItemsSub(iterator.next(), counter + 1);
        }
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

}
