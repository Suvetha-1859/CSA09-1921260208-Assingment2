import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class ContractTests {

    static class BadStudent {
        String regNo; String name;
        BadStudent(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof BadStudent)) return false;
            return regNo.equals(((BadStudent) o).regNo);
        }
        @Override public String toString() { return regNo + " " + name; }
    }

    static class GoodStudent {
        private final String regNo; private final String name;
        GoodStudent(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GoodStudent)) return false;
            return regNo.equals(((GoodStudent) o).regNo);
        }
        @Override public int hashCode() { return Objects.hash(regNo); }
        @Override public String toString() { return regNo + " " + name; }
    }

    static class MutableStudent {
        String regNo; final String name;
        MutableStudent(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) {
            return o instanceof MutableStudent && regNo.equals(((MutableStudent) o).regNo);
        }
        @Override public int hashCode() { return regNo.hashCode(); }
    }

    static void report(String test, int predicted, int observed, String why) {
        System.out.printf("%-52s predicted=%d observed=%d  %s%n   reason: %s%n", test, predicted, observed,
                predicted == observed ? "[MATCH]" : "[DIFFERENT]", why);
    }

    public static void main(String[] args) {
        BadStudent arun = new BadStudent("192311001", "Arun");
        Set<BadStudent> t1 = new HashSet<>();
        t1.add(arun); t1.add(new BadStudent("192311002", "Divya")); t1.add(arun);
        report("T1 BadStudent, same object re-added", 2, t1.size(),
                "same reference -> same identity hashCode -> same bucket, == true");

        Set<BadStudent> t2 = new HashSet<>();
        t2.add(new BadStudent("192311001", "Arun"));
        t2.add(new BadStudent("192311002", "Divya"));
        t2.add(new BadStudent("192311001", "Arun"));
        report("T2 BadStudent, two equal objects", 3, t2.size(),
                "identity hashCodes differ -> different buckets -> equals() never called");
        System.out.println("   contents: " + t2);

        Set<GoodStudent> t2b = new HashSet<>();
        t2b.add(new GoodStudent("192311001", "Arun"));
        t2b.add(new GoodStudent("192311002", "Divya"));
        t2b.add(new GoodStudent("192311001", "Arun"));
        report("T2b GoodStudent, same scans", 2, t2b.size(), "equal objects share a hashCode, so equals() removes the duplicate");

        MutableStudent m = new MutableStudent("A100", "Kavi");
        Set<MutableStudent> t3 = new HashSet<>();
        t3.add(m);
        m.regNo = "B200";
        boolean found = t3.contains(m);
        t3.add(new MutableStudent("B200", "Kavi"));
        report("T3 mutate hash field after insert", 2, t3.size(),
                "object stays in bucket of OLD hash; contains(m)=" + found);

        Set<GoodStudent> t4 = new TreeSet<>(Comparator.comparing((GoodStudent s) -> s.name));
        t4.add(new GoodStudent("192311001", "Arun"));
        t4.add(new GoodStudent("192311002", "Arun"));
        report("T4 TreeSet comparing by name only", 1, t4.size(),
                "TreeSet uses the comparator, not equals(): compare()==0 means duplicate");

        Map<String, BadStudent> map = new HashMap<>();
        map.putIfAbsent("192311001", new BadStudent("192311001", "Arun"));
        map.putIfAbsent("192311002", new BadStudent("192311002", "Divya"));
        map.putIfAbsent("192311001", new BadStudent("192311001", "Arun"));
        System.out.println("\nAlt: HashMap keyed by regNo -> size " + map.size());
        Set<BadStudent> ts = new TreeSet<>(Comparator.comparing((BadStudent s) -> s.regNo));
        ts.add(new BadStudent("192311001", "Arun")); ts.add(new BadStudent("192311002", "Divya")); ts.add(new BadStudent("192311001", "Arun"));
        System.out.println("Alt: TreeSet with Comparator on regNo -> size " + ts.size());

        class ConstHash { final int id; ConstHash(int i) { id = i; }
            @Override public boolean equals(Object o) { return o instanceof ConstHash && ((ConstHash) o).id == id; }
            @Override public int hashCode() { return 42; } }
        int n = 20000;
        long t0 = System.nanoTime(); Set<ConstHash> ch = new HashSet<>(); for (int i = 0; i < n; i++) ch.add(new ConstHash(i));
        double bad = (System.nanoTime() - t0) / 1e6;
        t0 = System.nanoTime(); Set<GoodStudent> gh = new HashSet<>(); for (int i = 0; i < n; i++) gh.add(new GoodStudent("R" + i, "x"));
        double good = (System.nanoTime() - t0) / 1e6;
        System.out.printf("%nConstant hashCode 42: %d inserts %.1f ms   vs proper hash: %.1f ms%n", n, bad, good);
    }
}
