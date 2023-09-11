package org.example;


import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FireStoreOperations {

    Firestore db;
    String currentCollection = "ImagesAndLandmarks";

    public void init(String pathFileKeyJson) throws IOException {
        GoogleCredentials credentials = null;
        if (pathFileKeyJson != null) {
            InputStream serviceAccount = new FileInputStream(pathFileKeyJson);
            credentials = GoogleCredentials.fromStream(serviceAccount);
        } else {
            // use GOOGLE_APPLICATION_CREDENTIALS environment variable
            credentials = GoogleCredentials.getApplicationDefault();
        }
        FirestoreOptions options = FirestoreOptions
                .newBuilder().setCredentials(credentials).build();
        db = options.getService();
    }

    public void close() throws Exception {
        db.close();
    }


    public void createDocument(String requestID, List<DetectedLandmark> detectedLandmarks) {

        CollectionReference docs = db.collection(currentCollection);
        DocumentReference newDoc = docs.document(requestID);

        List<String> locationsNames = new ArrayList<String>(0);
        List<String> locationPositions = new ArrayList<String>(0);
        List<String> mapIds = new ArrayList<String>(0);
        List<Double> confidences = new ArrayList<Double>(0);

        for (DetectedLandmark obj : detectedLandmarks) {
            locationsNames.add(obj.name);
            locationPositions.add(obj.latitude + "," + obj.longitude);
            mapIds.add("landmark_detection_app_maps/" + obj.id);
            confidences.add(obj.confidence);
        }

        HashMap<String, Object> map = new HashMap<String, Object>() {
            {
                put("ImageId", "landmark_detection_app_images/" + requestID);
                put("LocationName", locationsNames);
                put("LocationPosition", locationPositions);
                put("MapId", mapIds);
                put("confidence", confidences);
            }
        };
        ApiFuture<WriteResult> objects = newDoc.create(map);
        objects = newDoc.update(map);

    }

}
