package org.example;


import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.pubsub.v1.*;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class LandmarksApp {

    public static String PROJECT_ID = "cn2223-t1-g08";
    public static String key = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
    public static String IMAGES_BUCKET_NAME = "landmark_detection_app_images";
    public static String MAPS_BUCKET_NAME = "landmark_detection_app_maps";
    public static Storage storage;

    public static Subscriber subscribeMessages(String projectID, String subscriptionName) {

        ProjectSubscriptionName projSubscriptionName = ProjectSubscriptionName.of(
            projectID, subscriptionName);
        Subscriber subscriber =
            Subscriber.newBuilder(projSubscriptionName, new MessageReceiveHandler(key))
                .build();
        subscriber.startAsync().awaitRunning();
        return subscriber;
    }

    public static void main(String[] args){

        initStorage();

        String subscriptionName = "detectionworkers-sub";
        String projectID = PROJECT_ID;
        Subscriber subscriber = subscribeMessages(projectID, subscriptionName);
        System.out.println("Started listening...");
        subscriber.awaitTerminated();

        System.out.println("Terminating...");

    }

    private static void initStorage() {

        StorageOptions storageOptions;

        storageOptions = StorageOptions.getDefaultInstance();

        LandmarksApp.storage = storageOptions.getService();
        String projID = storageOptions.getProjectId();
        if (projID != null) System.out.println("Current Project ID:" + projID);
        else {
            System.out.println("The environment variable GOOGLE_APPLICATION_CREDENTIALS isn't well defined!!");
            System.exit(-1);
        }
    }
}
