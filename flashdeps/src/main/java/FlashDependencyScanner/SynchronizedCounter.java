package FlashDependencyScanner;

public class SynchronizedCounter {
    private int count;
    
    // Construct this with some initial count.
    public SynchronizedCounter(int initialCount) {
        count = initialCount;
    }

    /**
     * Get the count in a thread-safe manner.
     * @return The current count of this object.
     */
    public synchronized int getCount() {
        return count;
    }

    /**
     * Increment the count by one in a thread-safe manner.
     */
    public synchronized void increment() {
        count += 1;
    }
}
