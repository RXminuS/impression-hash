package ai.zook.impression_hash;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

/**
 * Created by aaaton on 2016/12/14.
 * the class follows the pHash algorithm described in http://blog.iconfinder.com/detecting-duplicate-images-using-python/
 * with exception to the blurring and actual hashing
 */
public class ImpressionHash {
    private BufferedImage image;
    private boolean[] changes;
    private boolean[] larger;
    private final int BLACK = 0;
    private final int WHITE = 255;
    private final int fuzziness = 12; // How much noise can we accept?

    public ImpressionHash(BufferedImage image) {
        this.image = image;

//        Scale down the image if it's too large so it's faster to work with
        int pSize = 200;
        if (image.getWidth() > pSize || image.getHeight() > pSize)
            resize(pSize, pSize);

        crop();
        grayScale();

//        The size of the width needs to be one larger than the intended size
        int dWidth = 11;
        int dHeight = 10;
        resize(dWidth, dHeight);
        blur();
        normalize();
        comparePixels();
    }

    public ImpressionHash(String hash) {
        this.changes = hexToBools(hash);
    }

    public float similarityTo(ImpressionHash other) {
        //Only using the changes hash
        float c = 0f;
        for (int i = 0; i < changes.length; i++) {
            if (this.changes[i] == other.changes[i])
                c++;
        }
        return c / (float) changes.length;
    }

    @Override
    public String toString() {
        return changes();
    }

    // Better for jpgs (actual photos). Use blurring if you want to do jpgs.
    public String larger() {
        return boolToHex(larger);
    }

    // Better for pngs (icons, logos & vector art). Can do inverted images etc.
    public String changes() {
        return boolToHex(changes);
    }

    private final String hexChars = "0123456789abcdef";

    private String boolToHex(boolean[] array) {
        int move = 3;
        byte b = 0;
        char[] hexArray = hexChars.toCharArray();
        char[] output = new char[array.length / 4 + (array.length % 4 == 0 ? 0 : 1)];
        for (int i = 0; i < array.length; i++) {
            int c = array[i] ? 1 : 0;
            b += c << move;
            move--;
            if (move < 0) {
                output[i / 4] = hexArray[b];
                move = 3;
                b = 0;
            }
        }
        if (array.length % 4 != 0) output[array.length / 4] = hexArray[b];
        return new String(output);
    }

    private boolean[] hexToBools(String hex) {
        char[] chars = hex.toCharArray();
        int a = 0;
        boolean[] changes = new boolean[hex.length() * 4]; //4 bits per hex char
        for (int i = 0; i < chars.length; i++) {
            int num = hexChars.indexOf(chars[i]);
            for (int j = 3; j >= 0; j--) {
                changes[a] = (num >> j & 1) == 1;
                a++;
            }
        }
        return changes;
    }

    //    Removes unicoloured borders and padding around images
    private void crop() {
        int color = image.getRGB(0, 0);
//        Remove
        if (hasBorder()) {
            while (hasRow(color, 0))
                cropRow(0);
            while (hasRow(color, image.getHeight() - 1))
                cropRow(image.getHeight() - 1);
            while (hasColumn(color, 0))
                cropColumn(0);
            while (hasColumn(color, image.getWidth() - 1))
                cropColumn(image.getWidth() - 1);
        }
    }

