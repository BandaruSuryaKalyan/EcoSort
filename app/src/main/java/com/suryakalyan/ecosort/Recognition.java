package com.suryakalyan.ecosort;

import android.graphics.RectF;

public class Recognition {
    private String id;
    private String title;
    private float confidence;
    private RectF location;  // You can include location information if needed
    
    public Recognition(String id, String title, float confidence, RectF location) {
        this.id = id;
        this.title = title;
        this.confidence = confidence;
        this.location = location;
    }
    
    public String getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public float getConfidence() {
        return confidence;
    }
    
    public RectF getLocation() {
        return location;
    }
}