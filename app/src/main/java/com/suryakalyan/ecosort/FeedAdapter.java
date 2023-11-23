package com.suryakalyan.ecosort;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

// FeedAdapter.java
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {
    
    private final List<LocationData> locationDataList;
    private static Context context;
    private String userId;  // Added user ID parameter
    
    public FeedAdapter(List<LocationData> locationDataList, String userId) {
        this.locationDataList = locationDataList;
        this.userId = userId;
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
        holder.bindData(locationData, userId);
    }
    
    @Override
    public int getItemCount() {
        return locationDataList.size();
    }
    
    public static class FeedViewHolder extends RecyclerView.ViewHolder {
        
        private ImageView imageViewLocation;
        private TextView textViewComplainer, textViewDateTime, textViewLatitude, textViewLongitude, textViewAddress;
        
        public FeedViewHolder( @NonNull View itemView ) {
            super( itemView );
            
            context = itemView.getContext();
            
            textViewComplainer = itemView.findViewById( R.id.textViewComplainer );
            textViewDateTime = itemView.findViewById( R.id.textViewDateTimeValue );
            textViewLatitude = itemView.findViewById( R.id.textViewLatitudeValue );
            textViewLongitude = itemView.findViewById( R.id.textViewLongitudeValue );
            textViewAddress = itemView.findViewById( R.id.textViewAddressValue );
            imageViewLocation = itemView.findViewById(R.id.imageView);
        }
        
        public void bindData( LocationData model , String userId) {
            // Bind data to the views
            textViewComplainer.setText( ( model.getTitle() != null ? model.getTitle() : "-" ) );
            textViewDateTime.setText( model.getSnippet() );
            textViewLatitude.setText( ( model.getPosition() != null ? model.getPosition().latitude : "-" ).toString() );
            textViewLongitude.setText( ( model.getPosition() != null ? model.getPosition().longitude : "-" ).toString() );
            textViewAddress.setText( ( model.getAddress() != null ? model.getAddress() : "-" ) );
            
            Glide.with(context)
                    .load(getImageUrl(userId, model.getUuid()))
                    .placeholder(R.drawable.image_loading) // Placeholder image while loading
                    .error(R.drawable.error_image) // Image to display in case of error
                    .into(imageViewLocation);
        }
        private String getImageUrl(String userId, String uuid) {
            // Construct the Firebase Storage URL based on the user ID, folder, and UUID
            return "https://firebasestorage.googleapis.com/v0/b/ecosort-f863c.appspot.com/o/" +
                    userId + "%2FAutoCaptureImages%2F" + uuid + ".jpg?alt=media";
        }
        
        
    }
}