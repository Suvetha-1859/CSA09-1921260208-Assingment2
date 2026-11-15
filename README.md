# CSA09 – Programming in Java – Assignment 2 (CO2)

| | |
|---|---|
| **Name** | Suvetha S |
| **Register number** | 1921260208 |
| **GitHub** | Suvetha-1859 |
| **Repository** | CSA09-1921260208-Assignment2 |
| **Java version used** | OpenJDK 21.0.12.1 |
| **Machine** | 2 CPU cores, Linux; timings are averages of repeated runs after JIT warm-up |

## How to compile and run

Every task is in its own folder. From the repository root:

```bash
cd Q1 && javac TicketBench.java && java TicketBench
cd ../Q2 && javac PlaylistBench.java && java PlaylistBench
cd ../Q3 && javac Attendance.java ContractTests.java && java Attendance && java ContractTests
cd ../Q4 && javac PharmacyBench.java && java PharmacyBench
cd ../Q5 && javac LeaderboardDemo.java SortStrategies.java && java LeaderboardDemo && java SortStrategies
cd ../Q6 && javac Triage.java TriageStudy.java && java Triage && java TriageStudy
cd ../Q7 && javac AgingTriage.java && java AgingTriage
```

Raw program output for every run is saved in each `Qn/output/` folder. Timing numbers vary a little from run
to run (JIT, garbage collection, other processes); the *shape* of each result (which approach grows faster) is stable.

## Folder contents and result of each task

| Folder | Files | Result in one line |
|---|---|---|
| `Q1` | `TicketBench.java` | `+` in a loop copies O(n²) chars: 830 ms vs 4.8 ms for StringBuilder on 2,000-field tickets (≈170×), but negligible for a single short ticket. StringBuilder lost updates in 4 of 5 multithreaded trials; StringBuffer in 0 of 5. |
| `Q2` | `PlaylistBench.java` | ArrayList wins get(k), middle insert/remove and iteration; LinkedList wins only front operations; ArrayDeque matches LinkedList for the "Up Next" queue at half the memory per element. |
| `Q3` | `Attendance.java`, `ContractTests.java`, `before_todo/Attendance.java` | Without `hashCode()` the program prints `Unique students: 3`; with TODO 1 completed it prints `Unique students: 2`. Four designed tests, all predictions matched. |
| `Q4` | `PharmacyBench.java` | HashMap ≈ 66 ns/lookup, TreeMap ≈ 1,084 ns at 200,000 medicines; TreeMap gives the code-order report for free, LinkedHashMap the insertion-order report. |
| `Q5` | `LeaderboardDemo.java`, `SortStrategies.java` | Output `[Eagles(9,6), Bisons(9,4), Panthers(9,4), Cobras(6,1)]` as expected; a subtraction comparator is shown to violate transitivity (17 violating triples). |
| `Q6` | `Triage.java`, `TriageStudy.java` | Output `Meena(S1) Kumar(S1) Lata(S2) Ravi(S3) John(S3) Asha(S5)` as expected; starvation demonstrated and fixed with an aging rule. |
| `Q7` | `AgingTriage.java` | Diagnosed the Q6 aging rule (heap rebuilt every tick); redesign with five FIFO queues is identical in output and about 500× faster at 8,000 ticks. |

The "Complete the Code" programs (`Q3/Attendance.java`, `Q5/LeaderboardDemo.java`, `Q6/Triage.java`) keep the given
code unchanged and replace only the `TODO` lines, each marked `(completed)`.

## Commit history

One commit per task (Q1 … Q7) followed by the README and report commits, so the history shows the work in progress.

## Report

`Assignment2_Report.docx` contains the predictions, designs, tables, complexity analysis and conclusions for Q1–Q7.

