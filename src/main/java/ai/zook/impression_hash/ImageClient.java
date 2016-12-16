package ai.zook.impression_hash;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Created by aaaton on 2016/12/16.
 */
public class ImageClient {

    private final ManagedChannel channel;
    private final HasherGrpc.HasherBlockingStub blockingStub;

    /**
     * Construct client connecting to HashServer at {@code host:port}.
     */
    public ImageClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext(true)
                .build();
        blockingStub = HasherGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void hashImage(BufferedImage image) throws IOException {
        String hash = getHash(image);
        System.out.println(hash);
    }

    public void compareImages(BufferedImage img1, BufferedImage img2) throws IOException {
        String hash1 = getHash(img1);
        String hash2 = getHash(img2);
        ToCompare compare = ToCompare.newBuilder().setFirst(hash1).setSecond(hash2).build();
        HashComparison comparison = blockingStub.similarity(compare);
        System.out.println(comparison.getSimilarity());
    }

    private String getHash(BufferedImage image) throws IOException {
        ByteString bytes = toByteString(image);
        HashRequest req = HashRequest.newBuilder().setImageBytes(bytes).setType(HashRequest.Type.PNG).build();
        HashValue hashValue = blockingStub.impressionHash(req);
        return hashValue.getHash();
    }

    private ByteString toByteString(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        baos.flush();
        return ByteString.copyFrom(baos.toByteArray());
    }

    /**
     * Image client. If provided, the first two elements of {@code args} are the file paths to use in the
     * hashing and comparison. Example usage
     */
    public static void main(String[] args) throws Exception {
        /* Access a service running on the local machine on port 50051 */
        ImageClient client = new ImageClient("localhost", 50051);
        try {
            String filename1 = "src/test/resources/logo1.png";
            String filename2 = "src/test/resources/logo2.png";
            if (args.length > 1) {
                filename1 = args[0]; /* Use the arg as the filename if provided */
                filename2 = args[1];
            }
            BufferedImage image1 = ImageIO.read(Files.newInputStream(Paths.get(filename1)));
            BufferedImage image2 = ImageIO.read(Files.newInputStream(Paths.get(filename2)));
            client.hashImage(image1);
            client.compareImages(image1, image2);
        } finally {
            client.shutdown();
        }
    }

}
