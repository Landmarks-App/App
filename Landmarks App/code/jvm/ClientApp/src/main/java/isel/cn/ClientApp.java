package isel.cn;

import cn2223tf.*;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Scanner;

public class ClientApp {

    private static int svcPort = 8000;
    private static ManagedChannel channel;
    private static CN2223TFServiceGrpc.CN2223TFServiceStub noBlockStub;

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length > 0) {
            svcPort = Integer.parseInt(args[0]);
        }

        loadMenu();

    }

    static void setupServerConnection(String svcIP, Integer svcPort) {
        channel = ManagedChannelBuilder.forAddress(svcIP, svcPort)
                .usePlaintext()
                .build();

        noBlockStub = CN2223TFServiceGrpc.newStub(channel);
    }

    static int Menu() {
        Scanner scan = new Scanner(System.in);
        int option;
        do {
            System.out.println("######## MENU ##########");
            System.out.println("Options for Google Storage Operations:");
            System.out.println(" 1: uploadImage");
            System.out.println(" 2: getLandmarks");
            System.out.println(" 3: getImage");
            System.out.println(" 4: getAllImages");
            System.out.println("99: Exit");
            System.out.print("Enter an Option: ");
            option = scan.nextInt();
        } while (!((option >= 1 && option <= 4) || option == 99));
        return option;
    }


    static void loadMenu() throws IOException, InterruptedException {
        boolean end = false;
        Scanner sc = new Scanner(System.in);
        System.out.println("Searching for available servers...");
        String[] serversIP = getAvailableServers();
        String serverIP = "";
        if (serversIP != null && serversIP.length > 0) {
            serverIP = selectAvailableServer(serversIP);
            serverIP = serverIP.equals("") ? "localhost" : serverIP;
        } else {
            serverIP = "localhost";
        }
        System.out.println("Server IP selected: " + serverIP);

        setupServerConnection(serverIP, svcPort);
        while (!end) {
            try {
                int option = Menu();
                switch (option) {
                    case 1 :
                        System.out.println();
                        System.out.println("Insert image path");
                        String imagePath = sc.next();
                        uploadImage(imagePath);
                        System.out.println();
                        break;
                    case 2 :
                        System.out.println();
                        System.out.println("Insert requestID ");
                        String uuid = sc.next();
                        getLandmarks(uuid);
                        System.out.println();
                    break;
                    case 3 :
                        System.out.println();
                        System.out.println("Insert requestID ");
                        uuid = sc.next();
                        getImage(uuid);
                        System.out.println();
                        break;
                    case 4 :
                        System.out.println();
                        Double confidence = verifyConfidence(sc);
                        getAllImages(confidence);
                        System.out.println();
                        break;
                    case 99 :
                        end = true;
                        break;
                }
            } catch (Exception ex) {
                System.out.println("Error executing operations!");
                ex.printStackTrace();
            }
        }
        channel.shutdown();
    }

    private static String selectAvailableServer(String[] serversIP) {
        int rnd = new Random().nextInt(serversIP.length);
        return serversIP[rnd];
    }


    private static String[] getAvailableServers() throws IOException, InterruptedException {
        String cfURL = "https://europe-west1-cn2223-t1-g08.cloudfunctions.net/funcLookup?";
        cfURL += "projectid=cn2223-t1-g08&";
        cfURL += "europe-west1-b&";
        cfURL += "instance-group-servers";


        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(cfURL))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            System.out.println(response.body());
            return response.body().split(",");
        } else {
            System.out.println("[" + response.statusCode() + "] There was a problem! Server's IP couldn't be accessed");
        }
        return null;
    }

    private static Double verifyConfidence(Scanner sc) {
        Double confidence = null;
        boolean number = false;
        while (!number) {
            System.out.println("Insert the minimum confidence of the object presence in image [0-1]: ");
            try {
                 confidence = Double.valueOf(sc.next());
                if (confidence <= 1.0f && confidence >= 0.0f) {
                    number = true;
                }
            } catch (Exception e) {
                System.out.println("Confidence inserted not valid!");
            }

        }
        return Math.abs(confidence);
    }

    public static void uploadImage(String imagePath) throws IOException {

        StreamObserver<Block> streamObserver = noBlockStub.uploadImage(new ClientStreamObserverImageUploadResponse(imagePath));

        // upload file as chunk
        Path path = Paths.get(imagePath);
        String filename = String.valueOf(path.getFileName());
        String[] filenameParts = filename.split("\\.");
        String basename = filenameParts[0];
        String extension = filenameParts[1];
        InputStream inputStream = Files.newInputStream(path);
        byte[] bytes = new byte[4096];
        int size;
        while ((size = inputStream.read(bytes)) > 0) {
            Block uploadRequest = Block.newBuilder()
                    .setImage(Image.newBuilder()
                            .setContent(ByteString.copyFrom(bytes, 0, size))
                            .setMetadata(Metadata.newBuilder()
                                    .setName(basename)
                                    .setType(extension)
                                    .build())
                            .build())
                    .build();
            streamObserver.onNext(uploadRequest);
        }

        // close the stream
        inputStream.close();
        streamObserver.onCompleted();
    }

    public static void getLandmarks(String uuid) {
        Identifier identifier = Identifier.newBuilder()
                .setUuid(uuid)
                .build();

        noBlockStub.getLandmarks(identifier, new ClientStreamObserverSubmissionResponse());
    }

    public static void getImage(String uuid) {
        Identifier identifier = Identifier.newBuilder()
                .setUuid(uuid)
                .build();

        noBlockStub.getImage(identifier, new StreamObserver<ImageResponse>() {

            ByteArrayOutputStream writer = new ByteArrayOutputStream();
            String filename = "";
            String fileType = "png";
            String destinationPath = "src/main/java/isel/cn/images";

            @Override
            public void onNext(ImageResponse imageResponse) {
                try {
                    if (imageResponse.getImage().hasMetadata()) {
                        setMetadata(imageResponse.getImage().getMetadata());
                    }
                    writer.write(imageResponse.getImage().getContent().toByteArray());
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println();
                System.err.println("An Error Ocurred! The processed image <" + this.filename + "> couldn't be accessed.");
            }

            @Override
            public void onCompleted() {
                try {
                    saveProcessedImage();
                    writer.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println();
                System.out.println("Image <" + this.filename + '.' + this.fileType + "> downloaded to <" + this.destinationPath + ">");
                System.out.println("Done");
            }

            private void setMetadata(Metadata metadata) {
                this.filename = metadata.getName();
                this.fileType = metadata.getType();
            }

            private void saveProcessedImage() throws IOException {
                Path path = Paths.get(destinationPath, filename + "." + fileType);
                File imageFile = path.toFile();

                try (InputStream is = new ByteArrayInputStream(writer.toByteArray());
                     FileOutputStream fos = new FileOutputStream(imageFile)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }

                    System.out.println("File created successfully.");
                } catch (IOException e) {
                    System.out.println("Error saving the image: " + e.getMessage());
                }
            }
        });
    }

    public static void getAllImages(double certainty) {
        Parameters parameters = Parameters.newBuilder()
                .setCertainty(certainty)
                .build();

        noBlockStub.getAllImages(parameters, new ClientStreamObserverImagesResponse(certainty));
    }
}
