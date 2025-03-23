import java.util.Arrays;
import java.util.function.Predicate;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import static java.lang.System.out;

public class Project3 {

    private static final Logger logger = Logger.getLogger(Project3.class.getName());

    static {
        try {
            FileHandler fh = new FileHandler("project3.log", true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setUseParentHandlers(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // 1) Run tests for Strong Filter
        logger.info("=== Strong Filter Tests ===");
        runTestsForFilterType("STRONG");

//        // 2) Run tests for Medium Filter
//        logger.info("=== Medium Filter Tests ===");
//        runTestsForFilterType("MEDIUM");
//
//        // 3) Run tests for Weak Filter
//        logger.info("=== Weak Filter Tests ===");
//        runTestsForFilterType("WEAK");
    }

    /**
     * Loops over data sizes (100k to 1M) and runs 6 iterations each time.
     * Discards the first iteration (JIT warm-up) and averages the remaining 5.
     *
     * @param filterType "STRONG", "MEDIUM", or "WEAK"
     */
    private static void runTestsForFilterType(String filterType) {
        for (int j = 100_000; j <= 1_000_000; j += 100_000) {
            logger.info("Tuples: " + j);
            long[] times = new long[6];

            // 1) Generate random data & build the tables
            Table[] tables = generateTuples(j);
            Table student = tables[0];
            Table course   = tables[1];
            Table takes  = tables[2];
            Predicate<Comparable[]> predicate = getPredicate(filterType);

            for (int i = 0; i < 6; i++) {
                // 1) Start timing
                long start = System.nanoTime();

                // 2) Run the query (select → join → join → project) with chosen filter
                Table joins = student.select(predicate).i_join(takes).i_join(course).project("cname");

                // 3) End timing
                times[i] = System.nanoTime() - start;
            }
            logger.info(Arrays.toString(times));

            // 5) Discard the first iteration, average the remaining 5
            long sum = 0;
            for (int i = 1; i < 6; i++) {
                sum += times[i];
            }
            long averageTime = sum / 5;

            // 6) Print or store the result (in nanoseconds, or convert to ms)
            logger.info(String.format("DataSize=%d, Filter=%s, AverageTime(ns)=%d%n",
                    j, filterType, averageTime));
        }
    }

    /**
     * Generates j Student tuples, 2*j Takes tuples, and j/2 Course tuples using TupleGenerator.
     * Then inserts them into Table objects.
     *
     * @param j number of rows for Student
     * @return an array { studentTable, takesTable, courseTable }
     */
    private static Table[] generateTuples(int j) {
        // 1) Set up the generator
        TupleGenerator generator = new TupleGeneratorImpl();

        generator.addRelSchema("Student",
                "sid sname",     // attributes
                "Integer String", // domains
                "sid",                          // primary key
                null);                          // no foreign key

        generator.addRelSchema("Course",
                "cid cname",
                "Integer String",
                "cid",
                null);

        generator.addRelSchema("Takes",
                "sid cid",
                "Integer Integer",
                "sid cid",
                new String[][] {
                        {"sid","Student","sid"},
                        {"cid", "Course", "cid"}
                }
        );

        out.println("Generating start");
        // 2) Decide how many rows for each table
        //    Student: j, Course: j, Takes: j/8
        int[] tups = { j, j, j / 8};

        // 3) Generate the data (3D array: table i, row j, column k)
        Comparable[][][] result = generator.generate(tups);

        out.println("generated");

        // 4) Create Table objects with matching schemas
        Table student = new Table("Student",
                "sid sname",
                "Integer String",
                "sid");

        Table course = new Table("Course",
                "cid cname",
                "Integer String",
                "cid");

        Table takes = new Table("Takes",
                "sid cid",
                "Integer Integer",
                "sid cid");


        // 5) Insert generated tuples into the Table objects
        for (Comparable[] row : result[0]) {
            student.insert(row);
        }
        out.println("Inserted Student");
        for (Comparable[] row : result[1]) {
            course.insert(row);
        }
        out.println("Inserted Course");
        for (Comparable[] row : result[2]) {
            takes.insert(row);
        }
        out.println("Inserted Takes");

        // (Optional) Create indices if desired:
        student.createUniqueIndex("sid");
        takes.createIndex("sid");
        takes.createIndex("cid");
        course.createUniqueIndex("cid");

        out.println("Index Created");

        return new Table[]{ student, course, takes };
    }

    private static Predicate<Comparable[]> getPredicate(String filterType) {
        Predicate<Comparable[]> predicate = switch (filterType) {
            case "STRONG" -> (Comparable[] row) -> ((Integer) row[0]) > 90_000_000;  // ~10% pass
            case "MEDIUM" -> (Comparable[] row) -> ((Integer) row[0]) > 50_000_000;  // ~50% pass
            case "WEAK"   -> (Comparable[] row) -> ((Integer) row[0]) > 10_000_000;  // ~90% pass
            default       -> (Comparable[] row) -> true; // no filter
        };
        return predicate;
    }
}
