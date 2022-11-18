import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Math;

public class FractalGenerator {
    public static BufferedImage generator(int xpx, int ypx, double minx, double maxx, double miny, double maxy, int max_iterations) throws IOException {
        BufferedImage img = new BufferedImage(xpx, ypx, BufferedImage.TYPE_INT_RGB);
        //int[] array = new int [xpx*ypx];
        double x;
        double y = miny;
        double dx = (maxx - minx) / (xpx - 1);
        double dy = (maxy - miny) / (ypx - 1);
        for (int i = 0; i < ypx; i++) {
            x = minx;
            for (int j = 0; j < xpx; j++) {
                double localx = x;
                double localy = y;
                int iteration = 0;
                for (; iteration < max_iterations; iteration++) {
                    double tmp = localx;
                    localx = localx * localx - localy * localy + x;
                    localy = 2 * tmp * localy + y;
                    if (Math.sqrt(localx * localx + localy * localy) > 2) {
                        break;
                    }
                }
                //wizualizacja wzorowana na: https://stackoverflow.com/questions/16500656/which-color-gradient-is-used-to-color-mandelbrot-in-wikipedia
                int rgb;
                double prop = Double.valueOf(iteration) / max_iterations;
                if (prop < 0.5) {
                    rgb = 0;  //red
                    rgb = (rgb << 8) + (int) (prop * 255); //green
                    rgb = (rgb << 8) + 0; //blue
                } else if (prop == 1) {
                    rgb = 0;
                } else {
                    rgb = (int) (prop * 255);  //red
                    rgb = (rgb << 8) + 255; //green
                    rgb = (rgb << 8) + (int) (prop * 255); //blue
                }
                img.setRGB(j, i, rgb);
                x += dx;
            }
            y += dy;
        }
        return img;
    }

    public static void drawFractal(BufferedImage img) throws IOException {
        File outputFile = new File("output.bmp");
        ImageIO.write(img, "bmp", outputFile);
    }

    public static double getAverageTimeOfGeneration(int xpx, int ypx, int how_many) throws IOException {
        double minx = -2.1;
        double maxx = 0.6;
        double miny = -1.2;
        double maxy = 1.2;
        int max_iterations = 200;

        long start = System.nanoTime();
        for (int i = 0; i < how_many; i++){
            generator(xpx, ypx, minx, maxx, miny, maxy, max_iterations);
        }
        long stop = System.nanoTime();
        return Double.valueOf(stop - start)/how_many/1e9;
    }

    public static void main(String[] args) throws IOException {
        int how_many = 20;

        BufferedWriter output_file = new BufferedWriter(new FileWriter("times"));
        int[] sizes = {32, 64, 128, 256, 512, 1024, 2048, 4096, 8192};
        for (int i = 0; i < sizes.length; i++){
            output_file.write(sizes[i]*sizes[i] + " " + getAverageTimeOfGeneration(sizes[i], sizes[i], how_many) + "\n");
            System.out.println(sizes[i]);
        }
        output_file.close();
    }
}