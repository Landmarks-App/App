package cn2223tf;

import cn2223tf.firestore.*;
import cn2223tf.pubsub.StreamObserverImage;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class ServerApp extends CN2223TFServiceGrpc.CN2223TFServiceImplBase {

    private static String IMAGES_BUCKET_NAME = "landmark_detection_app_images";
    private static String MAPS_BUCKET_NAME = "landmark_detection_app_maps";
    private static int svcPort = 8000;
    private static Storage storage = null;

    static final String COLLECTION_NAME = "ImagesAndLandmarks";

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length > 0) {
            svcPort = Integer.parseInt(args[0]);
        }
        initStorage();
        initGRPC();

        System.out.println("Terminating...");
    }

    private static void initGRPC() throws IOException {
        io.grpc.Server svc = ServerBuilder.forPort(svcPort).addService(new ServerApp()).build();
        svc.start();
        System.out.println("Images Bucket Name = " + IMAGES_BUCKET_NAME);
        System.out.println("Maps Bucket Name = " + MAPS_BUCKET_NAME);
        System.out.println("Started listening...");

        try {
            svc.awaitTermination();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void initStorage() {

        StorageOptions storageOptions;

        storageOptions = StorageOptions.getDefaultInstance();

        ServerApp.storage = storageOptions.getService();
        String projID = storageOptions.getProjectId();
        if (projID != null) System.out.println("Current Project ID:" + projID);
        else {
            System.out.println("The environment variable GOOGLE_APPLICATION_CREDENTIALS isn't well defined!!");
            System.exit(-1);
        }
    }
    public byte[] downloadBlobFromBucket(String bucketName, String blobName) throws IOException {
        BlobId blobId = BlobId.of(bucketName, blobName);

        Blob blob = storage.get(blobId);
        if (blob == null) {
            System.out.println("No such Blob exists !");
            return null;
        }

        ByteArrayOutputStream downloadedBlob = new ByteArrayOutputStream();
        if (blob.getSize() < 1_000_000) {

            // Blob is small read all its content in one request
            byte[] content = blob.getContent();
            downloadedBlob.write(content);
        } else {

            // When Blob size is big or unknown use the blob's channel reader.
            try (ReadChannel reader = blob.reader()) {

                byte[] bytes = new byte[64 * 1024];
                while (reader.read(ByteBuffer.wrap(bytes)) > 0) {
                    downloadedBlob.write(bytes);
                }
            }
        }
        System.out.println(downloadedBlob.size());
        return downloadedBlob.toByteArray();
    }

    @Override
    public StreamObserver<Block> uploadImage(StreamObserver<ImageUploadResponse> responseObserver) {
        System.out.println();
        System.out.println("Uploading image...");
        return new StreamObserverImage(responseObserver, storage, IMAGES_BUCKET_NAME);
    }

    @Override
    public void getLandmarks(Identifier identifier, StreamObserver<SubmissionResponse> responseObserver) {
        String imageName = identifier.getUuid();

        System.out.println();
        System.out.println("Getting objects of " + imageName+".jpg");

        // GET objects by image name
        Operations op = new Operations();
        try {
            op.init(null);

            List<DetectedLandmark> imageObjects = op.getLandmarksOfImage(imageName);
            SubmissionResponse.Builder responseBuilder = SubmissionResponse.newBuilder();

            for(DetectedLandmark object : imageObjects) {

                Double latitude = object.getLatitude();
                Double longitude = object.getLongitude();
                Double confidence = object.getConfidence();
                String name = object.getName();

                DetectedLandmark landmark = DetectedLandmark.newBuilder()
                        .setName(name)
                        .setLatitude(latitude)
                        .setLongitude(longitude)
                        .setConfidence(confidence)
                        .build();
                responseBuilder.addLandmarks(landmark);
                System.out.println("Landmark Identified:");
                System.out.println("Name: " + name + " | Confidence: " + confidence);
                System.out.println("Latitude: " + latitude + " | Longitude: " + longitude);
            }
            SubmissionResponse response = responseBuilder.build();
            responseObserver.onNext(response);
            System.out.println("Finished request");
            op.close();
        } catch (Exception e) {
            e.printStackTrace();
            responseObserver.onError(new Throwable());
        }

        responseObserver.onCompleted();
    }

    @Override
    public void getImage(Identifier request, StreamObserver<ImageResponse> responseObserver) {
        String uuId =request.getUuid();
        System.out.println();

        Operations op = new Operations();

        //System.out.println("Getting processed image: " + imageName);
        try {



            op.init(null);


            String[] map = op.getMapOfImage(uuId).split("/");

            String bucket = map[0];
            String blob = map[1]+".png";

            byte[] imageBytes = downloadBlobFromBucket(bucket, blob);
            String[] filenameParts = blob.split("\\.");
            String basename = filenameParts[0];
            String extension = filenameParts[1];
            InputStream inputStream = new ByteArrayInputStream(imageBytes);

            byte[] bytes = new byte[4096];
            int size;
            while ((size = inputStream.read(bytes)) > 0){
                ImageResponse response = ImageResponse.newBuilder()
                        .setImage(Image.newBuilder()
                                .setContent(ByteString.copyFrom(bytes, 0 , size))
                                .setMetadata(Metadata.newBuilder()
                                        .setName(basename)
                                        .setType(extension)
                                        .build())
                                .build())
                        .build();
                responseObserver.onNext(response);
            }

            // close the stream
            inputStream.close();
            responseObserver.onCompleted();
            System.out.println("Finished request");
        } catch (IOException e) {
            e.printStackTrace();
            responseObserver.onError(new Throwable());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void getAllImages(Parameters request, StreamObserver<ImagesResponse> responseObserver) {
        Operations op = new Operations();
        try {
            op.init(null);
            System.out.println();
            System.out.println("Searching landmarks with certainty above <"+ request.getCertainty() +">");
            List<IdentifiedImage> images = op.getImagesByAboveCertainConfidence(request.getCertainty());
            ImagesResponse.Builder responseBuilder = ImagesResponse.newBuilder();
            responseBuilder.addAllIdentifiedImage(images);
            ImagesResponse response = responseBuilder.build();
            responseObserver.onNext(response);

            System.out.println("Finished request");
            op.close();
        } catch (Exception e) {
            e.printStackTrace();
            responseObserver.onError(new Throwable("Invalid Query Parameters"));
        }
        responseObserver.onCompleted();

    }
}