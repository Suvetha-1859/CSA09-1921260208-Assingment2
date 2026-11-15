import java.util.*;

public class TriageStudy {
    record P(String name, int sev, long arr) {}
    static long sink;
    static long now = 0;

    static final Comparator<P> RULE = Comparator.comparingInt(P::sev).thenComparingLong(P::arr);

    public static void main(String[] args) {
        System.out.println("Java " + System.getProperty("java.version") + "\n");

        System.out.println("=== Hand-verified case (8 patients) ===");
        String[] names = {"Anil", "Bala", "Chitra", "Dev", "Esha", "Farid", "Gita", "Hari"};
        int[] sev = {4, 2, 5, 2, 1, 4, 3, 1};
        PriorityQueue<P> pq = new PriorityQueue<>(RULE);
        for (int i = 0; i < 8; i++) pq.add(new P(names[i], sev[i], i));
        System.out.println("Arrival order : Anil(4) Bala(2) Chitra(5) Dev(2) Esha(1) Farid(4) Gita(3) Hari(1)");
        System.out.println("By hand       : Esha(1) Hari(1) Bala(2) Dev(2) Gita(3) Anil(4) Farid(4) Chitra(5)");
        System.out.println("pq.toString() : " + pq + "   <- heap order, NOT treatment order");
        StringBuilder sb = new StringBuilder();
        PriorityQueue<P> copy = new PriorityQueue<>(pq);
        while (!copy.isEmpty()) { P p = copy.poll(); sb.append(p.name()).append('(').append(p.sev()).append(") "); }
        System.out.println("poll() order  : " + sb.toString().trim());
        System.out.println("Matches hand-worked order: " + sb.toString().trim().equals(
                "Esha(1) Hari(1) Bala(2) Dev(2) Gita(3) Anil(4) Farid(4) Chitra(5)"));

        Queue<P> fifo = new ArrayDeque<>();
        for (int i = 0; i < 8; i++) fifo.add(new P(names[i], sev[i], i));
        System.out.println("Plain FIFO    : first served = " + fifo.peek().name() + "(S" + fifo.peek().sev() + ") although Esha(S1) is critical\n");

        System.out.println("=== Growth: n adds then n polls (average of 5 runs) ===");
        System.out.printf("%-10s %14s %14s %14s %14s%n", "n", "add all (ms)", "poll all (ms)", "ns per add", "ns per poll");
        for (int n : new int[]{5_000, 50_000, 500_000, 2_000_000}) {
            Random r = new Random(5);
            int[] sv = new int[n]; for (int i = 0; i < n; i++) sv[i] = 1 + r.nextInt(5);
            double ta = 0, tp = 0;
            for (int run = 0; run < 6; run++) {
                PriorityQueue<P> q = new PriorityQueue<>(RULE);
                long t0 = System.nanoTime();
                for (int i = 0; i < n; i++) q.add(new P("p", sv[i], i));
                double a = (System.nanoTime() - t0) / 1e6;
                t0 = System.nanoTime();
                while (!q.isEmpty()) sink += q.poll().arr();
                double p = (System.nanoTime() - t0) / 1e6;
                if (run > 0) { ta += a; tp += p; }
            }
            System.out.printf("%-10d %14.2f %14.2f %14.1f %14.1f%n", n, ta / 5, tp / 5, ta / 5 * 1e6 / n, tp / 5 * 1e6 / n);
        }

        System.out.println("\n=== Starvation: one S5 patient vs a steady stream of S1-S4 arrivals ===");
        for (boolean aging : new boolean[]{false, true}) {
            Random r = new Random(9);
            final int AGE_STEP = 50;
            Comparator<P> cmp = aging
                    ? Comparator.comparingInt((P p) -> Math.max(1, p.sev() - (int) ((TriageStudy.now - p.arr()) / AGE_STEP)))
                                .thenComparingLong(P::arr)
                    : RULE;
            List<P> waiting = new ArrayList<>();
            waiting.add(new P("Old-S5", 5, 0));
            long servedAt = -1;
            for (long clock = 1; clock <= 2000 && servedAt < 0; clock++) {
                now = clock;
                for (int k = 0; k < 3; k++) waiting.add(new P("n" + clock + "_" + k, 1 + r.nextInt(4), clock));
                PriorityQueue<P> q = new PriorityQueue<>(cmp); q.addAll(waiting);
                for (int k = 0; k < 3; k++) { P p = q.poll(); waiting.remove(p); if (p.name().equals("Old-S5")) servedAt = clock; }
            }
            System.out.printf("%-22s Old-S5 patient %s%n", aging ? "With aging (50 ticks):" : "Without aging:",
                    servedAt < 0 ? "NEVER served in 2000 ticks (starved)" : "served at tick " + servedAt);
        }

        System.out.println("\n=== Alternatives (n adds then n polls) ===");
        for (int n : new int[]{5_000, 20_000, 50_000}) {
            Random r = new Random(5);
            int[] sv = new int[n]; for (int i = 0; i < n; i++) sv[i] = 1 + r.nextInt(5);
            double tpq = 0, tll = 0, tfi = 0;
            for (int run = 0; run < 4; run++) {
                long t0 = System.nanoTime();
                PriorityQueue<P> q = new PriorityQueue<>(RULE);
                for (int i = 0; i < n; i++) q.add(new P("p", sv[i], i));
                while (!q.isEmpty()) sink += q.poll().arr();
                double a = (System.nanoTime() - t0) / 1e6;

                t0 = System.nanoTime();
                LinkedList<P> ll = new LinkedList<>();
                for (int i = 0; i < n; i++) {
                    P p = new P("p", sv[i], i); int pos = 0;
                    var it = ll.listIterator();
                    while (it.hasNext() && RULE.compare(it.next(), p) <= 0) pos++;
                    ll.add(pos, p);
                }
                while (!ll.isEmpty()) sink += ll.removeFirst().arr();
                double b = (System.nanoTime() - t0) / 1e6;

                t0 = System.nanoTime();
                @SuppressWarnings("unchecked") ArrayDeque<P>[] lv = new ArrayDeque[6];
                for (int i = 1; i <= 5; i++) lv[i] = new ArrayDeque<>();
                for (int i = 0; i < n; i++) lv[sv[i]].addLast(new P("p", sv[i], i));
                for (int served = 0; served < n; ) for (int s = 1; s <= 5; s++) if (!lv[s].isEmpty()) { sink += lv[s].pollFirst().arr(); served++; break; }
                double c = (System.nanoTime() - t0) / 1e6;
                if (run > 0) { tpq += a; tll += b; tfi += c; }
            }
            System.out.printf("n=%-7d PriorityQueue=%9.2f ms | sorted LinkedList=%10.2f ms | 5 FIFO queues=%8.2f ms%n", n, tpq / 3, tll / 3, tfi / 3);
        }
        System.out.println("(sink " + sink + ")");
    }
}
