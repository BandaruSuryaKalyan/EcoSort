package com.suryakalyan.ecosort;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
    
    ManualCaptureFragment manualCaptureFragment = new ManualCaptureFragment();
    AutomaticModeCapturing automaticModeCapturing = new AutomaticModeCapturing();
    Button manualPhotoButton,automaticPhotoButton;
    
    
    @Nullable
    @Override
    public View onCreateView( @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.fragment_home, container, false );
        
        manualPhotoButton = view.findViewById( R.id.manualPhotoButton );
        automaticPhotoButton = view.findViewById( R.id.AutomaticCapture );
        manualPhotoButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                requireActivity().getSupportFragmentManager().beginTransaction().replace( R.id.nav_host_fragment_activity_main, manualCaptureFragment ).commit();
            }
        } );
        
        automaticPhotoButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                requireActivity().getSupportFragmentManager().beginTransaction().replace( R.id.nav_host_fragment_activity_main, automaticModeCapturing ).commit();
            }
        } );
        
        return view;
    }
}