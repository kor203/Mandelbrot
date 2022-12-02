import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FractalGenerator002_1 {
    public static void singlePixelGenerator (int xpx, int ypx, double x, double y, int max_iterations, BufferedImage img){
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
        img.setRGB(xpx, ypx, rgb);
    }
    public static class positionOfPixel{
        public int xpx;
        public int ypx;
        public double x;
        public double y;

        public positionOfPixel(int xpx, int ypx, double x, double y){
            this.xpx = xpx;
            this.ypx = ypx;
            this.x = x;
            this.y = y;
        }
    }
    public static class poolGenerator implements Runnable {
        BufferedImage img;
        positionOfPixel[] pos;
        int max_iterations;
        CountDownLatch cl;

        public poolGenerator(positionOfPixel[] pos, int max_iterations, BufferedImage img, CountDownLatch cl){
            this.img = img;
            this.pos = pos;
            this.max_iterations = max_iterations;
            this.cl = cl;
        }

        @Override
        public void run() {
            int iterator = pos.length;
            for (int i = 0; i < iterator; i++) {
                singlePixelGenerator(pos[i].xpx, pos[i].ypx, pos[i].x, pos[i].y, max_iterations, img);
            }
            cl.countDown();
        }
    }
    public static positionOfPixel[][] splitByNumber(int xpx, int ypx, double minx, double maxx, double miny, double maxy, int numberOfThreads){
        long max_pixel = xpx*ypx;
        int pixels_per_thread = (int) Math.ceil((double)max_pixel/numberOfThreads);
        positionOfPixel[][] out = new positionOfPixel[numberOfThreads][];
        for (int i = 0; i < numberOfThreads-1; i++) {
            out[i] = new positionOfPixel[pixels_per_thread];
        }
        out[numberOfThreads-1] = new positionOfPixel[(int) (max_pixel-(pixels_per_thread*(numberOfThreads-1)))];
        int ctr = 0;
        int tctr = 0;
        double x;
        double y = miny;
        double dx = (maxx - minx) / (xpx - 1);
        double dy = (maxy - miny) / (ypx - 1);
        for (int i = 0; i < ypx; i++) {
            x = minx;
            for (int j = 0; j < xpx; j++) {
                out[tctr][ctr] = new positionOfPixel(j, i, x, y);
                ctr++;
                if (ctr >= pixels_per_thread) {
                    ctr = 0;
                    tctr++;
                }
                x += dx;
            }
            y += dy;
        }
        return out;
    }
    public static BufferedImage generator(int xpx, int ypx, double minx, double maxx, double miny, double maxy, int max_iterations, ExecutorService ex) throws IOException {
        BufferedImage img = new BufferedImage(xpx, ypx, BufferedImage.TYPE_INT_RGB);
        CountDownLatch cl = new CountDownLatch(Runtime.getRuntime().availableProcessors());
        positionOfPixel[][] splits = splitByNumber(xpx, ypx, minx, maxx, miny, maxy, Runtime.getRuntime().availableProcessors());
        for (int i = 0; i < splits.length; i++) {
            ex.submit(new poolGenerator(splits[i], max_iterations, img, cl));
        }
        try {
            cl.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
        ExecutorService ex = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        long start = System.nanoTime();
        for (int i = 0; i < how_many; i++){
            generator(xpx, ypx, minx, maxx, miny, maxy, max_iterations, ex);
        }
        long stop = System.nanoTime();

        ex.shutdown();
        try {
            ex.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return Double.valueOf(stop - start)/how_many/1e9;
    }

    public static void main(String[] args) throws IOException {
        /*int how_many = 20;

        BufferedWriter output_file = new BufferedWriter(new FileWriter("times_002_1.txt"));
        int[] sizes = {32, 64, 128, 256, 512, 1024, 2048, 4096, 8192};
        for (int i = 0; i < sizes.length; i++){
            output_file.write(sizes[i]*sizes[i] + " " + getAverageTimeOfGeneration(sizes[i], sizes[i], how_many) + "\n");
            System.out.println(sizes[i]);
        }
        output_file.close();*/
        ExecutorService ex = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        drawFractal(generator(1024, 1024, -2.1, 0.6, -1.2, 1.2, 200, ex));
        ex.shutdown();
    }
}