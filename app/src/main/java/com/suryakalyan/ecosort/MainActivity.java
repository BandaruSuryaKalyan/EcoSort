package com.suryakalyan.ecosort;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private int REQUEST_CODE = 100;
    private String[] Permissions = { android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA, android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS };
    
    BottomNavigationView bottomNavigationView;
    HomeFragment homeFragment = new HomeFragment();
    DashboardFragment dashboardFragment = new DashboardFragment();
    RewardsFragment rewardsFragment = new RewardsFragment();
    ProfileFragment profileFragment = new ProfileFragment();
    MapsFragment mapsFragment = new MapsFragment();
    ManualCaptureFragment manualCaptureFragment = new ManualCaptureFragment();
    AutomaticModeCapturing automaticModeCapturing= new AutomaticModeCapturing();
    
    
    boolean PermissionsFlag = true;
    
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        
        
        if (!PermissionsFlag) {
            requestRequiredPermissions();
        }
        
        
        bottomNavigationView = findViewById( R.id.bottom_nav_view );
        getSupportFragmentManager().beginTransaction().replace( R.id.nav_host_fragment_activity_main, automaticModeCapturing ).commit();
        
        bottomNavigationView.setOnItemSelectedListener( item -> {
            if (item.getItemId() == R.id.navigation_home) {
                replaceFragment( new HomeFragment() );
                return true;
            } else if (item.getItemId() == R.id.navigation_maps) {
                replaceFragment( new MapsFragment() );
                return true;
            } else if (item.getItemId() == R.id.navigation_dashboard) {
                replaceFragment( new DashboardFragment() );
                return true;
            } else if (item.getItemId() == R.id.navigation_profile) {
                replaceFragment( new ProfileFragment() );
                return true;
            }
            return false;
        } );
        
    }
    
    private void replaceFragment( Fragment fragment ) {
        getSupportFragmentManager().beginTransaction().replace( R.id.nav_host_fragment_activity_main, fragment ).commit();
    }
    
    private void requestRequiredPermissions() {
        
        if (ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission( this,
                        Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED) {
            
            Toast.makeText( this, "Location Permission Granted", Toast.LENGTH_SHORT ).show();
            
        } else {
            
            if (ActivityCompat.shouldShowRequestPermissionRationale( this,
                    Manifest.permission.ACCESS_FINE_LOCATION )) {
                new AlertDialog.Builder( this )
                        .setMessage( "Required Location Permission" )
                        .setTitle( "Location" )
                        .setPositiveButton( "ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick( DialogInterface dialogInterface, int i ) {
                                ActivityCompat.requestPermissions( MainActivity.this, Permissions,
                                        REQUEST_CODE );
                                PermissionsFlag = true;
                            }
                        } )
                        .setNegativeButton( "Cancel", ( dialogInterface, i ) -> {
                            dialogInterface.dismiss();
                        } );
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult( int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults ) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );
        
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0) {
                
                for (int i = 0; i <= Permissions.length; i++) {
                    if (grantResults[ i ] == PackageManager.PERMISSION_DENIED) {
                        
                        new AlertDialog.Builder( this )
                                .setTitle( "Location" )
                                .setMessage( "Last and Final time" )
                                .setPositiveButton( "ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick( DialogInterface dialogInterface, int i ) {
                                        Intent intent =
                                                new Intent( Settings.ACTION_APPLICATION_DETAILS_SETTINGS );
                                        intent.setData( Uri.parse( "package:" + getPackageName() ) );
                                        startActivity( intent );
                                        
                                        dialogInterface.dismiss();
                                    }
                                } )
                                .setNegativeButton( "Cancel",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick( DialogInterface dialogInterface, int i ) {
                                                dialogInterface.dismiss();
                                            }
                                        } )
                                .create()
                                .show();
                        
                    }
                    
                }
            }
        }
        
    }
}