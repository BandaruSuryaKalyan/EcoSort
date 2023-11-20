package com.suryakalyan.ecosort;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AutomaticModeCapturing extends Fragment {
    
    private static final String TAG = "AutomaticModeCapturing";
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int CAPTURE_IMAGE_REQUEST_CODE = 1001;
    private static final int CAPTURE_INTERVAL = 5000; // 5 seconds
    
    private Handler captureHandler;
    private boolean capturingEnabled = false;
    
    private File currentImageFile;
    AppCompatButton automaticCaptureButton;
    private LottieAnimationView animationView;
    private StorageReference storageRef;
    
    private String[] Permissions = { android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA, android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS };
    
    @Nullable
    @Override
    public View onCreateView( @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.fragment_automatic_capturing, container, false );
        
        automaticCaptureButton = view.findViewById( R.id.AutomaticCapture );
        
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        
        requestPermissionsIfNeeded();
        
        automaticCaptureButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                startCapturing();
            }
        } );
        
        
        return view;
    }
    
    private void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission( requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions( requireActivity(), Permissions,
                        PERMISSION_REQUEST_CODE );
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult( int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED) {
                startCapturing();
            } else {
                Toast.makeText( requireContext(), "Permission denied. Cannot capture images.", Toast.LENGTH_SHORT ).show();
            }
        }
    }
    
    private void startCapturing() {
        capturingEnabled = true;
        captureHandler = new Handler( Looper.getMainLooper() );
        captureHandler.postDelayed( captureRunnable, CAPTURE_INTERVAL );
        Toast.makeText( requireContext(), "Capturing started", Toast.LENGTH_SHORT ).show();
    }
    
    private void stopCapturing() {
        capturingEnabled = false;
        if (captureHandler != null) {
            captureHandler.removeCallbacksAndMessages( null );
        }
        Toast.makeText( requireContext(), "Capturing stopped", Toast.LENGTH_SHORT ).show();
    }
    
    private Runnable captureRunnable = new Runnable() {
        @Override
        public void run() {
            if (capturingEnabled) {
                captureImage();
                captureHandler.postDelayed( this, CAPTURE_INTERVAL );
            }
        }
    };
    
    private void captureImage() {
        // Ensure external storage is available
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals( state )) {
            
            File autoCapturesFolder = new File(requireContext().getExternalFilesDir(null), "autoCapturesFolder");
            if (!autoCapturesFolder.exists()) {
                autoCapturesFolder.mkdirs();
            }
            
            String timeStampOnlyDate = new SimpleDateFormat( "dd-MM-yyyy",
                    Locale.getDefault() ).format( new Date() );
            
            File autoCapturesFolderWithDate = new File(autoCapturesFolder, timeStampOnlyDate);
            if (!autoCapturesFolder.exists()) {
                autoCapturesFolder.mkdirs();
            }
            
            String timeStamp =
                    new SimpleDateFormat( "dd-MM-yyyy  HH-mm-ss", Locale.getDefault() ).format( new Date() );
            String imageFileName = "IMG_" + timeStamp + ".jpg";
            
            // Create image file
            Intent cameraIntent = new Intent( MediaStore.ACTION_IMAGE_CAPTURE );
            currentImageFile = new File( autoCapturesFolderWithDate, imageFileName );
            if (currentImageFile != null) {
                Uri photoUri = FileProvider.getUriForFile( requireContext(), requireContext().getPackageName() + ".provider", currentImageFile );
                cameraIntent.putExtra( MediaStore.EXTRA_OUTPUT, photoUri );
                cameraIntent.putExtra( "android.intent.extra.quickCapture", true );
                startActivityForResult( cameraIntent, CAPTURE_IMAGE_REQUEST_CODE );
            }
        } else {
            Log.e( TAG, "External storage not available." );
        }
    }
    
    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult( requestCode, resultCode, data );
        
        if (requestCode == CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == getActivity().RESULT_OK) {
                // Image captured successfully, upload to Firebase
                Log.d( TAG, "Image captured: " + currentImageFile.getAbsolutePath() );
            } else {
                // Capture failed or canceled
                Log.e( TAG, "Image capture Failed" );
            }
        }
    }
    
    private void uploadImageToFirebase() {
        Uri fileUri = Uri.fromFile( currentImageFile );
        StorageReference imageRef = storageRef.child( "images/" + currentImageFile.getName() );
        
        imageRef.putFile( fileUri )
                .addOnSuccessListener( new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess( UploadTask.TaskSnapshot taskSnapshot ) {
                        // Image uploaded successfully
                        Log.d( TAG, "Image uploaded to Firebase" );
                        // Optionally, you can do further processing or update UI here
                    }
                } )
                .addOnFailureListener( new OnFailureListener() {
                    @Override
                    public void onFailure( @NonNull Exception e ) {
                        // Handle unsuccessful uploads
                        Log.e( TAG, "Error uploading image to Firebase: " + e.getMessage() );
                        // Optionally, you can display an error message to the user
                    }
                } );
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCapturing();
    }
}