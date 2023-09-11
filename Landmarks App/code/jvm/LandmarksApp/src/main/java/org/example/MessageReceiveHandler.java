package org.example;

import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.storage.*;
import com.google.pubsub.v1.PubsubMessage;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.example.LandmarksApp.IMAGES_BUCKET_NAME;
import static org.example.LandmarksApp.MAPS_BUCKET_NAME;

public class MessageReceiveHandler implements MessageReceiver {

    private String key;
    Storage storage = null;
    FireStoreOperations operations = new FireStoreOperations();

    public MessageReceiveHandler(String key) {
        this.key = key;
    }

    @Override
    public void receiveMessage(PubsubMessage pubsubMessage, AckReplyConsumer ackReplyConsumer) {
        System.out.println("Message Id:" + pubsubMessage.getMessageId() + "\nData:" + pubsubMessage.getData().toStringUtf8() + ")");
        Map<String, String> atribs = pubsubMessage.getAttributesMap();
        String imageID = null;
        for (String key : atribs.keySet()) {
            System.out.println("Msg Attribute:(" + key + ", " + atribs.get(key) + ")");
            imageID = atribs.get(key);
        }


        // First: Initiate storage
        if (storage == null) {
            initStorage();
        }
        try {
            operations.init(key);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Second: Process image
        List<DetectedLandmark> landmarkList = null;
        try {
            landmarkList = LandmarkDetector.detectLandmarksGcs(imageID);
        } catch (IOException e) {
            try {
                operations.close();

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }

        // Third: Create Document in Firestore with all information
        operations.createDocument(imageID, landmarkList);


        try {
            operations.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        System.out.println("Done");

        ackReplyConsumer.ack();
    }

    public void initStorage() {
        StorageOptions storageOptions = StorageOptions.getDefaultInstance();
        storage = storageOptions.getService();
        String projID = storageOptions.getProjectId();
        if (projID != null) System.out.println("Current Project ID:" + projID);
        else {
            System.out.println("The environment variable GOOGLE_APPLICATION_CREDENTIALS isn't well defined!!");
            System.exit(-1);
        }
    }





}
