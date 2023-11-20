package com.suryakalyan.ecosort;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class AutomaticModeCapturing extends Fragment {
    
    private static final String TAG = "AutomaticModeCapturing";
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int CAPTURE_IMAGE_REQUEST_CODE = 1001;
    private static final int CAPTURE_INTERVAL = 5000; // 5 seconds
    private static int imageWidth = 1080; // Set your desired width
    private static int imageHeight = 1920;
    
    private Handler captureHandler;
    private boolean capturingEnabled = false;
    private LottieAnimationView animationView;
    private StorageReference storageRef;
    private File currentImageFile;
    
    AppCompatButton automaticCaptureButton;
    private CameraManager cameraManager;
    private ImageReader imageReader;
    private CameraDevice cameraDevice;
    private ImageView textImageView;
    
    private String[] Permissions = { android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA, android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS };
    
    @Nullable
    @Override
    public View onCreateView( @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.fragment_automatic_capturing, container, false );
        
        automaticCaptureButton = view.findViewById( R.id.AutomaticCapture );
        textImageView = view.findViewById( R.id.textImageView );
        
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
                initializeCamera();
                captureHandler.postDelayed( this, CAPTURE_INTERVAL );
            }
        }
    };
    
    private void initializeCamera() {
        cameraManager = (CameraManager) requireActivity().getSystemService( Context.CAMERA_SERVICE );
        
        try {
            String cameraId = getCameraId();
            if (cameraId != null) {
                if (ActivityCompat.checkSelfPermission( getContext(), Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                cameraManager.openCamera( cameraId, cameraStateCallback, null );
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    
    private String getCameraId() throws CameraAccessException {
        String[] cameraIds = cameraManager.getCameraIdList();
        for (String id : cameraIds) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics( id );
            if (characteristics.get( CameraCharacteristics.LENS_FACING ) == CameraCharacteristics.LENS_FACING_BACK) {
                return id;
            }
        }
        return null;
    }
    
    private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened( @NonNull CameraDevice camera ) {
            cameraDevice = camera;
            createCameraPreview();
        }
        
        @Override
        public void onDisconnected( @NonNull CameraDevice camera ) {
            camera.close();
            cameraDevice = null;
        }
        
        @Override
        public void onError( @NonNull CameraDevice camera, int error ) {
            camera.close();
            cameraDevice = null;
        }
    };
    private void createCameraPreview() {
        try {
            imageReader = ImageReader.newInstance(imageWidth, imageHeight, ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
            
            final CaptureRequest.Builder captureBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            
            cameraDevice.createCaptureSession( Arrays.asList(imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            try {
                                cameraCaptureSession.capture(captureBuilder.build(), captureCallback, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        
                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                processCapturedImage(image);
                image.close();
            }
        }
    };
    private void processCapturedImage(Image image) {
        // Convert the Image to a Bitmap
        Bitmap bitmap = convertImageToBitmap(image);
        textImageView.setImageBitmap(bitmap);
        // uploadImageToFirebase(bitmap);
    }
    
    private Bitmap convertImageToBitmap(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
    
    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            closeCamera();
        }
    };
    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
    
    
    
    private void uploadImageToFirebase(Image image) {
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