package isel.cn;

import cn2223tf.IdentifiedImage;
import cn2223tf.ImagesResponse;
import io.grpc.stub.StreamObserver;

public class ClientStreamObserverImagesResponse implements StreamObserver<ImagesResponse> {

    private final double certainty;

    public ClientStreamObserverImagesResponse(Double certainty) {
        this.certainty = certainty;
    }

    @Override
    public void onNext(ImagesResponse imagesResponse) {
        System.out.println();
        if (imagesResponse.getIdentifiedImageList().size() == 0) {
            System.out.println("No landmarks found with certainty greater than " + this.certainty);
            return;
        }
        System.out.println("-----------------------------------------");
        System.out.println("Landmarks with certainty greater than " + this.certainty);
        System.out.println("-----------------------------------------");
        for (IdentifiedImage identifiedImage : imagesResponse.getIdentifiedImageList()) {
            System.out.println("Image: " + identifiedImage.getName() + " Location: " + identifiedImage.getLocation());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println();
        System.err.println("An Error Ocurred! Invalid query parameters for object");
    }

    @Override
    public void onCompleted() {
        System.out.println("-----------------------------------------");
        System.out.println("Done");
    }
}
