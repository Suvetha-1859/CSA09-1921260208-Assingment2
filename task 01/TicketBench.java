import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CountDownLatch;

public class TicketBench {

    static String[] fields(int n) {
        String[] f = new String[n];
        for (int i = 0; i < n; i++) f[i] = "F" + i + "=value" + i + ";";
        return f;
    }

    static String viaPlus(String[] f) {
        String s = "";
        for (String x : f) s = s + x;
        return s;
    }

    static String viaBuilder(String[] f) {
        StringBuilder sb = new StringBuilder();
        for (String x : f) sb.append(x);
        return sb.toString();
    }

    static String viaBuffer(String[] f) {
        StringBuffer sb = new StringBuffer();
        for (String x : f) sb.append(x);
        return sb.toString();
    }

    static String viaJoin(String[] f) { return String.join("", f); }

    static String viaJoiner(String[] f) {
        StringJoiner j = new StringJoiner("");
        for (String x : f) j.add(x);
        return j.toString();
    }

    interface Builder { String build(String[] f); }

    static long checksum;

    static double timeMs(Builder b, String[] f, int tickets, int reps) {
        double total = 0;
        for (int r = 0; r < reps; r++) {
            long t0 = System.nanoTime();
            for (int t = 0; t < tickets; t++) checksum += b.build(f).length();
            total += (System.nanoTime() - t0) / 1e6;
        }
        return total / reps;
    }

    static long usedMemory() {
        System.gc(); System.gc();
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    static void workload(String name, int nFields, int tickets, int reps) {
        String[] f = fields(nFields);
        System.out.println("\n=== Workload " + name + ": " + tickets + " tickets x " + nFields + " fields (avg of " + reps + " runs) ===");
        for (int i = 0; i < 20000; i++) {
            checksum += viaPlus(fields(10)).length() + viaBuilder(f).length() + viaBuffer(f).length()
                      + viaJoin(f).length() + viaJoiner(f).length();
        }
        System.out.printf("%-14s %12s%n", "Approach", "avg time ms");
        System.out.printf("%-14s %12.4f%n", "String +", timeMs(TicketBench::viaPlus, f, tickets, reps));
        System.out.printf("%-14s %12.4f%n", "StringBuilder", timeMs(TicketBench::viaBuilder, f, tickets, reps));
        System.out.printf("%-14s %12.4f%n", "StringBuffer", timeMs(TicketBench::viaBuffer, f, tickets, reps));
        System.out.printf("%-14s %12.4f%n", "String.join", timeMs(TicketBench::viaJoin, f, tickets, reps));
        System.out.printf("%-14s %12.4f%n", "StringJoiner", timeMs(TicketBench::viaJoiner, f, tickets, reps));
    }

    static void estimates() {
        System.out.println("\n=== Estimated intermediate String objects per ticket ===");
        for (int n : new int[]{10, 50, 2000}) {
            String[] f = fields(n);
            long copied = 0, len = 0;
            for (String x : f) { len += x.length(); copied += len; }
            System.out.printf("fields=%-5d '+' creates %d Strings, copies %d chars | builder: 1 String, ~%d chars%n", n, n, copied, len);
        }
    }

    static void memory() {
        System.out.println("\n=== Memory ===");
        String[] f = fields(2000);
        long before = usedMemory();
        List<String> keep = new ArrayList<>();
        for (int i = 0; i < 200; i++) keep.add(viaBuilder(f));
        long after = usedMemory();
        System.out.printf("200 retained tickets: before=%d KB after=%d KB delta=%d KB%n", before / 1024, after / 1024, (after - before) / 1024);
        keep.clear();
        com.sun.management.ThreadMXBean mx = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        long id = Thread.currentThread().getId();
        long a0 = mx.getThreadAllocatedBytes(id);
        for (int i = 0; i < 20; i++) checksum += viaPlus(f).length();
        long a1 = mx.getThreadAllocatedBytes(id);
        for (int i = 0; i < 20; i++) checksum += viaBuilder(f).length();
        long a2 = mx.getThreadAllocatedBytes(id);
        System.out.printf("Bytes allocated for 20 tickets x 2000 fields: '+' = %,d  StringBuilder = %,d  (ratio %.0fx)%n",
                a1 - a0, a2 - a1, (double) (a1 - a0) / (a2 - a1));
    }

    static void threadSafety() throws Exception {
        final int threads = 8, per = 50000, trials = 5;
        System.out.println("\n=== Shared booking log: " + threads + " threads x " + per + " appends, " + trials + " trials ===");
        for (String kind : new String[]{"StringBuilder", "StringBuffer"}) {
            int bad = 0; double totalMs = 0;
            for (int tr = 0; tr < trials; tr++) {
                final StringBuilder sb = new StringBuilder();
                final StringBuffer sf = new StringBuffer();
                CountDownLatch done = new CountDownLatch(threads);
                long t0 = System.nanoTime();
                for (int t = 0; t < threads; t++) {
                    new Thread(() -> {
                        try {
                            for (int i = 0; i < per; i++) {
                                if (kind.equals("StringBuilder")) sb.append("x"); else sf.append("x");
                            }
                        } catch (Throwable e) { }
                        done.countDown();
                    }).start();
                }
                done.await();
                totalMs += (System.nanoTime() - t0) / 1e6;
                int len = kind.equals("StringBuilder") ? sb.length() : sf.length();
                if (len != threads * per) bad++;
            }
            System.out.printf("%-14s trials with lost updates: %d/%d   avg %.1f ms%n", kind, bad, trials, totalMs / trials);
        }
    }

    public static void main(String[] a) throws Exception {
        System.out.println("Java " + System.getProperty("java.version"));
        workload("A (one short ticket)", 10, 1, 2000);
        workload("A' (20,000 short tickets)", 10, 20000, 7);
        workload("B (10,000 tickets x 50 fields)", 50, 10000, 7);
        workload("C (own: 200 long tickets)", 2000, 200, 5);
        estimates();
        memory();
        threadSafety();
        System.out.println("\n(checksum " + checksum + ")");
    }
}
