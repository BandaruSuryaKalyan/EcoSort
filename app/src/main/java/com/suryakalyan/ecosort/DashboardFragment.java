package com.suryakalyan.ecosort;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {
    
    private RecyclerView recyclerView;
    private FeedAdapter locationAdapter;
    private List<LocationData> locationList;
    private DatabaseReference databaseReference;
    private ProgressBar progressBar;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        locationList = new ArrayList<>();
        locationAdapter = new FeedAdapter(locationList);
        recyclerView.setAdapter(locationAdapter);
        
//        progressBar = view.findViewById(R.id.inProgress);
//        startProgressBarRotation(view);
        
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Live Locations");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                locationList.clear();
//                stopProgressBarRotation(view); // Stop rotation when data is loaded
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot locationSnapshot : userSnapshot.getChildren()) {
                        LocationData locationData = locationSnapshot.getValue(LocationData.class);
                        if (locationData != null) {
                            locationList.add(locationData);
                        }
                    }
                }
                locationAdapter.notifyDataSetChanged();
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Firebase Connection Failure", Toast.LENGTH_SHORT).show();
//                stopProgressBarRotation(view); // Stop rotation on error
            }
        });
        
        return view;
    }
    
    private void startProgressBarRotation(View view) {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setRotation(0);
        progressBar.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setRotation(0);
                progressBar.animate().rotationBy(360).setInterpolator(new LinearInterpolator())
                        .setDuration(1000).withEndAction(this).start();
            }
        });
    }
    
    private void stopProgressBarRotation(View view) {
        progressBar.setVisibility(View.GONE);
        progressBar.clearAnimation();
    }
}