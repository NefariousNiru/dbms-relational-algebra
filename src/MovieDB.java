
/*****************************************************************************************
 * @file  MovieDB.java
 *
 * @author   John Miller
 */




import static java.lang.System.out;

/*****************************************************************************************
 * The MovieDB class makes a Movie Database.  It serves as a template for making other
 * databases.  See "Database Systems: The Complete Book", second edition, page 26 for more
 * information on the Movie Database schema.
 */
class MovieDB
{
    /*************************************************************************************
     * Main method for creating, populating and querying a Movie Database.
     * @param args  the command-line arguments
     */
    public static void main (String [] args)
    {
        out.println ();

        var movie = new Table ("movie", "title year length genre studioName producerNo",
                                        "String Integer Integer String String Integer", "title year");

        var cinema = new Table ("cinema", "title year length genre studioName producerNo",
                                          "String Integer Integer String String Integer", "title year");

        var movieStar = new Table ("movieStar", "name address gender birthdate",
                                                "String String Character String", "name");

        var starsIn = new Table ("starsIn", "movieTitle movieYear starName",
                                            "String Integer String", "movieTitle movieYear starName");

        var movieExec = new Table ("movieExec", "certNo name address fee",
                                                "Integer String String Float", "certNo");

        var studio = new Table ("studio", "name address presNo",
                                          "String String Integer", "name");

        var film0 = new Comparable [] { "Star_Wars", 1977, 124, "sciFi", "Fox", 12345 };
        var film1 = new Comparable [] { "Star_Wars_2", 1980, 124, "sciFi", "Fox", 12345 };
        var film2 = new Comparable [] { "Rocky", 1985, 200, "action", "Universal", 12125 };
        var film3 = new Comparable [] { "Rambo", 1978, 100, "action", "Universal", 32355 };

        out.println ();
        movie.insert (film0);
        movie.insert (film1);
        movie.insert (film2);
        movie.insert (film3);
        movie.print ();

        var film4 = new Comparable [] { "Galaxy_Quest", 1999, 104, "comedy", "DreamWorks", 67890 };
        out.println ();
        cinema.insert (film2);
        cinema.insert (film3);
        cinema.insert (film4);
        cinema.print ();

        var star0 = new Comparable [] { "Carrie_Fisher", "Hollywood", 'F', "9/9/99" };
        var star1 = new Comparable [] { "Mark_Hamill", "Brentwood", 'M', "8/8/88" };
        var star2 = new Comparable [] { "Harrison_Ford", "Beverly_Hills", 'M', "7/7/77" };
        out.println ();
        movieStar.insert (star0);
        movieStar.insert (star1);
        movieStar.insert (star2);
        movieStar.print ();

        var cast0 = new Comparable [] { "Star_Wars", 1977, "Carrie_Fisher" };
        out.println ();
        starsIn.insert (cast0);
        starsIn.print ();

        var exec0 = new Comparable [] { 9999, "S_Spielberg", "Hollywood", 10000.00 };
        out.println ();
        movieExec.insert (exec0);
        movieExec.print ();

        var studio0 = new Comparable [] { "Fox", "Los_Angeles", 7777 };
        var studio1 = new Comparable [] { "Universal", "Universal_City", 8888 };
        var studio2 = new Comparable [] { "DreamWorks", "Universal_City", 9999 };
        out.println ();
        studio.insert (studio0);
        studio.insert (studio1);
        studio.insert (studio2);
        studio.print ();

        movie.save ();
        cinema.save ();
        movieStar.save ();
        starsIn.save ();
        movieExec.save ();
        studio.save ();

        movieStar.printIndex ();

        //--------------------- Test Index Methods
        
        test_index_methods(movie);

        //--------------------- project: title year

        out.println ();
        var t_project = movie.project ("title year");
        t_project.print ();

        //--------------------- select: equals, &&

        out.println ();
        var t_select = movie.select (t -> t[movie.col("title")].equals ("Star_Wars") &&
                                          t[movie.col("year")].equals (1977));
        t_select.print ();

        //--------------------- select: <

        out.println ();
        var t_select2 = movie.select (t -> (Integer) t[movie.col("year")] < 1980);
        t_select2.print ();

        //--------------------- select (condition): <

        out.println ();
        var t_select3 = movie.select ("year < 1980");
        t_select3.print ();


        //--------------------- indexed select tests: <
        test_indexed_select(movie, movieStar);


        //--------------------- union tests ---------------------

        test_indexed_union(movie, cinema);


        //--------------------- minus: movie MINUS cinema

        out.println ();
        var t_minus = movie.minus (cinema);
        t_minus.print ();

        //--------------------- equi-join: movie JOIN studio ON studioName = name

        out.println ();
        var t_join = movie.join ("studioName", "name", studio);
        t_join.print ();

        //--------------------- natural join: movie JOIN studio

        out.println ();
        var t_join2 = movie.join (cinema);
        t_join2.print ();

        //--------------------- theta join: movie JOIN studio
        out.println ();
        var t_join3 = movie.join ("studioName == name", studio);
        t_join3.print ();

    } // main

