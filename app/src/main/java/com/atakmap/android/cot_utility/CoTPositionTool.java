package com.atakmap.android.cot_utility;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;

/*
    Copyright 2020 Raytheon BBN Technologies

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
public class CoTPositionTool {

    /*
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <event version="2.0" uid="TEST-98:0C:82:FB:A2:FF" type="a-f-G-U-C" time="2012-10-29T20:07:45.314Z" start="2012-10-29T20:07:45.314Z" stale="2012-10-29T20:08:00.314Z" how="m-g">
        <point lat="42.3892892899364" lon="-71.1475206" hae="46.0999755859375" ce="9999999" le="9999999"/>
        <detail>
            <contact callsign="George" endpoint="192.168.1.141:4242:tcp"/>
            <__group name="Cyan"/>
        </detail>
    </event>
     */

    public static CotEvent createCoTEvent(MapItem mapItem){
        CotEvent cotEvent = new CotEvent();
        cotEvent.setUID(mapItem.getUID());
        cotEvent.setType(mapItem.getType());

        Long time = mapItem.get("lastUpdateTime");
        CoordinatedTime coordinatedTime = new CoordinatedTime(time);

        cotEvent.setTime(coordinatedTime);
        cotEvent.setStart(coordinatedTime);
        cotEvent.setStale(coordinatedTime);

        GeoPoint geoPoint = ((PointMapItem)mapItem).getPoint();
        CotPoint cotPoint = new CotPoint(geoPoint);

        cotEvent.setPoint(cotPoint);

        return cotEvent;
    }
}
