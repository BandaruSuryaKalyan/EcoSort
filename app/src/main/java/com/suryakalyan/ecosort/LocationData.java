package com.suryakalyan.ecosort;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.PrimitiveIterator;

public class LocationData implements ClusterItem {
    private String name;
    private String dateTime;
    private String uuid;
    private double latitude;
    private double longitude;
    private String address;
    
    // Default constructor required by Firebase
    public LocationData() {
    }
    
    // Constructor with name, latitude, and longitude parameters
    public LocationData( String uuid, String name, double latitude, double longitude,String address ) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.uuid = uuid;
        this.address = address;
        
        // Format the current date and time
        SimpleDateFormat sdf = new SimpleDateFormat( "dd-MM-yyyy HH:mm:ss", Locale.US );
        this.dateTime = sdf.format( new Date() );
    }
    
    // Getters for Firebase
    public String getUuid() {
        return uuid;
    }
    
    @Override
    public LatLng getPosition() {
        return new LatLng( latitude, longitude );
    }
    
    @Override
    public String getTitle() {
        return name;
    }
    
    @Override
    public String getSnippet() {
        return dateTime;
    }
    
    public String getAddress() {
        return address;
    }
}