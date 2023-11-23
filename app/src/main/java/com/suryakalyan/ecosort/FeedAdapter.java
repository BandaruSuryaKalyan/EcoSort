package com.suryakalyan.ecosort;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.LocationViewHolder> {
    
    private List<LocationData> feedList;
    
    public FeedAdapter( List<LocationData> feedList ) {
        this.feedList = feedList;
    }
    
    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder( @NonNull ViewGroup parent, int viewType ) {
        View view = LayoutInflater.from( parent.getContext() ).inflate( R.layout.item_dashboard_card, parent, false );
        return new LocationViewHolder( view );
    }
    
    @Override
    public void onBindViewHolder( @NonNull LocationViewHolder holder, int position ) {
        LocationData locationData = feedList.get( position );
        holder.bindData( locationData );
    }
    
    @Override
    public int getItemCount() {
        return feedList.size();
    }
    
    static class LocationViewHolder extends RecyclerView.ViewHolder {
        
        private TextView textViewComplainer;
        private TextView textViewDateTime;
        private TextView textViewLatitude;
        private TextView textViewLongitude;
        private TextView textViewAddress;
        
        public LocationViewHolder( @NonNull View itemView ) {
            super( itemView );
            textViewComplainer = itemView.findViewById( R.id.textViewComplainer );
            textViewDateTime = itemView.findViewById( R.id.textViewDateTimeValue );
            textViewLatitude = itemView.findViewById( R.id.textViewLatitudeValue );
            textViewLongitude = itemView.findViewById( R.id.textViewLongitudeValue );
            textViewAddress = itemView.findViewById( R.id.textViewAddressValue );
        }
        
        public void bindData( LocationData locationData ) {
            textViewComplainer.setText( locationData.getTitle() );
            textViewDateTime.setText( locationData.getSnippet() );
            textViewLatitude.setText( String.valueOf( locationData.getPosition().latitude ) );
            textViewLongitude.setText( String.valueOf( locationData.getPosition().longitude ) );
            textViewAddress.setText( locationData.getAddress() );
            
        }
    }
}