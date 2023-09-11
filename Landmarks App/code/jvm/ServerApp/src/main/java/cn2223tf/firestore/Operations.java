package cn2223tf.firestore;

import cn2223tf.DetectedLandmark;
import cn2223tf.IdentifiedImage;
import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firestore.v1.Document;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Operations {

    Firestore db;
    String currentCollection;

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
        currentCollection = "ImagesAndLandmarks";
    }

    public void close() throws Exception {
        db.close();
    }

    public List<DetectedLandmark> getLandmarksOfImage(String uuidD) throws Exception {
        // Single query
        DocumentSnapshot doc = db.collection(currentCollection).document(uuidD).get().get();
        // retrieve  query results asynchronously using query.get()

        List<String> locationNames = (List<String>) doc.get("LocationName");
        List<Double> confidences = (List<Double>) doc.get("confidence");
        List<String> locationPositions = (List<String>) doc.get("LocationPosition");


        List<DetectedLandmark> detectedLandmarks = new ArrayList<>();

        for (int i = 0; i < locationNames.size(); i++) {
            String[] positionParts = locationPositions.get(i).split(",");
            double latitude = Double.parseDouble(positionParts[0].trim());
            double longitude = Double.parseDouble(positionParts[1].trim());

            DetectedLandmark landmark = DetectedLandmark.newBuilder()
                    .setName(locationNames.get(i))
                    .setLatitude(latitude)
                    .setLongitude(longitude)
                    .setConfidence(confidences.get(i))
                    .build();


            detectedLandmarks.add(landmark);

        }
        return detectedLandmarks;

    }

    public String getMapOfImage(String uuidD) throws Exception {
        // Single query
        DocumentSnapshot doc = db.collection(currentCollection).document(uuidD).get().get();
        // retrieve  query results asynchronously using query.get()

        List<String> maps = (List<String>) doc.get("MapId");

        String firstMap = maps.get(0);
        return firstMap.isEmpty() ? "" : firstMap;

    }

    public List<IdentifiedImage> getImagesByAboveCertainConfidence(double confidence) throws Exception {
        // Single query
        Query query = db.collection(currentCollection);

        // Retrieve query results asynchronously using query.get()
        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        List<IdentifiedImage> identifiedImages = new ArrayList<>();

        // Retrieve query results asynchronously using query.get()
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            List<Double> confidences = (List<Double>) doc.get("confidence");
            String imageName = doc.get("ImageId", String.class);
            List<String> locationName = (List<String>) doc.get("LocationName");

            for (int i = 0; i < confidences.size(); i++) {
                if (confidences.get(i) > confidence) {
                    IdentifiedImage image = IdentifiedImage.newBuilder().setName(imageName).setLocation(locationName.get(i)).build();
                    identifiedImages.add(image);
                }
            }
        }

        return identifiedImages;
    }
}
