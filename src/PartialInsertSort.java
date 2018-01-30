import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;

public class PartialInsertSort {

    private static int n;
    private static int k;

    private static int runs = 7;
    private static int medianIndex = 4;
    private static double[] arrTiming = new double[runs];
    private static double[] seqTiming = new double[runs];
    private static double[] parTiming = new double[runs];

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

        // Do and time parallell version.
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
        insertSort(nums, 0, k - 1);

        int cores = Runtime.getRuntime().availableProcessors();
        Thread[] threads = new Thread[cores];
        Lock lock = new ReentrantLock();

        int segmentSize = (nums.length - k) / threads.length;
        for (int i = 0; i < cores; i++) {
            int start = k + (segmentSize * i);
            int stop = k + segmentSize * (i + 1);
            stop = (i == (cores - 1)) ? n : stop;

            threads[i] = new Thread(new Worker(nums, start, stop, lock));
            threads[i].start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Worker class for the parallel solution.
     */
    class Worker implements Runnable {
        int[] nums;
        int start;
        int stop;
        Lock lock;

        /**
         * Constructor
         * @param nums Numbers array to sort.
         * @param start Start index.
         * @param stop Stop index.
         * @param lock A Lock object for nums.
         */
        Worker(int[] nums, int start, int stop, Lock lock) {
            this.nums = nums;
            this.start = start;
            this.stop = stop;
            this.lock = lock;
        }

        /**
         * Run the thing.
         */
        @Override
        public void run() {
            int tempInt;
            for (int i = start; i < stop; i++) {
                if (nums[k - 1] < nums[i]) {
                    try {
                        lock.lock();
                        if (nums[k - 1] < nums[i]) { // Compare twice, since the first is not synced.
                            tempInt = nums[i];
                            nums[i] = nums[k - 1];
                            nums[k - 1] = tempInt;

                            insertSortLast(nums, 0, k - 1);
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            }
        }
    }

    /**
     * Perform a sequential sorting of the k highest elements in nums.
     * @param nums The array to look at.
     */
    private void seq(int[] nums) {
        insertSort(nums, 0, k - 1);

        int tempInt;
        for (int i = k; i < nums.length; i++) {
            if (nums[k - 1] < nums[i]) {
                tempInt = nums[i];
                nums[i] = nums[k - 1];
                nums[k - 1] = tempInt;

                insertSortLast(nums, 0, k - 1);
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
