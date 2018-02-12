import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.Random;

public class PartialInsertSort {

    private static int n;
    private static int k;

    private static final int runs = 7;
    private static final int medianIndex = 4;
    private static double[] arrTiming = new double[runs];
    private static double[] seqTiming = new double[runs];
    private static double[] parTiming = new double[runs];

    private Thread[] threads;

    /**
     * Main. Ofc.
     * @param args Cmd/terminal args.
     */
    public static void main(String[] args) {
        // Check input
        if (args.length != 2) {
            System.out.println("Use: java PartialInsertSort N(number of values) K(size of portion to sort)");
            return;
        }
        try {
            n = Integer.parseInt(args[0]);
            k = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("N and K have to be valid integers.");
            return;
        }
        if (n < k) {
            System.out.println("K must be equal to or greater than K.");
            return;
        }

        // Run
        for (int i = 0; i < runs; i++) {
            System.out.println("\nSTARTING RUN " + i + ":");
            new PartialInsertSort(i);
        }

        System.out.println("\nTIMING CALCULATIONS:");

        Arrays.sort(arrTiming);
        Arrays.sort(seqTiming);
        Arrays.sort(parTiming);

        System.out.printf("Arrays.sort median : %.3f\n", arrTiming[medianIndex]);
        System.out.printf("Sequential median  : %.3f\n", seqTiming[medianIndex]);
        System.out.printf(
                "Parallel median    : %.3f    Speedup from sequential: %.3f\n",
                parTiming[medianIndex], (seqTiming[medianIndex] / parTiming[medianIndex])
        );

        System.out.println("\nN: " + n + "\tK: " + k);

    }

    /**
     * Create a PartialInsertSort object and perform the sorting timings and tests.
     * @param runNum The run count, used to store timings.
     */
    private PartialInsertSort(int runNum) {
        Random rng = new Random();
        int[] nums = new int[n];
        long startTime;

        // Generate numbers
        for (int i = 0; i < n; i++) {
            nums[i] = rng.nextInt(n);
        }

        // Make copies of the array to ensure they are sorting the same numbers, for better timing.
        int[] seqNums = nums.clone();
        int[] parNums = nums.clone();
        int[] sortedNums = nums.clone();

        // Do and time sequential version.
        System.out.println("Starting arrays.sort");
        startTime = System.nanoTime();
        Arrays.sort(sortedNums);
        arrTiming[runNum] = (System.nanoTime() - startTime) / 1000000.0;
        System.out.println("Arrays.sort time: " + arrTiming[runNum] + "ms.");

        // Do and time sequential version.
        System.out.println("Starting sequential");
        startTime = System.nanoTime();
        seq(seqNums);
        seqTiming[runNum] = (System.nanoTime() - startTime) / 1000000.0;
        System.out.println("Sequential time: " + seqTiming[runNum] + "ms. Checking for mismatches..");
        checkSorting(seqNums, sortedNums, 0, k);

        // Do and time parallel version.
        System.out.println("Starting Parallel");
        startTime = System.nanoTime();
        par(parNums);
        parTiming[runNum] = (System.nanoTime() - startTime) / 1000000.0;
        System.out.println("Parallel time: " + parTiming[runNum] + "ms. Checking for mismatches..");
        checkSorting(parNums, sortedNums, 0, k);
    }

    /**
     * Perform a parallelized sorting of the k highest elements in nums.
     * @param nums The array to look at.
     */
    private void par(int[] nums) {
        int cores = Runtime.getRuntime().availableProcessors();
        threads = new Thread[cores];
        CyclicBarrier cb = new CyclicBarrier(threads.length + 1);

        int segmentSize = nums.length / threads.length;
        for (int i = 0; i < cores; i++) {
            int start = segmentSize * i;
            int stop = segmentSize * (i + 1);
            stop = (i == (cores - 1)) ? n : stop;

            threads[i] = new Thread(new Worker(i, nums, start, stop, cb));
            threads[i].start();
        }

        // For sync
        try {
            cb.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }

        // For result
        try {
            cb.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

    /**
     * Worker class for the parallel solution.
     */
    class Worker implements Runnable {
        int id;
        int[] nums;
        int start;
        int stop;
        CyclicBarrier cb;

        /**
         * Constructor
         * @param nums Numbers array to sort.
         * @param start Start index.
         * @param stop Stop index.
         * @param cb A CyclicBarrier object.
         */
        Worker(int id, int[] nums, int start, int stop, CyclicBarrier cb) {
            this.id = id;
            this.nums = nums;
            this.start = start;
            this.stop = stop;
            this.cb = cb;
        }

        /**
         * Run the thing.
         */
        @Override
        public void run() {
            sortSegment(nums, start, stop);

            // Sync
            try {
                cb.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }

            // Gather results
            if (id == 0) {
                int tempInt;
                int segmentSize = nums.length / threads.length;
                for (int i = 1; i < threads.length; i++) {
                    int threadStart = segmentSize * i;

                    for (int j = 0; j < k; j++) {
                        if (nums[threadStart + j] > nums[k - 1]) {
                            tempInt = nums[threadStart + j];
                            nums[threadStart + j] = nums[k - 1];
                            nums[k - 1] = tempInt;

                            insertSortLast(nums, 0, k - 1);
                        } else {
                            break;
                        }
                    }
                }
            }

            // Finish
            try {
                cb.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Perform a sequential sorting of the k highest elements in nums.
     * @param nums The array to look at.
     */
    private void seq(int[] nums) {
        sortSegment(nums, 0, nums.length);
    }

    private void sortSegment(int[] nums, int start, int stop) {
        insertSort(nums, start, start + k - 1);

        int tempInt;
        for (int i = start + k; i < stop; i++) {
            if (nums[start + k - 1] < nums[i]) {
                tempInt = nums[i];
                nums[i] = nums[start + k - 1];
                nums[start + k - 1] = tempInt;

                insertSortLast(nums, start, start + k - 1);
            }
        }
    }

    /**
     * Check that the given range of elements is sorted correctly.
     * @param a The array that has been partially sorted.
     * @param sorted An array run through Arrays.sort to sort all numbers.
     * @param left The start key of the partial sorting range to check.
     * @param right The end key of the partial sorting range to check.
     */
    private void checkSorting(int[] a, int[] sorted, int left, int right) {
        for (int i = left; i < right; i++) {
            if (a[i] != sorted[sorted.length - 1 - i]) {
                System.out.println("MISMATCH: " + a[i] + " and " + sorted[sorted.length - 1 - i] + " at " + i + ".");
            }
        }
    }

    /**
     * The insertSort code given as an appendix to the task text.
     * It has been slightly modified.
     * @param a The array to use insertsort on.
     * @param venstre The start key to sort from.
     * @param hoyre The end key to sort until.
     */
    private void insertSort (int[] a, int venstre, int hoyre) {
        int i, t;
        for (int k = venstre; k < hoyre; k++) {
            t = a[k + 1];
            i = k;
            while (i >= venstre && a[i] < t) {
                a[i + 1] = a[i];
                i--;
            }
            a[i + 1] = t;
        }
    }

    /**
     * A simple "insertsort" algorithm that only sorts the last number to the right space without checking the others.
     * @param a The array to sort.
     * @param left The start index.
     * @param right The end index.
     */
    private void insertSortLast(int[] a, int left, int right) {
        int i = right - 1;
        int t = a[right];
        while (i >= left && a[i] < t) {
            a[i + 1] = a[i];
            i--;
        }
        a[i + 1] = t;
    }
}
