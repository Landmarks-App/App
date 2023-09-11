package org.example;

import com.google.cloud.vision.v1.NormalizedVertex;

import java.util.List;

public class DetectedLandmark {

    public DetectedLandmark(String name,double latitude,double longitude,String id,double confidence){
        this.name=name;
        this.latitude=latitude;
        this.longitude=longitude;
        this.id=id;
        this.confidence=confidence;
    }
    public String name;
    public double latitude;
    public double longitude;
    public String id;
    public double confidence;
}
