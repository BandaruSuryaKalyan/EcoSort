package com.suryakalyan.ecosort;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class MyMarkerClass implements ClusterItem {
    
    private LocationData location;
    
    public MyMarkerClass(LocationData location) {
        this.location = location;
    }
    
    @Override
    public LatLng getPosition() {
        return location.getPosition();
    }
    
    @Override
    public String getTitle() {
        return location.getTitle();
    }
    
    @Override
    public String getSnippet() {
        return location.getSnippet();
    }
    
    public LocationData getLocationData() {
        return location;
    }
}