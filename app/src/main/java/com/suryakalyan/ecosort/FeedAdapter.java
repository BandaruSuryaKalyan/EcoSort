package com.suryakalyan.ecosort;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// FeedAdapter.java
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {
    
    private final List<LocationData> locationDataList;
    
    public FeedAdapter( List<LocationData> locationDataList ) {
        this.locationDataList = locationDataList;
    }
    
    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder( @NonNull ViewGroup parent, int viewType ) {
        View view = LayoutInflater.from( parent.getContext() ).inflate( R.layout.item_dashboard_card, parent, false );
        return new FeedViewHolder( view );
    }
    
    @Override
    public void onBindViewHolder( @NonNull FeedViewHolder holder, int position ) {
        LocationData locationData = locationDataList.get( position );
        holder.bindData( locationData );
    }
    
    @Override
    public int getItemCount() {
        return locationDataList.size();
    }
    
    public static class FeedViewHolder extends RecyclerView.ViewHolder {
        
        private TextView textViewComplainer, textViewDateTime, textViewLatitude, textViewLongitude, textViewAddress;
        
        public FeedViewHolder( @NonNull View itemView ) {
            super( itemView );
            
            textViewComplainer = itemView.findViewById( R.id.textViewComplainer );
            textViewDateTime = itemView.findViewById( R.id.textViewDateTimeValue );
            textViewLatitude = itemView.findViewById( R.id.textViewLatitudeValue );
            textViewLongitude = itemView.findViewById( R.id.textViewLongitudeValue );
            textViewAddress = itemView.findViewById( R.id.textViewAddressValue );
        }
        
        public void bindData( LocationData model ) {
            // Bind data to the views
            textViewComplainer.setText( ( model.getTitle() != null ? model.getTitle() : "-" ) );
            textViewDateTime.setText( model.getSnippet() );
            textViewLatitude.setText( ( model.getPosition() != null ? model.getPosition().latitude : "-" ).toString() );
            textViewLongitude.setText( ( model.getPosition() != null ? model.getPosition().longitude : "-" ).toString() );
            textViewAddress.setText( ( model.getAddress() != null ? model.getAddress() : "-" ) );
        }
    }
}