package com.suryakalyan.ecosort;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.icu.text.SimpleDateFormat;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.UUID;

public class AutomaticModeCapturing extends Fragment {
    
    private static final String TAG = "AutomaticModeCapturing";
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int CAPTURE_IMAGE_REQUEST_CODE = 1001;
    private static final int CAPTURE_INTERVAL = 5000; // 5 seconds
    private static int imageWidth = 1080; // Set your desired width
    private static int imageHeight = 1920;
    private static final int TOTAL_TARGET_IMAGES = 15;
    private static final float MINIMUM_CONFIDENCE_THRESHOLD = 0.6f;
    private static final int NUM_CLASSES = 9;
    
    private Handler captureHandler;
    private boolean capturingEnabled = false;
    private boolean isLocationRequestInProgress = false;
    private boolean isGpsEnabled = false;
    private LottieAnimationView animationView;
    private StorageReference storageRef;
    private File currentImageFile;
    private DatabaseReference database;
    private SharedPreferences preference;
    private Interpreter tflite;
    private String[] CLASS_LABELS;
    private String userId;
    
    AppCompatButton automaticCaptureButton;
    private CameraManager cameraManager;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private ImageReader imageReader;
    private CameraDevice cameraDevice;
    private ImageView textImageView;
    private File autoCaptureTempFile;
    private ProgressBar progressBar;
    private TextView coinCountTextView, detectionResult;
    private int capturedImageCount = 0;
    private int coinsEarned = 0;
    private int progress;
    
    private String[] Permissions = { android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA, android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION };
    
    @Nullable
    @Override
    public View onCreateView( @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.fragment_automatic_capturing, container, false );
        
        automaticCaptureButton = view.findViewById( R.id.AutomaticCapture );
        textImageView = view.findViewById( R.id.textImageView );
        progressBar = view.findViewById( R.id.progressBar );
        coinCountTextView = view.findViewById( R.id.coinTextView );
        detectionResult = view.findViewById( R.id.detectionResult );
        
        FirebaseStorage storage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
        storageRef = storage.getReference();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient( getActivity() );
        loadModel();
        
        preference = getContext().getSharedPreferences( "UserLoginActivity", MODE_PRIVATE );
        userId = preference.getString( "UserEmailPref", "Unknown Users" );
        
        requestPermissionsIfNeeded();
        
        automaticCaptureButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                LocationManager locationManager =
                        (LocationManager) getActivity().getSystemService( Context.LOCATION_SERVICE );
                isGpsEnabled = locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER );
                
                if (isGpsEnabled && !isLocationRequestInProgress) {
                    if (capturingEnabled) {
                        stopCapturing();
                        // Reset button to its initial state
                        automaticCaptureButton.setText( "Start the Ride" );
                        automaticCaptureButton.setTextColor( Color.parseColor( "#000000" ) );
                        automaticCaptureButton.setBackgroundResource( R.drawable.onepx_black_round_border );
                    } else {
                        startCapturing();
                        // Change button appearance for "Stop the Ride" state
                        automaticCaptureButton.setText( "Stop the Ride" );
                        automaticCaptureButton.setTextColor( Color.parseColor( "#ffffff" ) );
                        automaticCaptureButton.setBackgroundResource( R.drawable.red_gradient );
                    }
                } else {
                    Toast.makeText( getContext(), "Turn On GPS", Toast.LENGTH_SHORT ).show();
                }
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
                stopCapturing();
                Toast.makeText( requireContext(), "Unable to Locate", Toast.LENGTH_SHORT ).show();
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
        if (cameraDevice != null) {
            Toast.makeText( getContext(), "Already Running...", Toast.LENGTH_SHORT ).show();
            return;
        }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                createCameraPreview();
            }
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
    
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private void createCameraPreview() {
        try {
            imageReader = ImageReader.newInstance( imageWidth, imageHeight, ImageFormat.JPEG, 20 );
            imageReader.setOnImageAvailableListener( onImageAvailableListener, null );
            
            final CaptureRequest.Builder captureBuilder =
                    cameraDevice.createCaptureRequest( CameraDevice.TEMPLATE_STILL_CAPTURE );
            captureBuilder.addTarget( imageReader.getSurface() );
            captureBuilder.set( CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO );
            
            // Set auto exposure mode
            captureBuilder.set( CaptureRequest.CONTROL_AE_MODE,
                    CameraMetadata.CONTROL_AE_MODE_OFF );
            captureBuilder.set( CaptureRequest.SENSOR_SENSITIVITY, 400 );
            captureBuilder.set( CaptureRequest.SENSOR_EXPOSURE_TIME, 25000000L );
            // Set auto focus mode
            captureBuilder.set( CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO );
            // Set auto white balance mode
            captureBuilder.set( CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO );
            
            captureBuilder.set( CaptureRequest.CONTROL_SCENE_MODE, CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY );
            
            captureBuilder.set( CaptureRequest.CONTROL_SCENE_MODE, CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY );
            
            captureBuilder.set( CaptureRequest.CONTROL_SCENE_MODE, CameraMetadata.CONTROL_SCENE_MODE_HDR );
            
            
            cameraDevice.createCaptureSession( Arrays.asList( imageReader.getSurface() ),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured( @NonNull CameraCaptureSession cameraCaptureSession ) {
                            try {
                                cameraCaptureSession.capture( captureBuilder.build(), captureCallback, null );
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        
                        @Override
                        public void onConfigureFailed( @NonNull CameraCaptureSession cameraCaptureSession ) {
                        }
                    }, null );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    
    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable( ImageReader reader ) {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                capturedImageCount++;
                processCapturedImage( image );
                image.close();
            }
        }
    };
    
    private void processCapturedImage( Image image ) {
        try {
            ByteBuffer buffer = image.getPlanes()[ 0 ].getBuffer();
            byte[] imageData = new byte[ buffer.remaining() ];
            buffer.get( imageData );
            byte[] rotatedImageData = rotateImageData( imageData, 90 );
            
            Bitmap bitmap = BitmapFactory.decodeByteArray( rotatedImageData, 0, rotatedImageData.length );
            textImageView.setImageBitmap( bitmap );
            
            if (autoCaptureTempFile == null) {
                File autoCapturesFolder = new File( requireContext().getExternalFilesDir( null ), "AutoCaptures" );
                if (!autoCapturesFolder.exists()) {
                    autoCapturesFolder.mkdirs();
                }
                autoCaptureTempFile = new File( autoCapturesFolder, "IMG_" + "autoCapTempImage.jpg" );
            }
            
            try {
                // Append the new image data to the existing file
                FileOutputStream fos = new FileOutputStream( autoCaptureTempFile, false );
                fos.write( rotatedImageData );
                fos.close();
                
                // Upload the entire file to Firebase (if needed)
                 Uri fileUri = FileProvider.getUriForFile(requireContext(), "com.suryakalyan.ecosort" + ".provider", autoCaptureTempFile);
                 getUserCurrentLocationOnes(autoCaptureTempFile);
                String predictedClass = predictImage( rotatedImageData );
                Log.d( TAG, "Predicted Class: " + predictedClass );
                detectionResult.setText( predictedClass );
                detectionResult.setText( predictedClass );
                updateProgressAndCoins();
            } catch (IOException e) {
                e.printStackTrace();
                // Handle the exception, e.g., notify the user or log the error
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception, e.g., notify the user or log the error
        }
    }
    
    
    private String predictImage( byte[] imageData ) {
        int inputTensorIndex = 0; // Assuming the input tensor is the first one
        int[] inputShape = tflite.getInputTensor( inputTensorIndex ).shape();
        int inputWidth = inputShape[ 1 ];
        int inputHeight = inputShape[ 2 ];
        int inputSize = inputWidth * inputHeight * 3 * 4; // Assuming a 3-channel image with 4 bytes per channel (float)
        
        // Convert the image data to a ByteBuffer
        Bitmap bitmap = BitmapFactory.decodeByteArray( imageData, 0, imageData.length );
        Bitmap resizedBitmap = Bitmap.createScaledBitmap( bitmap, inputWidth, inputHeight, true );
        
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect( inputSize );
        inputBuffer.order( ByteOrder.nativeOrder() );
        inputBuffer.rewind();
        
        // Convert the resized bitmap to a ByteBuffer
        int[] intValues = new int[ inputWidth * inputHeight ];
        resizedBitmap.getPixels( intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight() );
        
        for (int pixelValue : intValues) {
            // Normalize the pixel values if needed
            float normalizedPixelValue = ( pixelValue & 0xFF ) / 255.0f;
            inputBuffer.putFloat( normalizedPixelValue );
        }
        
        // Rewind the buffer before running inference
        inputBuffer.rewind();
        
        // Run inference
        float[][] output = new float[ 1 ][ NUM_CLASSES ]; // Assuming one batch and NUM_CLASSES output classes
        tflite.run( inputBuffer, output );
        
        // Get the top 3 predictions
        PriorityQueue<Recognition> pq = new PriorityQueue<>( 3, ( lhs, rhs ) -> Float.compare( rhs.getConfidence(), lhs.getConfidence() ) );
        for (int i = 0; i < NUM_CLASSES; i++) {
            pq.add( new Recognition( "" + i, CLASS_LABELS[ i ], output[ 0 ][ i ], null ) );
        }
        
        // Format the results
        StringBuilder resultStringBuilder = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            Recognition recognition = pq.poll();
            if (recognition.getConfidence() > MINIMUM_CONFIDENCE_THRESHOLD) {
                resultStringBuilder.append( String.format( "%s (%.2f%%)\n", recognition.getTitle(), recognition.getConfidence() * 100.0f ) );
            }
        }
        
        // Return the formatted result or an empty string
        return resultStringBuilder.toString().isEmpty() ? " " : resultStringBuilder.toString();
    }
    
    
    private ByteBuffer loadModelFile( String filename ) throws IOException {
        AssetFileDescriptor fileDescriptor = getActivity().getAssets().openFd( filename );
        FileInputStream inputStream = new FileInputStream( fileDescriptor.getFileDescriptor() );
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map( FileChannel.MapMode.READ_ONLY, startOffset, declaredLength );
    }
    
    private List<String> loadLabels( String filename ) {
        List<String> labels = new ArrayList<>();
        try {
            InputStream inputStream = getActivity().getAssets().open( filename );
            BufferedReader reader = new BufferedReader( new InputStreamReader( inputStream ) );
            String line;
            while (( line = reader.readLine() ) != null) {
                labels.add( line );
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return labels;
    }
    
    // Helper method to find the index of the maximum value in an array
    private int argmax( float[] array ) {
        int best = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[ i ] > array[ best ]) {
                best = i;
            }
        }
        return best;
    }
    
    
    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted( @NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull TotalCaptureResult result ) {
            super.onCaptureCompleted( session, request, result );
            closeCamera();
        }
    };
    
    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
    
    private void getUserCurrentLocationOnes( File imageFile ) {
        LocationRequest request = new LocationRequest();
        request.setPriority( LocationRequest.PRIORITY_HIGH_ACCURACY );
        request.setNumUpdates( 2 );
        
        // Request single location update
        if (ActivityCompat.checkSelfPermission( requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission( requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        String uuid = UUID.randomUUID().toString();
        
        fusedLocationProviderClient.getCurrentLocation( LocationRequest.PRIORITY_HIGH_ACCURACY, null )
                .addOnSuccessListener( location -> {
                    if (location != null) {
                        isLocationRequestInProgress = false;
                        handleLocationResult( location, imageFile, uuid );
                    } else {
                        // Handle the case where location is null
                        isLocationRequestInProgress = false;
                        handleLocationResult( null, imageFile, uuid );  // Pass null location
                        Toast.makeText( getContext(), "Failed to Obtain Location", Toast.LENGTH_SHORT ).show();
                    }
                } )
                .addOnFailureListener( e -> {
                    isLocationRequestInProgress = false;
                    handleLocationResult( null, imageFile, uuid );  // Pass null location
                    Toast.makeText( getContext(), "Failed to Locate", Toast.LENGTH_SHORT ).show();
                } );
    }
    
    private void handleLocationResult( @Nullable Location location, File imageFile, String uuid ) {
        double latitude = 0;
        double longitude = 0;
        
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
        
        String newFileName = renameFile( uuid, imageFile, latitude, longitude );
        File newFile = new File( imageFile.getParentFile(), newFileName );
        
        // Rename the original file to the new name
        if (imageFile.renameTo( newFile )) {
            uploadImageToFirebase( Uri.fromFile( newFile ), newFileName );
            uploadImageDataToRealtimeDatabase( location, uuid );
            
            if (newFile.exists()) {
                if (!newFile.delete()) {
                    Log.e( TAG, "Error deleting original file" );
                }
            } else {
                Log.e( TAG, "File does not exist" );
            }
            
        } else {
            Log.e( TAG, "Error renaming file" );
        }
    }
    
    private String renameFile( String uuid, File originalFile, double latitude, double longitude ) {
        // Format the timestamp
//        String timeStamp = null;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//            timeStamp = new SimpleDateFormat( "dd-MM-yyyy HH-mm-ss", Locale.getDefault() ).format( new Date() );
//        }
        
        // Construct the new filename with location coordinates
        String fileName = uuid;
        
        // Append latitude and longitude if available
//        if (latitude != 0 && longitude != 0) {
//            fileName += " " + latitude + " " + longitude + " UUID:" + uuid;
//        } else {
//            // Append 0 if location details are not available
//            fileName += " 0 0";
//        }
//
        fileName += ".jpg";
        
        return fileName;
    }
    
    
    private void uploadImageDataToRealtimeDatabase( @Nullable Location location, String uuid ) {
        
        double latitude = 0;
        double longitude = 0;
        
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
        
        String address = getAddressFromLatLng( location.getLatitude(), location.getLongitude() );
        
        LocationData locationData = new LocationData( uuid,
                userId, location.getLatitude(),
                location.getLongitude(), address
        );
        
        database.child( "Live Locations" ).child( userId ).push().setValue( locationData );
    }
    
    private void uploadImageToFirebase( Uri imageUri, String fileName ) {
        // Create a StorageReference with a path and filename, organizing by userId
        StorageReference storageRef =
                FirebaseStorage.getInstance().getReference().child( userId ).child(
                        "AutoCaptureImages" ).child( fileName );
        
        // Upload the image file to Firebase Storage
        storageRef.putFile( imageUri )
                .addOnSuccessListener( taskSnapshot -> {
                    // Handle successful upload
                    Log.d( TAG, "Image uploaded to Firebase Storage" );
                    Toast.makeText( getContext(), "Image Uploaded.", Toast.LENGTH_SHORT ).show();
                } )
                .addOnFailureListener( exception -> {
                    // Handle failed upload
                    Log.e( TAG, "Error uploading image to Firebase Storage: " + exception.getMessage() );
                    Toast.makeText( getContext(), "Upload Failed.", Toast.LENGTH_SHORT ).show();
                } );
    }
    
    
    private String getAddressFromLatLng( double latitude, double longitude ) {
        Geocoder geocoder = new Geocoder( getContext(), Locale.getDefault() );
        try {
            List<Address> addresses = geocoder.getFromLocation( latitude, longitude, 1 );
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get( 0 );
                StringBuilder addressStringBuilder = new StringBuilder();
                
                // Add relevant address details (customize as needed)
                if (address.getSubThoroughfare() != null) {
                    addressStringBuilder.append( address.getSubThoroughfare() ).append( ", " );
                }
                if (address.getThoroughfare() != null) {
                    addressStringBuilder.append( address.getThoroughfare() ).append( ", " );
                }
                if (address.getLocality() != null) {
                    addressStringBuilder.append( address.getLocality() ).append( ", " );
                }
                if (address.getPostalCode() != null) {
                    addressStringBuilder.append( address.getPostalCode() ).append( ", " );
                }
                // Add other details like sub-locality, postal code, etc.
                
                return addressStringBuilder.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Address not available";
    }
    
    private byte[] rotateImageData( byte[] imageData, int rotationDegrees ) {
        Bitmap originalBitmap = BitmapFactory.decodeByteArray( imageData, 0, imageData.length );
        
        // Rotate the bitmap
        Matrix matrix = new Matrix();
        matrix.postRotate( rotationDegrees );
        Bitmap rotatedBitmap = Bitmap.createBitmap( originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true );
        
        // Convert the rotated bitmap back to byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBitmap.compress( Bitmap.CompressFormat.JPEG, 100, stream );
        return stream.toByteArray();
    }
    
    private void updateProgressAndCoins() {
        // Update the progressive bar
        progress = ( capturedImageCount % 2 ) * ( 100 / TOTAL_TARGET_IMAGES );
        if (progressBar != null) {
            progressBar.setProgress( progress );
        }
        
        if (capturedImageCount >= TOTAL_TARGET_IMAGES) {
            Toast.makeText( requireContext(), "Target reached!", Toast.LENGTH_SHORT ).show();
        }
        // Update the coin count (assuming 1 coin for every 2 images)
        coinsEarned = capturedImageCount / 2;
        coinCountTextView.setText( "Coins Earned: " + coinsEarned );
    }
    
    private void loadModel() {
        try {
            ByteBuffer model = loadModelFile( "model.tflite" );
            tflite = new Interpreter( model );
            // Load labels from the file
            CLASS_LABELS = loadLabels( "labels.txt" ).toArray( new String[ 0 ] );
            Toast.makeText( getContext(), "Model Loaded", Toast.LENGTH_SHORT ).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCapturing();
    }
}