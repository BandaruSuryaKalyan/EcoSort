package com.suryakalyan.ecosort;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.clustering.ClusterManager;

import java.io.IOException;
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
    private boolean isInfoCardVisible = false;
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
        
        // Handle navigation button click
//        navigateButton.setOnClickListener(v -> {
//            // Open Google Maps with the selected location
//            // Replace latitude and longitude with the actual values from the selected marker
//            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + selectedLocation.getLatitude() + "," + selectedLocation.getLongitude());
//            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
//            mapIntent.setPackage("com.google.android.apps.maps");
//            if (mapIntent.resolveActivity(getContext().getPackageManager()) != null) {
//                startActivity(mapIntent);
//            }
//        });
        
        return view;
    }
    
    @Override
    public void onMapReady( GoogleMap googleMap ) {
        mMap = googleMap;
        
        SharedPreferences preference = getActivity().getSharedPreferences( "UserLoginActivity", MODE_PRIVATE );
        String userId = preference.getString( "UserEmailPref", "Unknown Users" );
        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference locationsRef = database.getReference( "Live Locations" ).child( userId );
        
        // Initialize cluster manager
        clusterManager = new ClusterManager<>( this.getContext(), mMap );
        
        /// Set custom renderer for cluster colors
        CustomClusterRenderer customClusterRenderer = new CustomClusterRenderer( getContext(), mMap, clusterManager );
        clusterManager.setRenderer( customClusterRenderer );
        
        // Read locations from Firebase
        locationsRef.addValueEventListener( new ValueEventListener() {
            @Override
            public void onDataChange( DataSnapshot dataSnapshot ) {
                
                // Clear existing clusters and markers
                mMap.clear();
                clusterManager.clearItems();
                
                // Get locations from snapshot
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    
                    LocationData location = snapshot.getValue( LocationData.class );
                    MyMarkerClass marker = new MyMarkerClass( location );
                    clusterManager.addItem( marker );
                }
                // Cluster markers
                clusterManager.cluster();
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
                    (float) Math.floor( mMap.getCameraPosition().zoom + 5 ) ) );
            return true;
        } );
        
        // Handle cluster item (marker) click
        clusterManager.setOnClusterItemClickListener( myMarkerClass -> {
            // Get location details from marker
            LocationData location = myMarkerClass.getLocationData();
            
            // Show in card view
            nameTextView.setText( location.getName() );
            timeTextView.setText( location.getDateTime() );
            
            String address = getAddressFromLatLng( location.getLatitude(), location.getLongitude() );
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
                
                // Add relevant address details (customize as needed)
                if (address.getSubThoroughfare() != null) {
                    addressStringBuilder.append( address.getSubThoroughfare() ).append( ", " );
                }
                if (address.getThoroughfare() != null) {
                    addressStringBuilder.append( address.getThoroughfare() ).append( ", " );
                }
                if (address.getLocality() != null) {
                    addressStringBuilder.append( address.getLocality() ).append( ", " );
                }
                if (address.getPostalCode() != null) {
                    addressStringBuilder.append( address.getPostalCode() ).append( ", " );
                }
                // Add other details like sub-locality, postal code, etc.
                
                return addressStringBuilder.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Address not available";
    }
}