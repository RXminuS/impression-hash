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
    private final String dir = "src/test/resources/to_hash/";
    private final String output_dir = "src/test/resources/";
    private final String logo1 = dir + "logo1.png";
    private final String logo2 = dir + "logo2.png";
    private final String one = dir + "mionix1.png";
    private final String two = dir + "mionix2.png";
    private final String three = dir + "mionix3.jpg";
    private final String cat = dir + "laptopcat.jpg";
    private final String transparent = dir + "transparent.png";
    private final String black = dir + "transparent_black.png";
    private final String normalize = dir + "normalize.png";
    private final String transformed = dir + "tranformed.png";
    private final String shifted = dir + "shifted.png";
    private final String maxVal = dir + "maxval.png";


    private final String output1 = output_dir + "output1.png";
    private final String output2 = output_dir + "output2.png";

    @Test
    public void HashOneImage() throws IOException {
        ImpressionHash pHash = new ImpressionHash(getImage(logo1));
        pHash.save(output1);
    }

    @Test
    public void TransparentImage() throws IOException {
        new ImpressionHash(getImage(transparent)).save(output1);
    }

    @Test
    public void TransparentBlackImage() throws IOException {
        new ImpressionHash(getImage(black)).save(output1);
    }

    @Test
    public void NormalizeImage() throws IOException {
        new ImpressionHash(getImage(normalize)).save(output1);
    }

    @Test
    public void CompareInverted() throws IOException {
        ImpressionHash hash1 = new ImpressionHash(getImage(one));
        hash1.save(output1);
        ImpressionHash hash2 = new ImpressionHash(getImage(two));
        hash2.save(output2);
        System.out.println(hash1.similarityTo(hash2));
        assert hash1.similarityTo(hash2) > 0.95;
    }

    @Test
    public void CompareSoSo() {
        ImpressionHash hash1 = new ImpressionHash(getImage(one));
        ImpressionHash hash2 = new ImpressionHash(getImage(three));
        System.out.println(hash1.similarityTo(hash2));
    }

    @Test
    public void CompareWTF() {
        ImpressionHash hash1 = new ImpressionHash(getImage(one));
        ImpressionHash hash2 = new ImpressionHash(getImage(cat));
        System.out.println(hash1.similarityTo(hash2));
        assert hash1.similarityTo(hash2) < 0.6;
    }

    @Test
    public void TestCropping() throws IOException {
        ImpressionHash hash2 = new ImpressionHash(getImage(shifted));
        ImpressionHash hash1 = new ImpressionHash(getImage(normalize));
        hash2.save(output1);
        assert hash1.similarityTo(hash2) > 0.95;
    }

    @Test
    public void Similarity() throws IOException {
        ImpressionHash hash1 = new ImpressionHash(getImage(logo1));
        hash1.save(output1);
        ImpressionHash hash2 = new ImpressionHash(getImage(logo2));
        hash2.save(output2);
        System.out.println(hash1.similarityTo(hash2));
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