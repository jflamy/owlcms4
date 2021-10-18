package app.owlcms.utils;

public class IdUtils {

    /**
     * Compute a unique Id based on current system clock, at nanosecond granularity
     * 
     * Used when it is necessary to create a complex relationship between objects that have
     * not been persisted yet (e.g. new athletes wrt eligible categories and selected category)
     * @return
     */
    public static Long getTimeBasedId() {
        return (System.currentTimeMillis() << 20) | (System.nanoTime() & 0xFFFFFL);
    }

}
