//package isel.cn;
//
//import cn2223tf.ImageResponse;
//import cn2223tf.Metadata;
//import io.grpc.stub.StreamObserver;
//
//import java.awt.image.BufferedImage;
//import javax.imageio.ImageIO;
//import java.io.*;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//
//public class ClientStreamObserverImageResponse implements StreamObserver<ImageResponse> {
//
//    ByteArrayOutputStream writer;
//    String filename;
//    String fileType;
//    String destinationPath;
//
//    public ClientStreamObserverImageResponse() {
//        this.filename = "";
//        this.fileType = "png";
//        this.destinationPath = "src/main/java/isel/cn/images";
//        this.writer = new ByteArrayOutputStream();
//    }
//
//    @Override
//    public void onNext(ImageResponse imageResponse) {
//        try {
//            if(imageResponse.getImage().hasMetadata()) {
//                setMetadata(imageResponse.getImage().getMetadata());
//            }
//            writer.write(imageResponse.getImage().getContent().toByteArray());
//            writer.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void onError(Throwable throwable) {
//        System.out.println();
//        System.err.println("An Error Ocurred! The processed image <"+this.filename+"> couldn't be accessed.");
//    }
//
//    @Override
//    public void onCompleted() {
//        try {
//            saveProcessedImage();
//            writer.close();
//
//        } catch ( Exception  e) {
//            e.printStackTrace();
//        }
//        System.out.println();
//        System.out.println("Image <"+this.filename+'.'+this.fileType+"> downloaded to <"+this.destinationPath+">");
//        System.out.println("Done");
//    }
//
//    private void setMetadata(Metadata metadata) {
//        this.filename = metadata.getName();
//        this.fileType = metadata.getType();
//    }
//
//    private void saveProcessedImage() throws IOException {
//        Path path = Paths.get(destinationPath, filename + "." + fileType);
//        File imageFile = path.toFile();
//
//        try (InputStream is = new ByteArrayInputStream(writer.toByteArray());
//             FileOutputStream fos = new FileOutputStream(imageFile)) {
//
//            byte[] buffer = new byte[4096];
//            int bytesRead;
//            while ((bytesRead = is.read(buffer)) != -1) {
//                fos.write(buffer, 0, bytesRead);
//            }
//
//            System.out.println("File created successfully.");
//        } catch (IOException e) {
//            System.out.println("Error saving the image: " + e.getMessage());
//        }
//    }
//}

