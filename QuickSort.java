import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;

public class QuickSort {
    public static class QuickSortIteration extends RecursiveTask<Void> { //https://www.geeksforgeeks.org/quick-sort/
        double[] arr;
        int start;
        int end;

        public QuickSortIteration(double[] arr, int start, int end) {
            this.arr = arr;
            this.start = start;
            this.end = end;
        }
        @Override
        protected Void compute() {
            if (start < end) {
                double element = arr[end];
                double tmp;
                int i = start;
                for (int j = start; j < end; j++) {
                    if (arr[j] < element) {
                        tmp = arr[j];
                        arr[j] = arr[i];
                        arr[i] = tmp;
                        i++;
                    }
                }
                tmp = arr[i];
                arr[i] = element;
                arr[end] = tmp;

                QuickSortIteration left = new QuickSortIteration(arr, start, i-1);
                QuickSortIteration right = new QuickSortIteration(arr, i+1, end);

                left.fork();
                right.compute();
                left.join();
            }
            return null;
        }
    }

    public static void QuickSortSequential(double[] arr, int start, int end){
        if (start < end) {
            double element = arr[end];
            double tmp;
            int i = start;
            for (int j = start; j < end; j++) {
                if (arr[j] < element) {
                    tmp = arr[j];
                    arr[j] = arr[i];
                    arr[i] = tmp;
                    i++;
                }
            }
            tmp = arr[i];
            arr[i] = element;
            arr[end] = tmp;

            QuickSortSequential(arr, start, i-1);
            QuickSortSequential(arr, i+1, end);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ForkJoinPool fjp = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        ForkJoinPool single_t = new ForkJoinPool(1);
        Random random = new Random();
        int[] lengths = {10, 100, 1000, (int) 1e4, (int) 1e5, (int) 1e6, (int) 1e7, (int)1e8};
        int points_taken = lengths.length;
        int repeats = 20;
        double[] fjp_times = new double[points_taken];
        double[] single_fjp_times = new double[points_taken];
        double[] single_times = new double[points_taken];
        long startTime;
        long stopTime;
        BufferedWriter output_file = new BufferedWriter(new FileWriter("times_003.txt"));

        for (int r = 0; r < repeats; r++) {
            for (int i = 0; i < points_taken; i++) {
                double[] to_sort = new double[lengths[i]];
                for (int j = 0; j < lengths[i]; j++) {
                    to_sort[j] = random.nextDouble();
                }
                double[] to_sort2 = to_sort.clone();
                double[] to_sort3 = to_sort.clone();

                startTime = System.nanoTime();
                fjp.invoke(new QuickSortIteration(to_sort, 0, lengths[i] - 1));
                stopTime = System.nanoTime();
                fjp_times[i] += (stopTime - startTime) / 1e9;

                startTime = System.nanoTime();
                single_t.invoke(new QuickSortIteration(to_sort2, 0, lengths[i] - 1));
                stopTime = System.nanoTime();
                single_fjp_times[i] += (stopTime - startTime) / 1e9;

                startTime = System.nanoTime();
                QuickSortSequential(to_sort3, 0, lengths[i] - 1);
                stopTime = System.nanoTime();
                single_times[i] += (stopTime - startTime) / 1e9;
            }
            System.out.println("Repeat no. " + (r + 1) + " done.");
        }
        for (int i = 0; i < points_taken; i++) {
            fjp_times[i] /= repeats;
            single_fjp_times[i] /= repeats;
            single_times[i] /= repeats;
        }
        for (int i = 0; i < points_taken; i++) {
            output_file.write(lengths[i] + " " + single_times[i] + " " + single_fjp_times[i] +  " " +  fjp_times[i] + "\n");
        }
        output_file.close();

        fjp.shutdown();
        single_t.shutdown();

        fjp.awaitTermination(1, TimeUnit.DAYS);
        single_t.awaitTermination(1, TimeUnit.DAYS);
    }
}
