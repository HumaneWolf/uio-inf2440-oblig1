import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

public class PartialInsertSort {

    private static int n;
    private static int k;

    /**
     * Main. Ofc.
     * @param args Cmd/terminal args.
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Use: java PartialInsertSort n(number of values) k(size of portion to sort)");
            return;
        }
        try {
            n = Integer.parseInt(args[0]);
            k = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("n and k have to be valid integers.");
            return;
        }

        new PartialInsertSort();
    }

    /**
     * Create a PartialInsertSort object and perform the sorting timings and tests.
     */
    private PartialInsertSort() {
        Random rng = new Random();
        int[] nums = new int[n];
        long startTime;
        long endTime;

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
        endTime = System.nanoTime();
        double asTime = (endTime - startTime) / 1000000.0;
        System.out.println("Arrays.sort time: " + asTime + "ms.");

        // Do and time sequential version.
        System.out.println("Starting sequential");
        startTime = System.nanoTime();
        seq(seqNums);
        endTime = System.nanoTime();
        double seqTime = (endTime - startTime) / 1000000.0;
        System.out.println("Sequential time: " + seqTime + "ms. Checking for mismatches..");
        checkSorting(seqNums, sortedNums, 0, k);
        System.out.println("Check finished.");

        // Do and time parallell version.
        System.out.println("Starting Parallel");
        startTime = System.nanoTime();
        par(parNums);
        endTime = System.nanoTime();
        double parTime = (endTime - startTime) / 1000000.0;
        System.out.println("Parallel time: " + parTime + "ms. Checking for mismatches..");
        checkSorting(parNums, sortedNums, 0, k);
        System.out.println("Check finished.");
    }

    /**
     * Perform a parallelized sorting of the k highest elements in nums.
     * @param nums The array to look at.
     */
    private void par(int[] nums) {
        insertSort(nums, 0, k - 1);

        int cores = Runtime.getRuntime().availableProcessors();
        ArrayList<LinkedList<Integer>> largerNums = new ArrayList<LinkedList<Integer>>(cores);
        Thread[] threads = new Thread[cores];

        int segmentSize = (nums.length - k) / threads.length;
        for (int i = 0; i < cores; i++) {
            int start = k + (segmentSize * i);
            int stop = k + segmentSize * (i + 1);
            stop = (i == (cores - 1)) ? n : stop;

            largerNums.add(i, new LinkedList<>());

            threads[i] = new Thread(new Worker(nums, largerNums.get(i), start, stop));
            threads[i].start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int tempInt;
        for (LinkedList<Integer> l : largerNums) {
            for (int i : l) {
                if (nums[i] > nums[k - 1]) {
                    tempInt = nums[i];
                    nums[i] = nums[k - 1];
                    nums[k - 1] = tempInt;
                    insertSortLast(nums, 0, k - 1);
                }
            }
        }
    }

    /**
     * Worker class for the parallel solution.
     */
    class Worker implements Runnable {
        int[] nums;
        LinkedList<Integer> largerNums;
        int start;
        int stop;

        /**
         * Constructor
         * @param nums Numbers array to sort.
         * @param largerNums Linked list to store the indexes of potentially larger numbers in.
         * @param start Start index.
         * @param stop Stop index.
         */
        Worker(int[] nums, LinkedList<Integer> largerNums, int start, int stop) {
            this.nums = nums;
            this.largerNums = largerNums;
            this.start = start;
            this.stop = stop;
        }

        /**
         * Run the thing.
         */
        @Override
        public void run() {
            for (int i = start; i < stop; i++) {
                if (nums[i] > nums[k - 1]) {
                    largerNums.add(i);
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
