package com.suryakalyan.ecosort;

import android.graphics.BlendMode;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsFragment extends Fragment implements OnMapReadyCallback {
    
    private GoogleMap mMap;
    CardView cardView;
    
    @Nullable
    @Override
    public View onCreateView( @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.fragment_maps, container, false );
        
        cardView = view.findViewById( R.id.card_view_maps );
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            cardView.setRenderEffect( RenderEffect.createBlurEffect( 20, 20,
                    Shader.TileMode.MIRROR ) );
        }
        
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById( R.id.map );
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace( R.id.map, mapFragment ).commit();
        }
        mapFragment.getMapAsync( this );
        
        return view;
    }
    
    @Override
    public void onMapReady( GoogleMap googleMap ) {
        mMap = googleMap;
        
        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng( -34, 151 );
        mMap.addMarker( new MarkerOptions().position( sydney ).title( "Marker in Sydney" ) );
        mMap.moveCamera( CameraUpdateFactory.newLatLng( sydney ) );
    }
}