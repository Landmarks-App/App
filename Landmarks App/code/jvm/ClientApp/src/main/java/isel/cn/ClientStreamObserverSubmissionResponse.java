package isel.cn;

import cn2223tf.DetectedLandmark;
import cn2223tf.SubmissionResponse;
import io.grpc.stub.StreamObserver;

public class ClientStreamObserverSubmissionResponse implements StreamObserver<SubmissionResponse> {


    @Override
    public void onNext(SubmissionResponse submissionResponse) {
        System.out.println();
        if (submissionResponse.getLandmarksList().size() == 0) {
            System.out.println("No objects in image given bucket and blob.");
            return;
        }
        System.out.println("Objects in image given by requestId:");
        System.out.println("--------------------------------------");
        for (DetectedLandmark landmark : submissionResponse.getLandmarksList()) {
            System.out.println("Name: " + landmark.getName() + " | Latitude: " + landmark.getLatitude() + " | Longitude: " + landmark.getLongitude() + " | Confidence: " + landmark.getConfidence());
        }
        System.out.println("--------------------------------------");
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println();
        System.err.println("An Error Ocurred! Couldn't identify objects of requestID.");
    }

    @Override
    public void onCompleted() {
        System.out.println("Done");
    }
}
