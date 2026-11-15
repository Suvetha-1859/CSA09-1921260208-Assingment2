import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class SortStrategies {
    record T(String name, int points, int gd) {}
    static long sink;

    public static void main(String[] a) {
        Comparator<T> chain = Comparator.comparingInt((T t) -> t.points()).reversed()
                .thenComparing(Comparator.comparingInt((T t) -> t.gd()).reversed()).thenComparing(T::name);
        for (int n : new int[]{10_000, 100_000, 500_000}) {
            Random r = new Random(3);
            List<T> base = new ArrayList<>();
            for (int i = 0; i < n; i++) base.add(new T("T" + r.nextInt(n), r.nextInt(30), r.nextInt(40) - 20));
            double c1 = 0, c3 = 0;
            for (int run = 0; run < 6; run++) {
                List<T> x = new ArrayList<>(base);
                long t0 = System.nanoTime(); x.sort(chain); double t = (System.nanoTime() - t0) / 1e6; sink += x.get(0).points();
                List<T> y = new ArrayList<>(base);
                long t1 = System.nanoTime();
                y.sort(Comparator.comparing(T::name));
                y.sort(Comparator.comparingInt((T q) -> q.gd()).reversed());
                y.sort(Comparator.comparingInt((T q) -> q.points()).reversed());
                double t3 = (System.nanoTime() - t1) / 1e6; sink += y.get(0).points();
                if (run > 0) { c1 += t; c3 += t3; }
                if (run == 5) System.out.println("   results identical: " + x.equals(y));
            }
            System.out.printf("n=%-8d single chain sort = %8.2f ms | three stable sorts = %8.2f ms%n", n, c1 / 5, c3 / 5);
        }
        System.out.println("(sink " + sink + ")");
    }
}
