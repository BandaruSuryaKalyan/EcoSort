package com.suryakalyan.ecosort;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ManualCaptureFragment extends Fragment {
    
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference database;
    
    AppCompatButton acceptCapture;
    
    private boolean isLocationRequestInProgress = false;
    private boolean isGpsEnabled = false;
    
    @Override
    public void onCreate( @Nullable Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient( requireActivity() );
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult( LocationResult locationResult ) {
                if (locationResult != null) {
                    handleLocationResult( locationResult.getLastLocation() );
                }
            }
        };
        
        database = FirebaseDatabase.getInstance().getReference();
    }
    
    @Override
    public View onCreateView( @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.fragment_manual_capture, container, false );
        
        acceptCapture = view.findViewById( R.id.acceptCapture );
        acceptCapture.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                LocationManager locationManager =
                        (LocationManager) getActivity().getSystemService( Context.LOCATION_SERVICE );
                isGpsEnabled = locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER );
                
                if (isGpsEnabled && !isLocationRequestInProgress) {
                    isLocationRequestInProgress = true;
                    startLocationUpdates();
                }
            }
        } );
        return view;
    }
    
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission( requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission( requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates( createLocationRequest(), locationCallback, null );
    }
    
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates( locationCallback );
    }
    
    private void handleLocationResult( android.location.Location location ) {
        if (location != null) {
            
            SharedPreferences preference = getActivity().getSharedPreferences( "UserLoginActivity", MODE_PRIVATE );
            String userId = preference.getString( "UserEmailPref", "Unknown Users" );
            
            LocationData locationData = new LocationData(
                    userId, location.getLatitude(),
                    location.getLongitude()
            );
            
            database.child( "Live Locations" ).child( userId ).push().setValue( locationData );
            
            stopLocationUpdates();
            isLocationRequestInProgress = false;
        }
    }
    
    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority( LocationRequest.PRIORITY_HIGH_ACCURACY );
        return locationRequest;
    }
}