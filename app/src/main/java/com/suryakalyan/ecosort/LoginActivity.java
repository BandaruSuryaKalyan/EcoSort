package com.suryakalyan.ecosort;

import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    ImageView underTheCard;
    
    @Override
    protected void onCreate( @Nullable Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.login_activity );
        
        underTheCard = findViewById( R.id.illustration_under_card_view );
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            underTheCard.setRenderEffect( RenderEffect.createBlurEffect( 10,10,
                    Shader.TileMode.MIRROR ) );
        }
    }
}