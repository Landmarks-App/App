package org.example;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.vision.v1.*;
import com.google.type.LatLng;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.example.LandmarksApp.MAPS_BUCKET_NAME;
import static org.example.LandmarksApp.storage;


public class LandmarkDetector {
    final static int ZOOM = 15; // Streets
    public static String key = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
    final static String SIZE = "600x300";
    // Considera-se que o nomes de imagens correspondem aos nomes de BLOB
    // existentes num bucket de nome BUCKET_NAME no Storage do Projeto

    // A variável de ambiente GOOGLE_APPLICATION_CREDENTIALS
    // deve ter conta de serviço com as roles: Storage Admin + VisionAI Admin

    // Detects landmarks in the specified remote image on Google Cloud Storage.
    public static List<DetectedLandmark> detectLandmarksGcs(String blobGsPath) throws IOException {
        System.out.println("Detecting landmarks for: " + blobGsPath);
        List<AnnotateImageRequest> requests = new ArrayList<>();

        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri("gs://landmark_detection_app_images/" + blobGsPath + ".jpg").build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.LANDMARK_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        List<DetectedLandmark> detectedLandmarks = new ArrayList<>(0);

        // Initialize client that will be used to send requests. This client only needs to be created
        // once, and can be reused for multiple requests. After completing all of your requests, call
        // the "close" method on the client to safely clean up any remaining background resources.
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.format("Error: %s%n", res.getError().getMessage());
                    return Collections.emptyList();
                }

                System.out.println("Landmarks list size: " + res.getLandmarkAnnotationsList().size());
                // For full list of available annotations, see http://g.co/cloud/vision/docs
                boolean first = true;
                for (EntityAnnotation annotation : res.getLandmarkAnnotationsList()) {
                    LocationInfo info = annotation.getLocationsList().listIterator().next();
                    String mapUUID = UUID.randomUUID().toString();
                    System.out.format("Landmark: %s(%f)%n %s%n",
                            annotation.getDescription(),
                            annotation.getScore(),
                            info.getLatLng());

                    getStaticMapSaveImage(info.getLatLng(), mapUUID);
                    detectedLandmarks.add(new DetectedLandmark(annotation.getDescription(),info.getLatLng().getLatitude(),info.getLatLng().getLongitude(),mapUUID, annotation.getScore()));
                }
            }
        }
        return detectedLandmarks;
    }

    private static void getStaticMapSaveImage(LatLng latLng, String uuid) {
        String mapUrl = "https://maps.googleapis.com/maps/api/staticmap?"
                + "center=" + latLng.getLatitude() + "," + latLng.getLongitude()
                + "&zoom=" + ZOOM
                + "&size=" + SIZE
                + "&key=AIzaSyA4mgO0d4rTUUbVdqMMW-2JbF5Zabj1sb0";
        System.out.println(mapUrl);
        try {
            URL url = new URL(mapUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            InputStream in = conn.getInputStream();
            BufferedInputStream bufIn = new BufferedInputStream(in);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            FileOutputStream out = new FileOutputStream(uuid + ".png");
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = bufIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                outputStream.write(buffer, 0, bytesRead);
            }

            bufIn.close();
            in.close();
            out.close();

            byte[] imageBytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            uploadMapToCloudStorage(inputStream, uuid);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String uploadMapToCloudStorage(ByteArrayInputStream inputImage, String uuId) throws IOException {


        String bucketName = MAPS_BUCKET_NAME;

        String blobName = uuId + ".png";


        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        if (inputImage.available() > 1_000_000) {
            // When content is not available or large (1MB or more) it is recommended
            // to write it in chunks via the blob's channel writer.
            try (WriteChannel writer = storage.writer(blobInfo)) {
                byte[] buffer = new byte[1024];
                int limit;
                while ((limit = inputImage.read(buffer)) >= 0) {
                    try {
                        writer.write(ByteBuffer.wrap(buffer, 0, limit));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (inputImage.available() > 0) {
            byte[] bytes = inputImage.readAllBytes();
            // create the blob in one request.
            storage.create(blobInfo, bytes);
        }
        System.out.println("Blob " + blobName + " created in bucket " + bucketName);
        return uuId;
    }

}