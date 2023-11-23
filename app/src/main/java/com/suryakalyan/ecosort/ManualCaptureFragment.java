package com.suryakalyan.ecosort;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ManualCaptureFragment extends Fragment {
    
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference database;
    
    AppCompatButton acceptCapture;
    CheckBox captchaCheckbox;
    private boolean isLocationRequestInProgress = false;
    private boolean isGpsEnabled = false;
    String TAG = MainActivity.class.getSimpleName();
    Button btnverifyCaptcha;
    RequestQueue queue;
    private static final String SITE_KEY = "6Lf3XxUpAAAAAKcP5q3RHjZ8_b8AYrZxjxqe7Sch";
    private static final String SECRET_KEY = "6Lf3XxUpAAAAADLWxY85CvuaAj4bq9XuJi5aqy7i";
    
    @Override
    public void onCreate( @Nullable Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient( getActivity() );
        
        database = FirebaseDatabase.getInstance().getReference();
    }
    
    
    @Override
    public View onCreateView( @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.fragment_manual_capture, container, false );
        
        acceptCapture = view.findViewById( R.id.acceptCapture );
        captchaCheckbox = view.findViewById( R.id.recaptchaCheckbox );
        
        acceptCapture.setEnabled( false );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            acceptCapture.setRenderEffect( RenderEffect.createBlurEffect( 10, 10,
                    Shader.TileMode.MIRROR ) );
        }
        acceptCapture.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                LocationManager locationManager =
                        (LocationManager) getActivity().getSystemService( Context.LOCATION_SERVICE );
                isGpsEnabled = locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER );
                
                if (isGpsEnabled && !isLocationRequestInProgress) {
                    isLocationRequestInProgress = true;
                    getUserCurrentLocationOnes();
                }
            }
        } );
        
        captchaCheckbox.setOnCheckedChangeListener( ( buttonView, isChecked ) -> {
            if (isChecked) {
                queue = Volley.newRequestQueue( requireActivity().getApplicationContext() );
                verifyCaptcha();
            }
        } );
        
        return view;
    }
    
    private void getUserCurrentLocationOnes() {
        LocationRequest request = new LocationRequest();
        request.setPriority( LocationRequest.PRIORITY_HIGH_ACCURACY );
        request.setNumUpdates( 2 );
        
        // Request single location update
        if (ActivityCompat.checkSelfPermission( requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission( requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        String uuid = UUID.randomUUID().toString();
        fusedLocationClient.getCurrentLocation( LocationRequest.PRIORITY_HIGH_ACCURACY, null )
                .addOnSuccessListener( location -> {
                    if (location != null) {
                        handleLocationResult( location,uuid );
                    }
                } )
                .addOnFailureListener( e -> {
                    // Handle failure to get location
                    isLocationRequestInProgress = false; // Enable the button even if location request fails
                } );
    }
    
    private void handleLocationResult( android.location.Location location,String uuid) {
        if (location != null) {
            
            SharedPreferences preference = getContext().getSharedPreferences( "UserLoginActivity", MODE_PRIVATE );
            String userId = preference.getString( "UserEmailPref", "Unknown Users" );
            
            String address = getAddressFromLatLng(location.getLatitude(),location.getLongitude());
            
            LocationData locationData = new LocationData(uuid,
                    userId, location.getLatitude(),
                    location.getLongitude(),address
            );
            
            database.child( "Live Locations" ).child( userId ).push().setValue( locationData );
            
            isLocationRequestInProgress = false;
        }
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
    
    private void verifyCaptcha() {
        SafetyNet.getClient( requireContext() ).verifyWithRecaptcha( SITE_KEY )
                .addOnSuccessListener( new OnSuccessListener<SafetyNetApi.RecaptchaTokenResponse>() {
                    @Override
                    public void onSuccess( SafetyNetApi.RecaptchaTokenResponse response ) {
                        if (!response.getTokenResult().isEmpty()) {
                            handleSiteVerify( response.getTokenResult() );
                        }
                    }
                } )
                .addOnFailureListener( new OnFailureListener() {
                    @Override
                    public void onFailure( @NonNull Exception e ) {
                        if (e instanceof ApiException) {
                            ApiException apiException = (ApiException) e;
                            Log.d( TAG, "Error message: " +
                                    CommonStatusCodes.getStatusCodeString( apiException.getStatusCode() ) );
                        } else {
                            Log.d( TAG, "Unknown type of error: " + e.getMessage() );
                        }
                    }
                } );
    }
    
    protected void handleSiteVerify( final String responseToken ) {
        
        String url = "https://www.google.com/recaptcha/api/siteverify";
        StringRequest request = new StringRequest( Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse( String response ) {
                        try {
                            JSONObject jsonObject = new JSONObject( response );
                            if (jsonObject.getBoolean( "success" )) {
                                SharedPreferences preference = getContext().getSharedPreferences( "UserLoginActivity", MODE_PRIVATE );
                                String userId = preference.getString( "UserEmailPref", "Unknown Users" );
                                
                                captchaCheckbox.setEnabled( false );
                                Toast.makeText( getContext(), "verified " + userId,
                                        Toast.LENGTH_SHORT ).show();
                                acceptCapture.setEnabled( true );
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    acceptCapture.setRenderEffect( null );
                                }
                                
                            } else {
                                Toast.makeText( requireActivity().getApplicationContext(),
                                        String.valueOf( jsonObject.getString( "error-codes" ) ), Toast.LENGTH_LONG ).show();
                            }
                        } catch (Exception ex) {
                            Log.d( TAG, "JSON exception: " + ex.getMessage() );
                            
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse( VolleyError error ) {
                        Log.d( TAG, "Error message: " + error.getMessage() );
                    }
                } ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put( "secret", SECRET_KEY );
                params.put( "response", responseToken );
                return params;
            }
        };
        request.setRetryPolicy( new DefaultRetryPolicy(
                50000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT ) );
        queue.add( request );
    }
}