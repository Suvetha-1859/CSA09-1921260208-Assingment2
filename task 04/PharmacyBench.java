import java.util.*;

public class PharmacyBench {
    static long sink;
    static final int RUNS = 5, LOOKUPS = 100_000;

    record Medicine(String code, int stock) {}

    interface Op { void run(); }
    static double avgMs(Op op) {
        op.run();
        double t = 0;
        for (int i = 0; i < RUNS; i++) { long t0 = System.nanoTime(); op.run(); t += (System.nanoTime() - t0) / 1e6; }
        return t / RUNS;
    }

    static List<String> codes(int n) {
        List<String> c = new ArrayList<>();
        for (int i = 0; i < n; i++) c.add(String.format("MED%07d", i * 7 + 13));
        Collections.shuffle(c, new Random(42));
        return c;
    }

    static Map<String, Medicine> fill(Map<String, Medicine> m, List<String> codes) {
        for (String c : codes) m.put(c, new Medicine(c, c.length()));
        return m;
    }

    public static void main(String[] args) {
        System.out.println("Java " + System.getProperty("java.version"));
        System.out.println("Acceptability criterion: average lookup < 1 microsecond\n");

        for (int n : new int[]{1_000, 20_000, 200_000}) {
            List<String> codes = codes(n);
            Random rnd = new Random(7);
            String[] keys = new String[LOOKUPS];
            for (int i = 0; i < LOOKUPS; i++) keys[i] = codes.get(rnd.nextInt(n));

            Map<String, Medicine> hm = fill(new HashMap<>(), codes);
            Map<String, Medicine> tm = fill(new TreeMap<>(), codes);
            Map<String, Medicine> lm = fill(new LinkedHashMap<>(), codes);
            Map<String, Medicine> ht = fill(new Hashtable<>(), codes);

            System.out.println("=== n = " + n + " medicines ===");
            System.out.printf("%-14s %14s %16s %14s %22s%n", "Map", "put all (ms)", "100k lookups ms", "ns/lookup", "code-order report ms");
            Object[][] maps = {{"HashMap", hm}, {"TreeMap", tm}, {"LinkedHashMap", lm}, {"Hashtable", ht}};
            for (Object[] e : maps) {
                String name = (String) e[0];
                @SuppressWarnings("unchecked") Map<String, Medicine> m = (Map<String, Medicine>) e[1];
                double put = avgMs(() -> {
                    Map<String, Medicine> x = name.equals("HashMap") ? new HashMap<>() : name.equals("TreeMap") ? new TreeMap<>()
                            : name.equals("Hashtable") ? new Hashtable<>() : new LinkedHashMap<>();
                    fill(x, codes); sink += x.size(); });
                double look = avgMs(() -> { long s = 0; for (String k : keys) s += m.get(k).stock(); sink += s; });
                double rep = avgMs(() -> {
                    StringBuilder sb = new StringBuilder();
                    if (m instanceof TreeMap) { for (Medicine x : m.values()) sb.append(x.code()).append(' '); }
                    else { List<String> ks = new ArrayList<>(m.keySet()); Collections.sort(ks); for (String k : ks) sb.append(m.get(k).code()).append(' '); }
                    sink += sb.length();
                });
                System.out.printf("%-14s %14.2f %16.2f %14.1f %22.2f%n", name, put, look, look * 1e6 / LOOKUPS, rep);
            }
            double repLhm = avgMs(() -> { StringBuilder sb = new StringBuilder(); for (Medicine x : lm.values()) sb.append(x.code()).append(' '); sink += sb.length(); });
            double repHm = avgMs(() -> { StringBuilder sb = new StringBuilder(); for (Medicine x : hm.values()) sb.append(x.code()).append(' '); sink += sb.length(); });
            boolean lhmInsertion = new ArrayList<>(lm.keySet()).equals(codes);
            boolean hmInsertion = new ArrayList<>(hm.keySet()).equals(codes);
            List<String> sorted = new ArrayList<>(codes); Collections.sort(sorted);
            boolean tmSorted = new ArrayList<>(tm.keySet()).equals(sorted);
            boolean hmSorted = new ArrayList<>(hm.keySet()).equals(sorted);
            System.out.printf("Insertion-order report: LinkedHashMap %.2f ms (correct: %b) | HashMap %.2f ms (correct: %b)%n", repLhm, lhmInsertion, repHm, hmInsertion);
            System.out.printf("Code-order natural iteration: TreeMap correct=%b | HashMap correct=%b%n%n", tmSorted, hmSorted);
        }

        System.out.println("=== Alternative: sorted array + Arrays.binarySearch ===");
        for (int n : new int[]{1_000, 20_000, 200_000}) {
            List<String> codes = codes(n);
            String[] sorted = codes.toArray(new String[0]); Arrays.sort(sorted);
            Random rnd = new Random(7); String[] keys = new String[LOOKUPS];
            for (int i = 0; i < LOOKUPS; i++) keys[i] = codes.get(rnd.nextInt(n));
            double look = avgMs(() -> { long s = 0; for (String k : keys) s += Arrays.binarySearch(sorted, k); sink += s; });
            double ins = avgMs(() -> {
                String[] arr = Arrays.copyOf(sorted, n + 2000); int size = n;
                for (int i = 0; i < 2000; i++) {
                    String c = String.format("MED%07d", i * 7 + 14);
                    int pos = Arrays.binarySearch(arr, 0, size, c); if (pos < 0) pos = -pos - 1;
                    System.arraycopy(arr, pos, arr, pos + 1, size - pos); arr[pos] = c; size++;
                }
                sink += size;
            });
            System.out.printf("n=%-8d 100k lookups = %8.2f ms (%.1f ns each) | 2,000 sorted inserts = %8.2f ms%n", n, look, look * 1e6 / LOOKUPS, ins);
        }
        System.out.println("(sink " + sink + ")");
    }
}
