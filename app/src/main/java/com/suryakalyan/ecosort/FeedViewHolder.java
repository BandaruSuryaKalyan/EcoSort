package com.suryakalyan.ecosort;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.suryakalyan.ecosort.LocationData;

import java.util.List;

// FeedViewHolder.java
public class FeedViewHolder extends RecyclerView.ViewHolder {
    
    private TextView textViewComplainer, textViewDateTime, textViewLatitude, textViewLongitude, textViewAddress;
    private ImageView imageViewUpvote, imageViewDownvote, imageViewShare;
    
    public FeedViewHolder(@NonNull View itemView) {
        super(itemView);
        
        textViewComplainer = itemView.findViewById(R.id.textViewComplainer);
        textViewDateTime = itemView.findViewById(R.id.textViewDateTimeValue);
        textViewLatitude = itemView.findViewById(R.id.textViewLatitudeValue);
        textViewLongitude = itemView.findViewById(R.id.textViewLongitudeValue);
        textViewAddress = itemView.findViewById(R.id.textViewAddressValue);
        imageViewUpvote = itemView.findViewById(R.id.imageViewUpvote);
        imageViewDownvote = itemView.findViewById(R.id.imageViewDownvote);
        imageViewShare = itemView.findViewById(R.id.imageViewShare);
    }
    
    public void bindData( LocationData model) {
        // Bind data to the views
        textViewComplainer.setText("Complainer: " + (model.getTitle() != null ? model.getTitle() : ""));
        textViewDateTime.setText(model.getSnippet());
        textViewLatitude.setText("Latitude: " + (model.getPosition() != null ? model.getPosition().latitude : ""));
        textViewLongitude.setText("Longitude: " + (model.getPosition() != null ? model.getPosition().longitude : ""));
        textViewAddress.setText("Address: " + (model.getAddress() != null ? model.getAddress() : ""));
        
    }
}