    /*************************************************************************************
     * Method for testing indexed select.
     * @param movie  the movie table
     * @param cinema the cinema table
     */
    private static void test_indexed_union(Table movie, Table cinema) {
        // Test 1: Union of two non-empty tables with some common elements
        out.println("Test 1: Union of movie and cinema (overlapping rows)");
        var t_union1 = movie.union(cinema);
        t_union1.print();

        // Test 2: Union of two non-empty tables with no common elements
        var uniqueTable = new Table("uniqueTable", "title year length genre studioName producerNo",
                "String Integer Integer String String Integer", "title year");

        var uniqueFilm = new Comparable[] { "Inception", 2010, 148, "sciFi", "WarnerBros", 56789 };
        uniqueTable.insert(uniqueFilm);

        out.println("Test 2: Union of movie and uniqueTable (no common elements)");
        var t_union2 = movie.union(uniqueTable);
        t_union2.print();

        // Test 3: Union of a table with itself (should return the same table)
        out.println("Test 3: Union of movie with itself (idempotency test)");
        var t_union3 = movie.union(movie);
        t_union3.print();

        // Test 4: Union with an empty table (should return the original table)
        var emptyTable = new Table("emptyTable", "title year length genre studioName producerNo",
                "String Integer Integer String String Integer", "title year");

        out.println("Test 4: Union of movie with an empty table (identity test)");
        var t_union4 = movie.union(emptyTable);
        t_union4.print();

        // Test 5: Union of two empty tables (should return an empty table)
        var anotherEmptyTable = new Table("anotherEmptyTable", "title year length genre studioName producerNo",
                "String Integer Integer String String Integer", "title year");

        out.println("Test 5: Union of two empty tables (empty result test)");
        var t_union5 = emptyTable.union(anotherEmptyTable);
        t_union5.print();

        // Test 6: Union of tables with different schemas (should fail or handle gracefully)
        var mismatchedTable = new Table("mismatchedTable", "director budget",
                "String Float", "director");

        var directorEntry = new Comparable[] { "Christopher_Nolan", 200_000_000f };
        mismatchedTable.insert(directorEntry);

        out.println("Test 6: Union of movie and mismatchedTable (schema mismatch)");
        try {
            var t_union6 = movie.union(mismatchedTable);
            t_union6.print();
        } catch (Exception e) {
            out.println("Expected error due to schema mismatch: " + e.getMessage());
        }
    }

    /*************************************************************************************
     * Method for testing indexed select.
     * @param movie  the movie table
     * @param movieStar the movieStar table
     */
    private static void test_indexed_select(Table movie, Table movieStar) {
    //--------------------- indexed select: key

    out.println ();
    var t_iselect = movieStar.select (new KeyType ("Harrison_Ford")); // should pass
    t_iselect.print ();

    //--------------------- indexed select: multiple keys

    out.println ();
    var t_iselect2 = movie.select (new KeyType ("Star_Wars", 1977)); // should pass
    t_iselect2.print ();

    //--------------------- indexed select: key does not exist

    out.println();
    var t_iselect3 = movieStar.select(new KeyType("Tom_Hanks"));  // Should return empty
    t_iselect3.print();

    //--------------------- indexed select: composite key (wrong order)

    out.println();
    var t_iselect4 = movie.select(new KeyType(1977, "Star_Wars"));  // Wrong order, should fail
    t_iselect4.print();

    //--------------------- indexed select: composite key (wrong type)

    out.println();
    var t_iselect5 = movie.select(new KeyType("Star_Wars", "1977"));  // Type mismatch: "1977" is String
    t_iselect5.print();

    //--------------------- indexed select: composite key (partial key)

    out.println();
    var t_iselect6 = movie.select(new KeyType("Star_Wars"));  // Partial key, should fail
    t_iselect6.print();


    //--------------------- indexed select: Insert & Immediate Lookup

    out.println();
    var newMovie = new Comparable[]{"New_Movie", 2025, 150, "action", "Warner", 55555};
    movie.insert(newMovie);
    var t_iselect8 = movie.select(new KeyType("New_Movie", 2025));  // Should find the inserted movie
    t_iselect8.print();
}

    /*************************************************************************************
     * Method for testing indexed select.
     * @param movie  the movie table
     */
    private static void test_index_methods(Table movie) {
        out.println("\nTEST 1: Creating index on 'genre' (should work)");
        movie.createIndex("genre");

        //   TEST 2: Create a unique index on "year" (should work) all unique vals
        out.println("\nTEST 2: Creating UNIQUE index on 'year' (should work)");
        movie.createUniqueIndex("year");

        
        // TEST 3: Attempt to create a unique index on "genre" (should fail due to duplicates)
        try {
            out.println("\nTEST 3: Creating UNIQUE index on 'genre' (should FAIL)");
            movie.createUniqueIndex("genre");  // This should throw an error
        } catch (IllegalArgumentException e) {
            out.println("Expected error: " + e.getMessage());
        }

        //   TEST 4: Drop index on "genre" (should remove index)
        out.println("\nTEST 4: Dropping index on 'genre' (should work)");
        boolean dropped = movie.dropIndex("genre");
        out.println("Drop status: " + (dropped ? "Success" : "Failed"));

        //   TEST 5: Drop index on "studioName" (should remove index)
        out.println("\nTEST 5: Dropping index on 'studioName' (should work)");
        dropped = movie.dropIndex("studioName");
        out.println("Drop status: " + (dropped ? "Success" : "Failed"));

        //   TEST 6: Drop index on a non-existent column "director" (should fail)
        out.println("\nTEST 6: Dropping index on 'director' (should FAIL - index does not exist)");
        dropped = movie.dropIndex("director");
        out.println("Drop status: " + (dropped ? "Success" : "Failed"));
    }
} // MovieDB

