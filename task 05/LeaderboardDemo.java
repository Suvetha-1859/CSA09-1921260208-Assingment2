import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

class Team {
    final String name; final int points; final int goalDiff;
    Team(String name, int points, int goalDiff) {
        this.name = name; this.points = points; this.goalDiff = goalDiff;
    }
    @Override public String toString() {
        return name + "(" + points + "," + goalDiff + ")";
    }
}

class Leaderboard<T> {
    private final List<T> items = new ArrayList<>();
    private final Comparator<? super T> order;
    Leaderboard(Comparator<? super T> order) { this.order = order; }
    void add(T item) { items.add(item); }
    void addAll(List<? extends T> more) { items.addAll(more); }

    List<T> ranking() {
        // TODO 1: sorted copy, items not modified
        List<T> copy = new ArrayList<>(items);
        copy.sort(order);
        return copy;
    }
}

public class LeaderboardDemo {

    static final Comparator<Team> BROKEN_SUBTRACT = (a, b) -> b.points - a.points;

    static final Comparator<Team> BROKEN_NO_NAME =
            Comparator.comparingInt((Team t) -> t.points).reversed()
                      .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed());

    public static void main(String[] args) {
        // TODO 2: points (high first), goal difference (high first), name (A-Z)
        Comparator<Team> byRank = Comparator.comparingInt((Team t) -> t.points).reversed()
                .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
                .thenComparing(t -> t.name);

        Leaderboard<Team> board = new Leaderboard<>(byRank);
        board.add(new Team("Panthers", 9, 4));
        board.add(new Team("Eagles", 9, 6));
        board.add(new Team("Cobras", 6, 1));
        board.add(new Team("Bisons", 9, 4));
        System.out.println(board.ranking());

        System.out.println("\n--- Test 1: ties at every level ---");
        List<Team> ties = new ArrayList<>(List.of(
                new Team("Dragons", 6, 0), new Team("Ants", 6, 0), new Team("Cats", 6, 2),
                new Team("Zebras", 9, 3), new Team("Yaks", 9, 3), new Team("Wolves", 9, 5),
                new Team("Moles", 3, -1), new Team("Lions", 3, -1)));
        Leaderboard<Team> t1 = new Leaderboard<>(byRank);
        t1.addAll(ties);
        System.out.println("Expected: [Wolves(9,5), Yaks(9,3), Zebras(9,3), Cats(6,2), Ants(6,0), Dragons(6,0), Lions(3,-1), Moles(3,-1)]");
        System.out.println("Actual:   " + t1.ranking());
        List<Team> shuffled = new ArrayList<>(ties); Collections.shuffle(shuffled, new Random(1));
        Leaderboard<Team> t1b = new Leaderboard<>(byRank); t1b.addAll(shuffled);
        System.out.println("Same result for a different input order: " + t1.ranking().toString().equals(t1b.ranking().toString()));

        System.out.println("\n--- Test 2: data designed to break faulty comparators ---");
        List<Team> trap = List.of(
                new Team("Big", Integer.MAX_VALUE, 0),
                new Team("Neg", Integer.MIN_VALUE + 5, 0),
                new Team("Mid", 0, 0),
                new Team("Beta", 5, 1), new Team("Alpha", 5, 1));
        Leaderboard<Team> good = new Leaderboard<>(byRank);
        Leaderboard<Team> bad1 = new Leaderboard<>(BROKEN_SUBTRACT);
        Leaderboard<Team> bad2 = new Leaderboard<>(BROKEN_NO_NAME);
        good.addAll(trap); bad1.addAll(trap); bad2.addAll(trap);
        System.out.println("Correct comparator : " + good.ranking());
        System.out.println("Subtraction (b-a)  : " + bad1.ranking());
        System.out.println("Missing name tie   : " + bad2.ranking());
        List<Team> rev = new ArrayList<>(trap); Collections.reverse(rev);
        Leaderboard<Team> bad2r = new Leaderboard<>(BROKEN_NO_NAME); bad2r.addAll(rev);
        System.out.println("Missing name, reversed input: " + bad2r.ranking());
        System.out.println("Transitivity violations - correct: " + violations(trap, byRank)
                + " | subtraction: " + violations(trap, BROKEN_SUBTRACT));

        Leaderboard<String> words = new Leaderboard<>(Comparator.comparing(String::length).thenComparing(Comparator.naturalOrder()));
        words.add("pear"); words.add("fig"); words.add("apple"); words.add("kiwi");
        System.out.println("\nReuse with String: " + words.ranking());
    }

    static int violations(List<Team> l, Comparator<Team> c) {
        int v = 0;
        for (Team a : l) for (Team b : l) for (Team d : l)
            if (c.compare(a, b) < 0 && c.compare(b, d) < 0 && !(c.compare(a, d) < 0)) v++;
        return v;
    }
}
