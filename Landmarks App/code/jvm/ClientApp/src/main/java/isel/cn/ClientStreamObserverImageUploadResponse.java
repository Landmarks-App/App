package isel.cn;

import cn2223tf.ImageUploadResponse;
import io.grpc.stub.StreamObserver;

public class ClientStreamObserverImageUploadResponse implements StreamObserver<ImageUploadResponse> {
    private String imageName;

    public ClientStreamObserverImageUploadResponse(String requestID) {
        this.imageName = requestID;
    }

    @Override
    public void onNext(ImageUploadResponse imageUploadResponse) {
        System.out.println();
        System.out.println("Image and Request ID: " + imageUploadResponse.getIdentifier().getUuid());
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println();
        System.err.println("An Error Occurred! Couldn't Upload Image <"+ this.imageName +">.");
    }

    @Override
    public void onCompleted() {
        System.out.println("Done");
    }
}
