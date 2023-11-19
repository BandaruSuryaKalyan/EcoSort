package com.suryakalyan.ecosort;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocationData implements ClusterItem {
    private String name;
    private String dateTime;
    private double latitude;
    private double longitude;
    
    // Default constructor required by Firebase
    public LocationData() {
    }
    
    // Constructor with name, latitude, and longitude parameters
    public LocationData(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        
        // Format the current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US);
        this.dateTime = sdf.format(new Date());
    }
    
    // Getters for Firebase
    public String getName() {
        return name;
    }
    
    public String getDateTime() {
        return dateTime;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    @Override
    public LatLng getPosition() {
        return new LatLng(latitude, longitude);
    }
    
    @Override
    public String getTitle() {
        return name;
    }
    
    @Override
    public String getSnippet() {
        return dateTime;
    }
}