package com.suryakalyan.ecosort;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;

public class AutomaticModeCapturing extends Fragment implements SensorEventListener {
    
    private static final String TAG = "AutomaticModeCapturing";
    private static final int MOTION_SPEED_THRESHOLD = 2;
    private static final int PITCH_THRESHOLD_LOW = 40;
    private static final int PITCH_THRESHOLD_HIGH = 70;
    
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private LottieAnimationView animationView;
    
    private boolean isMoving = false;
    
    private float[] gravityValues = new float[ 3 ];
    private float[] magneticValues = new float[ 3 ];
    
    private boolean capturingEnabled = false;
    
    @Nullable
    @Override
    public View onCreateView( @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.fragment_automatic_capturing, container, false );
        
        animationView = view.findViewById( R.id.animationView );
        startCapturing();
        return view;
    }
    
    private void startCapturing() {
        sensorManager = (SensorManager) requireActivity().getSystemService( Context.SENSOR_SERVICE );
        
        if (sensorManager != null) {
            accelerometerSensor = sensorManager.getDefaultSensor( Sensor.TYPE_ACCELEROMETER );
            
            if (accelerometerSensor != null) {
                capturingEnabled = true;
                sensorManager.registerListener( this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI );
                startAnimation();
            } else {
                Toast.makeText( requireContext(), "Accelerometer not available", Toast.LENGTH_SHORT ).show();
            }
        } else {
            Toast.makeText( requireContext(), "Sensor Service not available", Toast.LENGTH_SHORT ).show();
        }
    }
    
    private void stopCapturing() {
        capturingEnabled = false;
        if (sensorManager != null) {
            sensorManager.unregisterListener( this );
        }
    }
    
    @Override
    public void onSensorChanged( SensorEvent event ) {
        Log.d( TAG, "Sensing..." );
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravityValues = event.values;
        }
        // Get accelerometer values
        float x = event.values[ 0 ];
        float y = event.values[ 1 ];
        float z = event.values[ 2 ];
        
        // Calculate pitch
        float[] rotationMatrix = new float[ 9 ];
        SensorManager.getRotationMatrixFromVector( rotationMatrix, event.values );
        float[] orientation = new float[ 3 ];
        SensorManager.getOrientation( rotationMatrix, orientation );
        float pitch = orientation[ 1 ];
        
        // Calculate motion speed
        float motionSpeed = Math.abs( x ) + Math.abs( y ) + Math.abs( z );
        
        if ( motionSpeed > MOTION_SPEED_THRESHOLD) {
            // Motion detected
            if (!isMoving) {
                captureImage();
                resumeAnimation();
                isMoving = true;
            }
        } else {
            // Motion stopped
            if (isMoving) {
                pauseAnimation();
                isMoving = false;
            }
            
        }
        
    }
    
    private void captureImage() {
        Log.d( TAG, "Capturing..." );
    }
    
    @Override
    public void onAccuracyChanged( Sensor sensor, int accuracy ) {
        // Do nothing for this example
    }
    
    private void startAnimation() {
        if (animationView != null) {
            animationView.playAnimation();
        }
    }
    
    // Method to stop the animation
    private void stopAnimation() {
        if (animationView != null) {
            animationView.cancelAnimation();
        }
    }
    
    // Method to pause the animation
    private void pauseAnimation() {
        if (animationView != null) {
            animationView.pauseAnimation();
        }
    }
    
    // Method to resume the animation
    private void resumeAnimation() {
        if (animationView != null) {
            animationView.resumeAnimation();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAnimation();
        stopCapturing();
    }
}