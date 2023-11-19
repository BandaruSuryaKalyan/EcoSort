package com.suryakalyan.ecosort;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.suryakalyan.ecosort.MyMarkerClass;

public class CustomClusterRenderer extends DefaultClusterRenderer<MyMarkerClass> {
    
    public CustomClusterRenderer( Context context, GoogleMap map, ClusterManager<MyMarkerClass> clusterManager) {
        super(context, map, clusterManager);
    }
    
    @Override
    protected void onBeforeClusterRendered( Cluster<MyMarkerClass> cluster, MarkerOptions markerOptions) {
        // Customize the appearance of the cluster
        // For example, change the color based on the number of items in the cluster
        int clusterSize = cluster.getSize();
        if (clusterSize < 5) {
            // Few items in the cluster, use green color
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_GREEN));
        } else {
            // Many items in the cluster, use red color
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        }
        
        // Call the superclass method to apply default rendering
        super.onBeforeClusterRendered(cluster, markerOptions);
    }
    
    // You can override other methods here to customize rendering further
}