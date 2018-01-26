import javax.xml.stream.events.StartDocument;
import java.util.Arrays;
import java.util.Random;

public class PartialInsertSort {

    static int n = 1000;
    static int k = 150;

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

        PartialInsertSort pi = new PartialInsertSort();
    }

    /**
     * Create a PartialInsertSort object and perform the sorting timings and tests.
     */
    public PartialInsertSort() {
        Random rng = new Random();
        int[] nums = new int[n];
        long startTime;
        long endTime;

        // Generate numbers
        for (int i = 0; i < n; i++) {
            nums[i] = rng.nextInt(n * 5);
        }

        // Make copies of the array to ensure they are sorting the same numbers, for better timing.
        int[] seqNums = nums.clone();
        int[] parNums = nums.clone();
        int[] sortedNums = nums.clone();
        Arrays.sort(sortedNums);

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
        System.out.println("Starting Parallell");
        startTime = System.nanoTime();
        par(parNums);
        endTime = System.nanoTime();
        double parTime = (endTime - startTime) / 1000000.0;
        System.out.println("Parallell time: " + parTime + "ms. Checking for mismatches..");
        checkSorting(parNums, sortedNums, 0, k);
        System.out.println("Check finished.");
    }

    /**
     * Perform a parallellized sorting of the k highest elements in nums.
     * @param nums The array to look at.
     */
    public void par(int[] nums) {
        seq(nums);
    }

    /**
     * Perform a sequential sorting of the k highest elements in nums.
     * @param nums The array to look at.
     */
    public void seq(int[] nums) {
        insertSort(nums, 0, k - 1);

        int tempInt;
        for (int i = k; i < nums.length; i++) {
            if (nums[k - 1] < nums[i]) {
                tempInt = nums[i];
                nums[i] = nums[k - 1];
                nums[k - 1] = tempInt;

                insertSort(nums, 0, k - 1);
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
    void checkSorting(int[] a, int[] sorted, int left, int right) {
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
    void insertSort (int[] a, int venstre, int hoyre) {
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
}