    private void cropRow(int yKill) {
        int c;
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight() - 1, image.getType());
        for (int y = 0; y < image.getHeight() - 1; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (y >= yKill) {
                    c = image.getRGB(x, y + 1);
                } else {
                    c = image.getRGB(x, y);
                }
                newImage.setRGB(x, y, c);
            }
        }
        image = newImage;
    }

    private void cropColumn(int xKill) {
        int c;
        BufferedImage newImage = new BufferedImage(image.getWidth() - 1, image.getHeight(), image.getType());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth() - 1; x++) {
                if (x >= xKill) {
                    c = image.getRGB(x + 1, y);
                } else {
                    c = image.getRGB(x, y);
                }
                newImage.setRGB(x, y, c);
            }
        }
        image = newImage;
    }

    private boolean hasBorder() {
        int maxY = image.getHeight() - 1;
        int maxX = image.getWidth() - 1;
        int c = image.getRGB(0, 0);

        if (!colorCanBeBorder(c))
            return false;

        return hasRow(c, 0) && hasRow(c, maxY) && hasColumn(c, 0) && hasColumn(c, maxX);
    }

    private boolean hasRow(int color, int y) {
        for (int x = 0; x < image.getWidth(); x++) {
            if (!withinReason(color, image.getRGB(x, y))) {
                return false;
            }
        }
        return true;
    }

    private boolean hasColumn(int color, int x) {
        for (int y = 0; y < image.getHeight(); y++) {
            if (!withinReason(color, image.getRGB(x, y))) {
                return false;
            }
        }
        return true;
    }

    private boolean withinReason(int c, int oc) {
        c = (c & 0xFF) + (c >> 8 & 0xFF) + (c >> 16 & 0xFF) + (c >> 24 & 0xFF);
        oc = (oc & 0xFF) + (oc >> 8 & 0xFF) + (oc >> 16 & 0xFF) + (oc >> 24 & 0xFF);
        return c < oc + fuzziness && c > oc - fuzziness;
    }

    private boolean colorCanBeBorder(int color) {
        int a = color >> 24 & 0xFF;
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;

        return (a == 0 || ((r == g && r == b) && (r < WHITE + fuzziness || r > BLACK - fuzziness)));
    }

    //Grayscaling with sensitivity to white on transparent background and black on transparent background.
    private void grayScale() {
        BufferedImage img = grayify(WHITE);
        if (min(img) == max(img)) // Does it lack contrast?
            img = grayify(BLACK);
        this.image = img;
    }

    private BufferedImage grayify(int transparent) {
        BufferedImage temp = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgba = image.getRGB(x, y);
                int b = rgba & 0xFF;
                int g = rgba >> 8 & 0xFF;
                int r = rgba >> 16 & 0xFF;
                int a = rgba >> 24 & 0xFF;

                int o = ((r * 76 + g * 150 + b * 30) >> 8);
                if (a == 0) {
                    o = transparent;
                }
                rgba = (o + (o << 8) + (o << 16) + (WHITE << 24));
                temp.setRGB(x, y, rgba);
            }
        }
        return temp;
    }

    private void normalize() {
        int min = min(image);
        int max = max(image);
        if (!(min == BLACK && max == WHITE) && max > 0) { //Does it need normalization?
            Raster data = image.getData();
            for (int y = 0; y < data.getHeight(); y++) {
                for (int x = 0; x < data.getWidth(); x++) {
                    int b = (data.getSample(x, y, 0) - min) * WHITE / (max - min);
                    int rgba = (b + (b << 8) + (b << 16) + (WHITE << 24));
                    image.setRGB(x, y, rgba);
                }
            }
        }
    }

    private int min(BufferedImage image) {
        int min = WHITE;
        Raster data = image.getData();
        for (int y = 0; y < data.getHeight(); y++) {
            for (int x = 0; x < data.getWidth(); x++) {
                int b = data.getSample(x, y, 0);
                if (b < min) min = b;
                if (min == BLACK) return min;
            }
        }
        return min;
    }

    private int max(BufferedImage image) {
        int max = BLACK;
        Raster data = image.getData();
        for (int y = 0; y < data.getHeight(); y++) {
            for (int x = 0; x < data.getWidth(); x++) {
                int b = data.getSample(x, y, 0);
                if (b > max) max = b;
                if (max == WHITE) return max;
            }
        }
        return max;
    }

    private void resize(int width, int height) {
        BufferedImage temp = new BufferedImage(width, height, image.getType());
        Graphics2D graphics2D = temp.createGraphics();
        graphics2D.drawImage(image, 0, 0, width, height, null);
        graphics2D.dispose();
        image = temp;
    }

    private void blur() {
        float n = 1f / 9f;
        int size = 1;
        Kernel kernel = new Kernel(size, size, new float[]{n, n, n, n, n, n, n, n, n});
        BufferedImageOp op = new ConvolveOp(kernel);
        image = op.filter(image, null);
    }

    private void comparePixels() {
        changes = new boolean[image.getHeight() * (image.getWidth() - 1)];
        larger = new boolean[image.getHeight() * (image.getWidth() - 1)];
        int i = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (x != image.getWidth() - 1) {
                    int b1 = image.getRGB(x, y) & 0xFF;
                    int b2 = image.getRGB(x + 1, y) & 0xFF;

                    changes[i] = b1 < b2 - fuzziness || b1 > b2 + fuzziness;
                    larger[i] = b1 > b2;
                    i++;
                }
            }
        }
    }

    public void save(String path) throws IOException {
        ImageIO.write(image, "png", new File(path));
    }

}
