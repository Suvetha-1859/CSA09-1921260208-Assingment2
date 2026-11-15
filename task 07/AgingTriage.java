import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

public class AgingTriage {
    static final int STEP = 50;
    static long SEQ;   // unique admission number: final tie-breaker

    record P(String name, int sev, long arr, long seq) {
        P(String name, int sev, long arr) { this(name, sev, arr, ++SEQ); }
    }

    static long comparisons;

    static int eff(P p, long now) { return Math.max(1, p.sev() - (int) ((now - p.arr()) / STEP)); }

    static int cmp(P a, P b, long now) {
        comparisons++;
        int c = Integer.compare(eff(a, now), eff(b, now));
        if (c != 0) return c;
        c = Long.compare(a.arr(), b.arr());
        return c != 0 ? c : Long.compare(a.seq(), b.seq());
    }

    interface Triage { void admit(P p); P serve(long now); int size(); }

    /** ORIGINAL: rebuild the heap on every serve. */
    static class Rebuild implements Triage {
        private final List<P> waiting = new ArrayList<>();
        public void admit(P p) { waiting.add(p); }
        public P serve(long now) {
            if (waiting.isEmpty()) return null;
            PriorityQueue<P> q = new PriorityQueue<>((a, b) -> cmp(a, b, now));
            q.addAll(waiting);
            P p = q.poll();
            waiting.remove(p);
            return p;
        }
        public int size() { return waiting.size(); }
    }

    /** REDESIGN: five FIFO queues, compare only the heads. */
    static class Buckets implements Triage {
        @SuppressWarnings("unchecked")
        private final ArrayDeque<P>[] lv = new ArrayDeque[6];
        private int size;
        Buckets() { for (int i = 1; i <= 5; i++) lv[i] = new ArrayDeque<>(); }
        public void admit(P p) { lv[p.sev()].addLast(p); size++; }
        public P serve(long now) {
            P best = null;
            for (int s = 1; s <= 5; s++) {
                P h = lv[s].peekFirst();
                if (h != null && (best == null || cmp(h, best, now) < 0)) best = h;
            }
            if (best != null) { lv[best.sev()].pollFirst(); size--; }
            return best;
        }
        public int size() { return size; }
    }

    static class Result { double ms; long cmps; String sequence; int maxBacklog; long maxWait; }

    static Result simulate(Triage t, int ticks, long seed) {
        Random r = new Random(seed);
        SEQ = 0;
        StringBuilder seq = new StringBuilder();
        Result res = new Result();
        comparisons = 0;
        long t0 = System.nanoTime();
        for (long now = 1; now <= ticks; now++) {
            for (int k = 0; k < 4; k++) t.admit(new P("n" + now + "_" + k, 1 + r.nextInt(5), now));
            for (int k = 0; k < 3; k++) {
                P p = t.serve(now);
                res.maxWait = Math.max(res.maxWait, now - p.arr());
                seq.append(p.name()).append(',');
            }
            res.maxBacklog = Math.max(res.maxBacklog, t.size());
        }
        res.ms = (System.nanoTime() - t0) / 1e6;
        res.cmps = comparisons;
        res.sequence = seq.toString();
        return res;
    }

    public static void main(String[] args) {
        System.out.println("Java " + System.getProperty("java.version"));
        System.out.printf("%-8s %-10s %14s %16s %14s %14s %s%n", "ticks", "design", "time ms (avg3)", "comparisons", "max backlog", "max wait", "same sequence as original?");
        for (int ticks : new int[]{500, 1000, 2000, 4000, 8000}) {
            simulate(new Buckets(), 200, 1); simulate(new Rebuild(), 100, 1);
            Result o = null, nw = null; double to = 0, tn = 0;
            for (int run = 0; run < 3; run++) {
                o = simulate(new Rebuild(), ticks, 11); to += o.ms;
                nw = simulate(new Buckets(), ticks, 11); tn += nw.ms;
            }
            boolean same = o.sequence.equals(nw.sequence);
            System.out.printf("%-8d %-10s %14.2f %,16d %14d %14d %s%n", ticks, "Rebuild", to / 3, o.cmps, o.maxBacklog, o.maxWait, "-");
            System.out.printf("%-8d %-10s %14.2f %,16d %14d %14d %s   speed-up %.0fx%n", ticks, "Buckets", tn / 3, nw.cmps, nw.maxBacklog, nw.maxWait, same, to / tn);
        }

        System.out.println("\n=== Correctness on the Q6 hand case ===");
        String[] names = {"Anil", "Bala", "Chitra", "Dev", "Esha", "Farid", "Gita", "Hari"};
        int[] sev = {4, 2, 5, 2, 1, 4, 3, 1};
        Triage b = new Buckets();
        for (int i = 0; i < 8; i++) b.admit(new P(names[i], sev[i], i));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) { P p = b.serve(8); sb.append(p.name()).append('(').append(p.sev()).append(") "); }
        System.out.println(sb.toString().trim());
        System.out.println("Expected      : Esha(1) Hari(1) Bala(2) Dev(2) Gita(3) Anil(4) Farid(4) Chitra(5)");

        Triage t = new Buckets();
        t.admit(new P("OldS5", 5, 0));
        for (int i = 0; i < 10; i++) t.admit(new P("NewS2_" + i, 2, 160));
        System.out.println("\nAging check: at tick 160 first served = " + t.serve(160).name() + " (expected OldS5)");
    }
}
