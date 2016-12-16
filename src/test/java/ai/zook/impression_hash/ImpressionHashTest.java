package ai.zook.impression_hash;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


/**
 * Created by aaaton on 2016/12/16.
 * Tests basic features of ImpressionHash
 */
public class ImpressionHashTest {
    private final String dir = "src/test/resources/";
    private final String logo1 = dir + "logo1.png";
    private final String logo2 = dir + "logo2.png";
    private final String output1 = dir + "output1.png";
    private final String output2 = dir + "output2.png";

    @Test
    public void Similarity() throws IOException {
        ImpressionHash hash1 = new ImpressionHash(getImage(logo1));
        hash1.save(output1);
        ImpressionHash hash2 = new ImpressionHash(getImage(logo2));
        hash2.save(output2);
        assert hash1.similarityTo(hash2) > 0.9;
    }

    @Test
    public void HashToBools() throws IOException {
        ImpressionHash hash1 = new ImpressionHash(getImage(logo1));
        hash1.save(output1);
        ImpressionHash hash2 = new ImpressionHash(hash1.toString());
        assert hash1.toString().equals(hash2.toString());
    }

    @Test
    public void BenchHashToBools() {
        ImpressionHash hash1 = new ImpressionHash(getImage(logo2));
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            new ImpressionHash(hash1.toString());
        }
        System.out.println((System.nanoTime() - start) / 1000000 + "ms");
    }

    private BufferedImage getImage(String path) {
        try {
            return ImageIO.read(Files.newInputStream(Paths.get(path)));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

    }
}