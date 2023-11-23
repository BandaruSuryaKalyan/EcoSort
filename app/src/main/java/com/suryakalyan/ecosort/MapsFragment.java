package com.suryakalyan.ecosort;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsFragment extends Fragment implements OnMapReadyCallback {
    
    private GoogleMap mMap;
    private CardView infoCard;
    private TextView nameTextView;
    private TextView timeTextView;
    private TextView addressTextView;
    private AppCompatButton navigateButton;
    private ConstraintLayout map;
    
    private ClusterManager<MyMarkerClass> clusterManager;
    private MyMarkerClass myMarkerClass;
    private boolean isInfoCardVisible = true;
    private static final int MIN_CLUSTER_SIZE = 1;
    
    @Nullable
    @Override
    public View onCreateView( @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.fragment_maps, container, false );
        
        infoCard = view.findViewById( R.id.card_view_maps );
        nameTextView = view.findViewById( R.id.nameTextView );
        timeTextView = view.findViewById( R.id.timeTextView );
        addressTextView = view.findViewById( R.id.addressTextView );
        navigateButton = view.findViewById( R.id.mapNavigateButton );
        
        
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById( R.id.map );
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace( R.id.map, mapFragment ).commit();
        }
        mapFragment.getMapAsync( this );
        
        infoCard.setVisibility( View.GONE );
        
        return view;
    }
    
    @Override
    public void onMapReady( GoogleMap googleMap ) {
        mMap = googleMap;
        
        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference locationsRef = database.getReference( "Live Locations" );
        
        // Initialize cluster manager
        clusterManager = new ClusterManager<>( this.getContext(), mMap );
        
        /// Set custom renderer for cluster colors
        CustomClusterRenderer customClusterRenderer = new CustomClusterRenderer( getContext(), mMap, clusterManager );
        customClusterRenderer.setMinClusterSize( MIN_CLUSTER_SIZE );
        clusterManager.setRenderer( customClusterRenderer );
        
        // Read locations from Firebase
        locationsRef.addValueEventListener( new ValueEventListener() {
            @Override
            public void onDataChange( DataSnapshot dataSnapshot ) {
                
                // Clear existing clusters and markers
                mMap.clear();
                clusterManager.clearItems();
                
                // Get locations from snapshot
                for (DataSnapshot userSnapshots : dataSnapshot.getChildren()) {
                    for (DataSnapshot snapshot : userSnapshots.getChildren()) {
                        LocationData location = snapshot.getValue( LocationData.class );
                        MyMarkerClass marker = new MyMarkerClass( location );
                        clusterManager.addItem( marker );
                        
                        // Add markers directly to the map
//                        mMap.addMarker(new MarkerOptions()
//                                .position(new LatLng(location.getLatitude(), location.getLongitude()))
//                                .title(location.getTitle())
//                                .snippet(location.getSnippet()));
                    }
                }
                // Cluster markers
                clusterManager.cluster();
                updateHeatmap();
            }
            
            @Override
            public void onCancelled( DatabaseError error ) {
                Log.w( "Cluster Marker Addition", "Failed to load locations", error.toException() );
            }
        } );
        
        
        // Handle cluster click
        clusterManager.setOnClusterClickListener( cluster -> {
            // Zoom in on cluster
            mMap.animateCamera( CameraUpdateFactory.newLatLngZoom(
                    cluster.getPosition(),
                    (float) Math.floor( mMap.getCameraPosition().zoom + 10 ) ) );
            return true;
        } );
        
        // Handle cluster item (marker) click
        clusterManager.setOnClusterItemClickListener( myMarkerClass -> {
            // Get location details from marker
            LocationData location = myMarkerClass.getLocationData();
            
            // Show in card view
            nameTextView.setText( location.getTitle() );
            timeTextView.setText( location.getSnippet() );
            
            String address = getAddressFromLatLng( location.getPosition().latitude,
                    location.getPosition().longitude );
            addressTextView.setText( address );
            
            mMap.setOnMapClickListener( new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick( @NonNull LatLng latLng ) {
                    if (isInfoCardVisible) {
                        infoCard.setVisibility( View.GONE );
                    } else {
                        infoCard.setVisibility( View.VISIBLE );
                    }
                    
                    isInfoCardVisible = !isInfoCardVisible;
                }
            } );
            
            return false;
        } );
    }
    
    private String getAddressFromLatLng( double latitude, double longitude ) {
        Geocoder geocoder = new Geocoder( getContext(), Locale.getDefault() );
        try {
            List<Address> addresses = geocoder.getFromLocation( latitude, longitude, 1 );
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get( 0 );
                StringBuilder addressStringBuilder = new StringBuilder();
                
                // Recipient Name
                if (address.getSubThoroughfare() != null) {
                    addressStringBuilder.append( "Recipient Name: " ).append( address.getSubThoroughfare() ).append( "\n" );
                }
                
                // Building or Apartment Number
                if (address.getPremises() != null) {
                    addressStringBuilder.append( "Building or Apartment Number: " ).append( address.getPremises() ).append( "\n" );
                }
                
                // Street Name
                if (address.getThoroughfare() != null) {
                    addressStringBuilder.append( "Street Name: " ).append( address.getThoroughfare() ).append( "\n" );
                }
                
                // City
                if (address.getLocality() != null) {
                    addressStringBuilder.append( "City: " ).append( address.getLocality() ).append( "\n" );
                }
                
                // State or Province
                if (address.getAdminArea() != null) {
                    addressStringBuilder.append( "State or Province: " ).append( address.getAdminArea() ).append( "\n" );
                }
                
                // Postal Code
                if (address.getPostalCode() != null) {
                    addressStringBuilder.append( "Postal Code: " ).append( address.getPostalCode() ).append( "\n" );
                }
                
                // Country
                if (address.getCountryName() != null) {
                    addressStringBuilder.append( "Country: " ).append( address.getCountryName() ).append( "\n" );
                }
                
                return addressStringBuilder.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Address not available";
    }
    
    
    private void updateHeatmap() {
        // Generate WeightedLatLng list based on your marker data
        List<WeightedLatLng> weightedLatLngList = new ArrayList<>();
        
        for (MyMarkerClass marker : clusterManager.getAlgorithm().getItems()) {
            LatLng markerPosition = marker.getPosition();
            if (markerPosition != null) {
                weightedLatLngList.add( new WeightedLatLng( markerPosition, 1 ) );
            }
        }
        
        // Check if the list is not empty before creating the HeatmapTileProvider
        if (!weightedLatLngList.isEmpty()) {
            HeatmapTileProvider heatmapTileProvider = new HeatmapTileProvider.Builder()
                    .weightedData( weightedLatLngList )
                    .radius( 50 ) // Adjust the radius as needed
                    .gradient( heatmapGradient() )
                    .build();
            // Clear previous heatmap overlay
            mMap.clear();
            // Add the HeatmapTileProvider to the map
            mMap.addTileOverlay( new TileOverlayOptions().tileProvider( heatmapTileProvider ) );
            
            // Cluster markers
            clusterManager.cluster();
        }
    }
    
    private Gradient heatmapGradient() {
        // Define your heatmap gradient colors
        int[] colors = {
                Color.rgb( 0, 255, 0 ),  // green
                Color.rgb( 255, 0, 0 )   // red
        };
        // Define your heatmap gradient start points
        float[] startPoints = {
                0.2f,  // green start point
                1.0f   // red end point
        };
        // Ensure colors and startPoints have the same length
        if (colors.length == startPoints.length) {
            return new Gradient( colors, startPoints );
        } else {
            return new Gradient( new int[]{ Color.rgb( 0, 255, 0 ), Color.rgb( 255, 0, 0 ) }, new float[]{ 0.2f, 1.0f } );
        }
    }
    
    
}