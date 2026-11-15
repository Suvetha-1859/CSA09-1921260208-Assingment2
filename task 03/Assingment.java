import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PlaylistBench {
    static long sink;
    static final int RUNS = 5;

    interface Op { void run(); }

    static double avgMs(Op op) {
        op.run();
        double total = 0;
        for (int i = 0; i < RUNS; i++) {
            long t0 = System.nanoTime();
            op.run();
            total += (System.nanoTime() - t0) / 1e6;
        }
        return total / RUNS;
    }

    static List<Integer> filled(List<Integer> l, int n) {
        for (int i = 0; i < n; i++) l.add(i);
        return l;
    }

    static void row(String op, int n, double a, double l) {
        System.out.printf("%-26s n=%-8d ArrayList=%10.3f ms   LinkedList=%10.3f ms%n", op, n, a, l);
    }

    public static void main(String[] args) {
        System.out.println("Java " + System.getProperty("java.version"));
        int[] sizes = {10_000, 50_000, 200_000};
        final int OPS = 5_000;

        for (int n : sizes) {
            double a = avgMs(() -> { List<Integer> l = new ArrayList<>(); for (int i = 0; i < n; i++) l.add(i); sink += l.size(); });
            double b = avgMs(() -> { List<Integer> l = new LinkedList<>(); for (int i = 0; i < n; i++) l.add(i); sink += l.size(); });
            row("add at end (n adds)", n, a, b);

            a = avgMs(() -> { List<Integer> l = filled(new ArrayList<>(), n); for (int i = 0; i < OPS; i++) l.add(0, i); sink += l.size(); });
            b = avgMs(() -> { List<Integer> l = filled(new LinkedList<>(), n); for (int i = 0; i < OPS; i++) l.add(0, i); sink += l.size(); });
            row("insert front (" + OPS + ")", n, a, b);

            a = avgMs(() -> { List<Integer> l = filled(new ArrayList<>(), n); for (int i = 0; i < OPS; i++) l.remove(0); sink += l.size(); });
            b = avgMs(() -> { List<Integer> l = filled(new LinkedList<>(), n); for (int i = 0; i < OPS; i++) l.remove(0); sink += l.size(); });
            row("remove front (" + OPS + ")", n, a, b);

            a = avgMs(() -> { List<Integer> l = filled(new ArrayList<>(), n); for (int i = 0; i < OPS; i++) l.add(l.size() / 2, i); sink += l.size(); });
            b = avgMs(() -> { List<Integer> l = filled(new LinkedList<>(), n); for (int i = 0; i < OPS; i++) l.add(l.size() / 2, i); sink += l.size(); });
            row("insert middle (" + OPS + ")", n, a, b);

            a = avgMs(() -> { List<Integer> l = filled(new ArrayList<>(), n); for (int i = 0; i < OPS; i++) l.remove(l.size() / 2); sink += l.size(); });
            b = avgMs(() -> { List<Integer> l = filled(new LinkedList<>(), n); for (int i = 0; i < OPS; i++) l.remove(l.size() / 2); sink += l.size(); });
            row("remove middle (" + OPS + ")", n, a, b);

            List<Integer> al = filled(new ArrayList<>(), n), ll = filled(new LinkedList<>(), n);
            a = avgMs(() -> { long s = 0; for (int i = 0; i < OPS; i++) s += al.get((int) ((i * 2654435761L) % n)); sink += s; });
            b = avgMs(() -> { long s = 0; for (int i = 0; i < OPS; i++) s += ll.get((int) ((i * 2654435761L) % n)); sink += s; });
            row("get k-th (" + OPS + ")", n, a, b);

            a = avgMs(() -> { long s = 0; for (int x : al) s += x; sink += s; });
            b = avgMs(() -> { long s = 0; for (int x : ll) s += x; sink += s; });
            row("iterate all", n, a, b);
            System.out.println();
        }

        System.out.println("=== Changed condition: Up Next queue (addLast, addFirst, removeFirst) ===");
        for (int n : sizes) {
            final int ops = n;
            double a = avgMs(() -> { ArrayList<Integer> l = new ArrayList<>(); for (int i = 0; i < ops; i++) { l.add(i); l.add(0, i); l.remove(0); } sink += l.size(); });
            double b = avgMs(() -> { LinkedList<Integer> l = new LinkedList<>(); for (int i = 0; i < ops; i++) { l.addLast(i); l.addFirst(i); l.removeFirst(); } sink += l.size(); });
            double c = avgMs(() -> { ArrayDeque<Integer> l = new ArrayDeque<>(); for (int i = 0; i < ops; i++) { l.addLast(i); l.addFirst(i); l.removeFirst(); } sink += l.size(); });
            System.out.printf("n=%-8d ArrayList=%10.3f ms  LinkedList=%8.3f ms  ArrayDeque=%8.3f ms%n", n, a, b, c);
        }

        System.out.println("\n=== Plain int[] with manual shifting: insert at front x " + OPS + " ===");
        for (int n : sizes) {
            double t = avgMs(() -> {
                int[] arr = new int[n + OPS]; int size = n;
                for (int i = 0; i < n; i++) arr[i] = i;
                for (int i = 0; i < OPS; i++) { System.arraycopy(arr, 0, arr, 1, size); arr[0] = i; size++; }
                sink += size;
            });
            System.out.printf("n=%-8d int[] manual shift = %10.3f ms%n", n, t);
        }

        System.out.println("\n=== Estimated memory per element (1,000,000 Integers) ===");
        int N = 1_000_000;
        for (String kind : new String[]{"ArrayList", "LinkedList", "ArrayDeque"}) {
            System.gc(); System.gc();
            long b0 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            Object c;
            if (kind.equals("ArrayList")) { ArrayList<Integer> l = new ArrayList<>(N); for (int i = 0; i < N; i++) l.add(i); c = l; }
            else if (kind.equals("LinkedList")) { LinkedList<Integer> l = new LinkedList<>(); for (int i = 0; i < N; i++) l.add(i); c = l; }
            else { ArrayDeque<Integer> l = new ArrayDeque<>(N); for (int i = 0; i < N; i++) l.add(i); c = l; }
            System.gc(); System.gc();
            long b1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            System.out.printf("%-11s ~%d bytes/element%n", kind, (b1 - b0) / N);
            sink += c.hashCode() & 1;
        }
        System.out.println("(sink " + sink + ")");
    }
}
