import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;

public final class PerceptualHash {
    private static final int SIZE = 32;
    private static final int SMALLER_SIZE = 8;

    private PerceptualHash() {
    }

    public static long computeHash(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Unsupported image format: " + file);
        }
        return computeHash(image);
    }

    public static long computeHash(BufferedImage image) {
        BufferedImage resized = resizeToGrayscale(image, SIZE, SIZE);
        double[][] pixels = extractPixels(resized);
        double[][] dct = applyDct(pixels);
        double[] topLeft = new double[SMALLER_SIZE * SMALLER_SIZE];
        int index = 0;
        for (int y = 0; y < SMALLER_SIZE; y++) {
            for (int x = 0; x < SMALLER_SIZE; x++) {
                topLeft[index++] = dct[y][x];
            }
        }
        double median = median(topLeft, 1);
        long hash = 0L;
        for (int i = 0; i < topLeft.length; i++) {
            if (topLeft[i] > median) {
                hash |= (1L << (topLeft.length - 1 - i));
            }
        }
        return hash;
    }

    public static int hammingDistance(long hashA, long hashB) {
        return Long.bitCount(hashA ^ hashB);
    }

    private static BufferedImage resizeToGrayscale(BufferedImage image, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        return resized;
    }

    private static double[][] extractPixels(BufferedImage image) {
        double[][] pixels = new double[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                pixels[y][x] = image.getRaster().getSampleDouble(x, y, 0);
            }
        }
        return pixels;
    }

    private static double[][] applyDct(double[][] pixels) {
        double[][] dct = new double[SIZE][SIZE];
        for (int u = 0; u < SIZE; u++) {
            for (int v = 0; v < SIZE; v++) {
                double sum = 0.0;
                for (int x = 0; x < SIZE; x++) {
                    for (int y = 0; y < SIZE; y++) {
                        sum += pixels[y][x]
                                * Math.cos(((2 * x + 1) * u * Math.PI) / (2.0 * SIZE))
                                * Math.cos(((2 * y + 1) * v * Math.PI) / (2.0 * SIZE));
                    }
                }
                double cu = (u == 0) ? 1.0 / Math.sqrt(2) : 1.0;
                double cv = (v == 0) ? 1.0 / Math.sqrt(2) : 1.0;
                dct[v][u] = 0.25 * cu * cv * sum;
            }
        }
        return dct;
    }

    private static double median(double[] values, int skipCount) {
        double[] copy = Arrays.copyOfRange(values, skipCount, values.length);
        Arrays.sort(copy);
        int mid = copy.length / 2;
        if (copy.length % 2 == 0) {
            return (copy[mid - 1] + copy[mid]) / 2.0;
        }
        return copy[mid];
    }
}
