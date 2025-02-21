
/****************************************************************************************
 * @file  Table.java
 *
 * @author   John Miller
 *
 * compile javac *.java
 * run     java MovieDB    
 */



import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.Boolean.*;
import static java.lang.System.arraycopy;
import static java.lang.System.out;

/****************************************************************************************
 * The Table class implements relational database tables (including attribute names, domains
 * and a list of tuples.  Five basic relational algebra operators are provided: project,
 * select, union, minus and join.  The insert data manipulation operator is also provided.
 * Missing are update and delete data manipulation operators.
 */
public class Table
       implements Serializable
{
    /** Relative path for storage directory
     */
    private static final String DIR = "store" + File.separator;

    /** Filename extension for database files
     */
    private static final String EXT = ".dbf";

    /** Counter for naming temporary tables.
     */
    private static int count = 0;

    /** Table name.
     */
    private final String name;

    /** Array of attribute names.
     */
    private final String [] attribute;

    /** Array of attribute domains: a domain may be
     *  integer types: Long, Integer, Short, Byte
     *  real types: Double, Float
     *  string types: Character, String
     */
    private final Class [] domain;

    /** Collection of tuples (data storage).
     */
    private final List <Comparable []> tuples;

    /** Primary key (the attributes forming). 
     */
    private final String [] key;

    /** Index into tuples (maps key to tuple).
     */
    private final Map <KeyType, Comparable []> index;

    /** Index into tuples (maps key to tuple).
     */
    private final Map<String, Map<Comparable, List<Comparable[]>>> secondaryIndices;

    /** The supported map types.
     */
    private enum MapType { NO_MAP, TREE_MAP, HASH_MAP, LINHASH_MAP, BPTREE_MAP }

    /** The map type to be used for indices.  Change as needed.
     */
    private static final MapType mType = MapType.HASH_MAP;

    /************************************************************************************
     * Make a map (index) given the MapType.
     */
    private static Map <KeyType, Comparable []> makeMap ()
    {
        return switch (mType) {
        case NO_MAP      -> null;
        case TREE_MAP    -> new TreeMap <> ();
        case HASH_MAP    -> new HashMap <> ();
//        Use in Project 2
//        case LINHASH_MAP -> new LinHashMap <> (KeyType.class, Comparable [].class);
//        case BPTREE_MAP  -> new BpTreeMap <> (KeyType.class, Comparable [].class);
        default          -> null;
        }; // switch
    } // makeMap


    /**
     * Creates a map for secondary indices (column value → list of rows).
     * Ensures that the selected map type is consistent with `mType`.
     *
     * @return A map where keys are column values, and values are lists of rows.
     */
    private static Map<Comparable, List<Comparable[]>> makeIndexMap() {
        return switch (mType) {
            case TREE_MAP    -> new TreeMap<>();
            case HASH_MAP    -> new HashMap<>();
//        case LINHASH_MAP -> new LinHashMap <> (KeyType.class, Comparable [].class);
//        case BPTREE_MAP  -> new BpTreeMap <> (KeyType.class, Comparable [].class);
            default          -> throw new IllegalArgumentException("Unsupported index type: " + mType);
        };
    }

    /************************************************************************************
     * Concatenate two arrays of type T to form a new wider array.
     *
     * @see <a href="http://stackoverflow.com/questions/80476/how-to-concatenate-two-arrays-in-java">...</a>
     *
     * @param arr1  the first array
     * @param arr2  the second array
     * @return a wider array containing all the values from arr1 and arr2
     */
    public static <T> T [] concat (T [] arr1, T [] arr2)
    {
        T [] result = Arrays.copyOf (arr1, arr1.length + arr2.length);
        arraycopy (arr2, 0, result, arr1.length, arr2.length);
        return result;
    } // concat

    //-----------------------------------------------------------------------------------
    // Constructors
    //-----------------------------------------------------------------------------------

    /************************************************************************************
     * Construct an empty table from the meta-data specifications.
     *
     * @param _name       the name of the relation
     * @param _attribute  the string containing attributes names
     * @param _domain     the string containing attribute domains (data types)
     * @param _key        the primary key
     */  
    public Table (String _name, String [] _attribute, Class [] _domain, String [] _key)
    {
        name      = _name;
        attribute = _attribute;
        domain    = _domain;
        key       = _key;
        tuples    = new ArrayList <> ();
        index     = makeMap ();
        secondaryIndices = new HashMap<>();
        out.println (Arrays.toString (domain));
    } // constructor

    /************************************************************************************
     * Construct a table from the meta-data specifications and data in _tuples list.
     *
     * @param _name       the name of the relation
     * @param _attribute  the string containing attributes names
     * @param _domain     the string containing attribute domains (data types)
     * @param _key        the primary key
     * @param _tuples     the list of tuples containing the data
     */  
    public Table (String _name, String [] _attribute, Class [] _domain, String [] _key,
                  List <Comparable []> _tuples)
    {
        name      = _name;
        attribute = _attribute;
        domain    = _domain;
        key       = _key;
        tuples    = _tuples;
        index     = makeMap ();
        secondaryIndices = new HashMap<>();
    } // constructor

    /************************************************************************************
     * Construct an empty table from the raw string specifications.
     *
     * @param _name       the name of the relation
     * @param attributes  the string containing attributes names
     * @param domains     the string containing attribute domains (data types)
     * @param _key        the primary key
     */
    public Table (String _name, String attributes, String domains, String _key)
    {
        this (_name, attributes.split (" "), findClass (domains.split (" ")), _key.split(" "));

        out.println ("DDL> create table " + name + " (" + attributes + ")");
    } // constructor

    //----------------------------------------------------------------------------------
    // Public Methods
    //----------------------------------------------------------------------------------

    /**
     * Creates a non-unique index on the specified column.
     * The index maps each column value to a list of rows having that value.
     *
     * @param columnName The column to be indexed.
     * @throws IllegalArgumentException If the column does not exist.
     */
    public void createIndex(String columnName)
    {
        out.println("Creating index on column: " + columnName);

        int colIdx = col(columnName);
        if (colIdx == - 1) {
            throw new IllegalArgumentException("Column " + columnName + " does not exist in table " + name);
        }

        if (mType == MapType.NO_MAP) {
            throw new IllegalArgumentException("Cannot create index when mType is NO_MAP.");
        }

        Map<Comparable, List<Comparable[]>> indexMap = makeIndexMap();

        for (var tuple: tuples) {
            Comparable key = tuple[colIdx];
            indexMap.computeIfAbsent(key, k -> new ArrayList<>()).add(tuple);
        }

        secondaryIndices.put(columnName, indexMap);
    }

    /**
     * Creates a unique index on the specified column.
     * This ensures that no two tuples have the same value for the indexed column.
     *
     * @param columnName The column to be uniquely indexed.
     * @throws IllegalArgumentException If the column does not exist or contains duplicates.
     */
    public void createUniqueIndex(String columnName) {
        out.println("Creating unique index on column: " + columnName);

        int colIdx = col(columnName);
        if (colIdx == -1) {
            throw new IllegalArgumentException("Column " + columnName + " does not exist in table " + name);
        }

        Map<Comparable, List<Comparable[]>> uniqueIndexMap = makeIndexMap();

        for (var tuple : tuples) {
            Comparable key = tuple[colIdx];
            if (uniqueIndexMap.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate value found in column " + columnName + ": " + key);
            }
            List<Comparable[]> singletonList = new ArrayList<>();
            singletonList.add(tuple);
            uniqueIndexMap.put(key, singletonList);
        }

        secondaryIndices.put(columnName, uniqueIndexMap);
    }

    /**
     * Drops an existing index on the specified column.
     *
     * @param columnName The column whose index should be dropped.
     * @return `true` if the index was successfully removed, `false` if no index or no column existed.
     */
    public boolean dropIndex(String columnName) {
        out.println("Dropping index on column: " + columnName);

        int colIdx = col(columnName);
        if (colIdx == -1) {
            out.println("No such column exists in table " + name + ".");
            return false;
        }

        if (secondaryIndices.containsKey(columnName)) {
            secondaryIndices.remove(columnName);
            out.println("Index on column " + columnName + " has been dropped.");
            return true;
        } else {
            out.println("No index found on column " + columnName + ".");
            return false;
        }
    }

    /************************************************************************************
     * Project the tuples onto a lower dimension by keeping only the given attributes.
     * Check whether the original key is included in the projection.
     *
     * #usage movie.project ("title year studioNo")
     *
     * @param attributes  the attributes to project onto
     * @return  a table of projected tuples
     */
    public Table project (String attributes)
    {
        out.println ("RA> " + name + ".project (" + attributes + ")");
        var attrs     = attributes.split (" ");
        var colDomain = extractDom (match (attrs), domain);
        var newKey    = (Arrays.asList (attrs).containsAll (Arrays.asList (key))) ? key : attrs;

        List <Comparable []> rows = new ArrayList <> ();

        for (var tuple : tuples) {
            var newTuples = new Comparable[attrs.length]; //array that holds projected tuple
            //find corresponding col for each attr and add it to newTuples
            for (int i = 0; i < attrs.length; i++) {
                newTuples[i] = tuple[col(attrs[i])];
            }
            rows.add(newTuples); //add newTuples into rows
        }

        return new Table (name + count++, attrs, colDomain, newKey, rows);
    } // project

    /************************************************************************************
     * Select the tuples satisfying the given predicate (Boolean function).
     *
     * #usage movie.select (t -> t[movie.col("year")].equals (1977))
     *
     * @param predicate  the check condition for tuples
     * @return  a table with tuples satisfying the predicate
     */
    public Table select (Predicate <Comparable []> predicate)
    {
        out.println ("RA> " + name + ".select (" + predicate + ")");

        return new Table (name + count++, attribute, domain, key,
                   tuples.stream ().filter (t -> predicate.test (t))
                                   .collect (Collectors.toList ()));
    } // select

    /************************************************************************************
     * Select the tuples satisfying the given simple condition on attributes/constants
     * compared using an <op> ==, !=, <, <=, >, >=.
     *
     * #usage movie.select ("year == 1977")
     *
     * @param condition  the check condition as a string for tuples
     * @return  a table with tuples satisfying the condition
     */
    public Table select (String condition)
    {
        out.println ("RA> " + name + ".select (" + condition + ")");

        List <Comparable []> rows = new ArrayList <> ();

        var token = condition.split (" ");
        if (token.length != 3) {
            throw new IllegalArgumentException("Condition must in the format 'column op value'");
        }

        String columnName = token[0];
        String operator =  token[1];
        String value = token[2];

        int colNo = col(columnName);
        if (colNo == -1) {
            String errorMessage = String.format("Column %s does not exists", columnName);
            throw new IllegalArgumentException(errorMessage);
        }

        Comparable<?> parsedValue = parseValue(value, domain[colNo]);

        for (var t : tuples) {
            if (satifies (t, colNo, operator, parsedValue)) {
                rows.add (t);
            } // if
        } // for

        return new Table (name + count++, attribute, domain, key, rows);
    } // select

    /************************************************************************************
     * Parses a string value into its corresponding type based on the provided domain.
     *
     * #Usage: parseValue("1980", Integer.class) -> 1980
     *
     * @param value   the string representation of the value to be parsed
     * @param domain  the class type of the domain (e.g., Integer.class, String.class)
     * @return        the parsed value as a Comparable object of the specified type
     * @throws        IllegalArgumentException if the domain type is unsupported
     */
    private Comparable<?> parseValue(String value, Class<?> domain) {
        String errorMessage = String.format("Unsupported Domain Type: %s", domain.getName());
        return switch (domain.getSimpleName()) {
            case "Byte"      -> Byte.valueOf(value);
            case "Character" -> value.charAt(0);
            case "Double"    -> Double.valueOf(value);
            case "Float"     -> Float.valueOf(value);
            case "Integer"   -> Integer.valueOf(value);
            case "Long"      -> Long.valueOf(value);
            case "Short"     -> Short.valueOf(value);
            case "String"    -> value;
            default -> throw new IllegalArgumentException(errorMessage);
        };
    }

    /************************************************************************************
     * Evaluates whether a tuple satisfies a given condition on a specified column.
     * The condition is in the form t[colNo] op value, where `op` can be ==, !=, <, <=, >, >=.
     *
     * #Usage: satisfies(t, 1, "<", 1980)
     *
     * @param t       the tuple (array of Comparable values) to evaluate
     * @param colNo   the index of the column in the tuple to be compared
     * @param op      the comparison operator (==, !=, <, <=, >, >=)
     * @param value   the value to compare the tuple's column value against
     * @return        true if the condition is satisfied, false otherwise
     * @throws        IllegalArgumentException if the operator is unsupported or types are mismatched
     */
    private boolean satifies (Comparable [] t, int colNo, String op, Comparable<?> value)
    {
        var t_A = t[colNo];
        out.println ("satisfies: " + t_A + " " + op + " " + value);
        int compare = t_A.compareTo(value);
        String errorMessage = String.format("Unsupported Operator: %s", op);
        return switch (op) {
            case "==" -> compare == 0;
            case "!=" -> compare != 0;
            case "<"  -> compare <  0;
            case "<=" -> compare <= 0;
            case ">"  -> compare >  0;
            case ">=" -> compare >= 0;
            default -> throw new IllegalArgumentException(errorMessage);
        };
    } // satifies

    /************************************************************************************
     * Select the tuples satisfying the given key predicate (key = value).  Use an index
     * (Map) to retrieve the tuple with the given key value.  INDEXED SELECT algorithm.
     *
     * @param keyVal  the given key value
     * @return  a table with the tuple satisfying the key predicate
     */
    public Table select (KeyType keyVal)
    {
        out.println ("RA> " + name + ".select (" + keyVal + ")");

        List <Comparable []> rows = new ArrayList <> ();

        // Checks if index is not null and index contains the key
        if (index != null && index.containsKey(keyVal)) {
            rows.add(index.get(keyVal));                               // O(1) Lookup and append
        } else {
            out.println("INFO: No matching record found for key: " + keyVal);
        }

        return new Table (name + count++, attribute, domain, key, rows);
    } // select

    /************************************************************************************
     * Union this table and table2.  Check that the two tables are compatible.
     *
     * #usage movie.union (show)
     *
     * @param table2  the rhs table in the union operation
     * @return  a table representing the union
     */
    public Table union(Table table2) {
        out.println("RA> " + name + ".union(" + table2.name + ")");

        if (!compatible(table2)) {
            throw new IllegalArgumentException("Tables are not compatible for union.");
        }

        Map<KeyType, Comparable[]> unionMap = new LinkedHashMap<>();

        if (index != null) {
            unionMap.putAll(index);
        } else {
            for (var t : tuples) {
                KeyType key = extractPrimaryKey(t);
                unionMap.putIfAbsent(key, t);
            }
        }

        if (table2.index != null) {
            for (var entry : table2.index.entrySet()) {
                unionMap.putIfAbsent(entry.getKey(), entry.getValue());
            }
        } else {
            for (var t : table2.tuples) {
                KeyType key = extractPrimaryKey(t);
                unionMap.putIfAbsent(key, t);
            }
        }

        List<Comparable[]> rows = new ArrayList<>(unionMap.values());

        Table result = new Table(name + count++, attribute, domain, key, rows);
        if (mType != MapType.NO_MAP) {
            result.index.putAll(unionMap);
        }

        return result;
    } // union

    /**
     * Extracts the primary key from a given tuple.
     *
     * This method retrieves the values corresponding to the primary key attributes
     * and creates a {@link KeyType} instance representing the primary key.
     *
     * @param tuple The tuple (row) from which the primary key should be extracted.
     * @return A {@link KeyType} instance representing the primary key.
     * @throws IllegalArgumentException If the tuple is null or any primary key column is missing.
     */
    private KeyType extractPrimaryKey(Comparable[] tuple) {
        if (tuple == null) {
            throw new IllegalArgumentException("Tuple cannot be null.");
        }

        Comparable[] keyValues = new Comparable[key.length];
        for (int i = 0; i < key.length; i++) {
            int colIndex = col(key[i]);
            if (colIndex == -1) {
                throw new IllegalArgumentException("Primary key column not found: " + key[i]);
            }
            keyValues[i] = tuple[colIndex];
        }
        return new KeyType(keyValues);
    }

    /************************************************************************************
     * Take the difference of this table and table2.  Check that the two tables are
     * compatible.
     *
     * #usage movie.minus (show)
     *
     * @param table2  The rhs table in the minus operation
     * @return  a table representing the difference
     */
    public Table minus (Table table2)
    {
        try {
            out.println ("RA> " + name + ".minus (" + table2.name + ")");

            // 1. Schema Check: Ensure both tables have the same structure
            if (!Arrays.equals(attribute, table2.attribute)) {
                throw new IllegalArgumentException("Schemas do not match. MINUS operation aborted.");
            }

            // 2. Handle Empty Tables
            if (this.tuples.isEmpty()) {
                out.println("Table " + this.name + " is empty. Returning an empty result.");
                return new Table(name + "_MINUS_" + table2.name, attribute, domain, key);
            }
            if (table2.tuples.isEmpty()) {
                out.println("Table " + table2.name + " is empty. Returning original table.");
                return this;
            }

            // 3. Create the Result Table
            Table result = new Table(name + "_MINUS_" + table2.name, attribute, domain, key);

            // 4. Convert table2 tuples to a HashSet for fast lookup
            Set<List<Comparable>> sSet = new HashSet<>();
            for (Comparable[] tuple : table2.tuples) {
                sSet.add(Arrays.asList(tuple)); // Store tuple as a list for proper comparison
            }

            // 5. Add Tuples from R that are NOT in S (Checking Full Row Using Arrays.equals)
            for (Comparable[] tuple : this.tuples) {
                if (!sSet.contains(Arrays.asList(tuple))) {  // Properly check full row match
                    result.tuples.add(tuple);
                }
            }

            return result;

        } catch (Exception e) {
            out.println("Error in MINUS operation: " + e.getMessage());
            e.printStackTrace();
            return null; // Return null if operation fails
        }
    } // minus

    /************************************************************************************
     * Join this table and table2 by performing an "equi-join".  Tuples from both tables
     * are compared requiring attributes1 to equal attributes2.  Disambiguate attribute
     * names by appending "2" to the end of any duplicate attribute name.  Implement using
     * a NESTED LOOP JOIN ALGORITHM.
     *
     * #usage movie.join ("studioName", "name", studio)
     *
     * @param attributes1  the attributes of this table to be compared (Foreign Key)
     * @param attributes2  the attributes of table2 to be compared (Primary Key)
     * @param table2       the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table join (String attributes1, String attributes2, Table table2)
    {
        out.println ("RA> " + name + ".join (" + attributes1 + ", " + attributes2 + ", " + table2.name + ")");

        var t_attrs = attributes1.split (" ");
        var u_attrs = attributes2.split (" ");
        var rows    = new ArrayList <Comparable []> ();

        if (t_attrs.length != u_attrs.length) {
            throw new IllegalArgumentException("Number of attributes in attributes1 and attributes2 must match.");
        }

        for (var thisRow : this.tuples) {
            for (var thatRow : table2.tuples) {
                boolean match = true;

                // Check if all corresponding attributes match
                for (int i = 0; i < t_attrs.length; i++) {
                    int thisIndex = col(t_attrs[i]);
                    int thatIndex = table2.col(u_attrs[i]);
                    if (thisIndex == -1 || thatIndex == -1) {
                        throw new IllegalArgumentException("Invalid attribute name: " + t_attrs[i] + " or " + u_attrs[i]);
                    }
                    if (!thisRow[thisIndex].equals(thatRow[thatIndex])) {
                        match = false;
                        break;
                    }
                }

                // If all attributes match, merge the rows
                if (match) {
                    var newRow = concat(thisRow, thatRow);
                    rows.add(newRow);
                }
            }
        }

        // Disambiguate duplicate attributes from table2
        var newAttributes = new ArrayList<String>(Arrays.asList(attribute));
        for (String attr : table2.attribute) {
            if (newAttributes.contains(attr)) {
                newAttributes.add(attr + "2");
            } else {
                newAttributes.add(attr);
            }
        }

        return new Table (name + count++, concat (attribute, table2.attribute),
                concat(domain, table2.domain), key, rows);
    } // join

    /************************************************************************************
     * Join this table and table2 by performing a "theta-join".  Tuples from both tables
     * are compared attribute1 <op> attribute2.  Disambiguate attribute names by appending "2"
     * to the end of any duplicate attribute name.  Implement using a Nested Loop Join algorithm.
     *
     * #usage movie.join ("studioName == name", studio)
     *
     * @param condition  the theta join condition
     * @param table2     the rhs table in the join operation
     * @return  a table with tuples satisfying the condition
     */
    public Table join (String condition, Table table2)
    {
        out.println ("RA> " + name + ".join (" + condition + ", " + table2.name + ")");

        var tokens = condition.split(" ");
        if (tokens.length != 3) {
            throw new IllegalArgumentException("Condition must be in the format 'attribute1 <op> attribute2'");
        }

        String attribute1 = tokens[0];
        String operator = tokens[1];
        String attribute2 = tokens[2];

        int index1 = col(attribute1);
        int index2 = table2.col(attribute2);

        if (index1 == -1 || index2 == -1) {
            throw new IllegalArgumentException("Invalid attributes in condition: " + attribute1 + ", " + attribute2);
        }

        var rows = new ArrayList<Comparable[]>();

        // Perform the theta-join using a nested loop
        for (var thisRow : this.tuples) {
            for (var thatRow : table2.tuples) {
                if (satifies(thisRow, index1, operator, thatRow[index2])) {
                    var newRow = concat(thisRow, thatRow);
                    rows.add(newRow);
                }
            }
        }

        // Disambiguate duplicate attribute names from table2
        var newAttributes = new ArrayList<String>(Arrays.asList(attribute));
        for (String attr : table2.attribute) {
            if (newAttributes.contains(attr)) {
                newAttributes.add(attr + "2");
            } else {
                newAttributes.add(attr);
            }
        }

        var newDomains = concat(domain, table2.domain);
        return new Table(name + count++, newAttributes.toArray(new String[0]), newDomains, key, rows);
    } // join

    /************************************************************************************
     * Join this table and table2 by performing an "equi-join".  Same as above equi-join,
     * but implemented using an INDEXED JOIN algorithm.
     *
     * @param attributes1  the attributes of this table to be compared (Foreign Key)
     * @param attributes2  the attributes of table2 to be compared (Primary Key)
     * @param table2       the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table i_join (String attributes1, String attributes2, Table table2)
    {
        //  T O   B E   I M P L E M E N T E D  - Project 2

        return null;

    } // i_join

    /************************************************************************************
     * Join this table and table2 by performing an NATURAL JOIN.  Tuples from both tables
     * are compared requiring common attributes to be equal.  The duplicate column is also
     * eliminated.
     *
     * #usage movieStar.join (starsIn)
     *
     * @param table2  the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table join (Table table2)
    {
        out.println ("RA> " + name + ".join (" + table2.name + ")");

        var commonAttributes = new ArrayList<String>();
        var commonIndicesThis = new ArrayList<Integer>();
        var commonIndicesOther = new ArrayList<Integer>();
        for (int i = 0; i < attribute.length; i++) {
            for (int j = 0; j < table2.attribute.length; j++) {
                if (attribute[i].equals(table2.attribute[j])) {
                    commonAttributes.add(attribute[i]);
                    commonIndicesThis.add(i);
                    commonIndicesOther.add(j);
                }
            }
        }

        var rows = new ArrayList<Comparable[]>();
        for (var thisRow : this.tuples) {
            for (var thatRow : table2.tuples) {
                boolean match = true;

                // Check if all common attributes match
                for (int k = 0; k < commonIndicesThis.size(); k++) {
                    int thisIndex = commonIndicesThis.get(k);
                    int thatIndex = commonIndicesOther.get(k);

                    if (!thisRow[thisIndex].equals(thatRow[thatIndex])) {
                        match = false;
                        break;
                    }
                }

                // If all common attributes match, merge the rows
                if (match) {
                    var newRow = new ArrayList<Comparable>();
                    // Add attributes from the current table
                    Collections.addAll(newRow, thisRow);

                    // Add attributes from the other table, excluding duplicates
                    for (int j = 0; j < thatRow.length; j++) {
                        if (!commonIndicesOther.contains(j)) {
                            newRow.add(thatRow[j]);
                        }
                    }

                    rows.add(newRow.toArray(new Comparable[0]));
                }
            }
        }

        var newAttributes = new ArrayList<String>(Arrays.asList(attribute));
        var newDomains = new ArrayList<>(Arrays.asList(domain));

        for (int i = 0; i < table2.attribute.length; i++) {
            if (!commonAttributes.contains(table2.attribute[i])) {
                newAttributes.add(table2.attribute[i]);
                newDomains.add(table2.domain[i]);
            }
        }

        return new Table(name + count++, newAttributes.toArray(new String[0]), newDomains.toArray(new Class[0]), key, rows);

    } // join

    /************************************************************************************
     * Return the column position for the given attribute name or -1 if not found.
     *
     * @param attr  the given attribute name
     * @return  a column position
     */
    public int col (String attr)
    {
        for (var i = 0; i < attribute.length; i++) {
           if (attr.equals (attribute [i])) return i;
        } // for

        return -1;       // -1 => not found
    } // col

    /************************************************************************************
     * Insert a tuple to the table.
     *
     * #usage movie.insert ("Star_Wars", 1977, 124, "T", "Fox", 12345)
     *
     * @param tup  the array of attribute values forming the tuple
     * @return  the insertion position/index when successful, else -1
     */
    public int insert (Comparable [] tup)
    {
        out.println ("DML> insert into " + name + " values (" + Arrays.toString (tup) + ")");

        if (!typeCheck(tup)) {
            return -1;
        }

        KeyType primaryKey = extractPrimaryKey(tup);
        if (index.containsKey(primaryKey)) {
            throw new IllegalArgumentException("Duplicate primary key: " + primaryKey);
        }

        tuples.add(tup);

        if (mType != MapType.NO_MAP) {
            index.put(primaryKey, tup);
        }

        for (var entry : secondaryIndices.entrySet()) {
            String columnName = entry.getKey();  // The column being indexed
            Map<Comparable, List<Comparable[]>> columnIndex = entry.getValue();  // The index map for that column

            int colIdx = col(columnName);
            if (colIdx == -1) {
                throw new IllegalStateException("Column " + columnName + " is indexed but does not exist in table " + name);
            }

            Comparable key = tup[colIdx];  // Get the value of the indexed column
            columnIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(tup);  // Add the tuple to the index
        }

        return tuples.size() - 1;
    } // insert

    /************************************************************************************
     * Get the tuple at index position i.
     *
     * @param i  the index of the tuple being sought
     * @return  the tuple at index position i
     */
    public Comparable [] get (int i)
    {
        return tuples.get (i);
    } // get

    /************************************************************************************
     * Get the name of the table.
     *
     * @return  the table's name
     */
    public String getName ()
    {
        return name;
    } // getName

    /************************************************************************************
     * Print tuple tup.
     * @param tup  the array of attribute values forming the tuple
     */
    public void printTup (Comparable [] tup)
    {
        out.print ("| ");
        for (var attr : tup) out.printf ("%15s", attr);
        out.println (" |");
    } // printTup

    /************************************************************************************
     * Print this table.
     */
    public void print ()
    {
        out.println ("\n Table " + name);
        out.print ("|-");
        out.print ("---------------".repeat (attribute.length));
        out.println ("-|");
        out.print ("| ");
        for (var a : attribute) out.printf ("%15s", a);
        out.println (" |");
        out.print ("|-");
        out.print ("---------------".repeat (attribute.length));
        out.println ("-|");
        for (var tup : tuples) printTup (tup);
        out.print ("|-");
        out.print ("---------------".repeat (attribute.length));
        out.println ("-|");
    } // print

    /************************************************************************************
     * Print this table's index (Map).
     */
    public void printIndex ()
    {
        out.println ("\n Index for " + name);
        out.println ("-------------------");
        if (mType != MapType.NO_MAP) {
            for (var e : index.entrySet ()) {
                out.println (e.getKey () + " -> " + Arrays.toString (e.getValue ()));
            } // for
        } // if
        out.println ("-------------------");
    } // printIndex

    /************************************************************************************
     * Load the table with the given name into memory. 
     *
     * @param name  the name of the table to load
     */
    public static Table load (String name)
    {
        Table tab = null;
        try {
            ObjectInputStream ois = new ObjectInputStream (new FileInputStream (DIR + name + EXT));
            tab = (Table) ois.readObject ();
            ois.close ();
        } catch (IOException ex) {
            out.println ("load: IO Exception");
            ex.printStackTrace ();
        } catch (ClassNotFoundException ex) {
            out.println ("load: Class Not Found Exception");
            ex.printStackTrace ();
        } // try
        return tab;
    } // load

    /************************************************************************************
     * Save this table in a file.
     */
    public void save ()
    {
        try {
            var oos = new ObjectOutputStream (new FileOutputStream (DIR + name + EXT));
            oos.writeObject (this);
            oos.close ();
        } catch (IOException ex) {
            out.println ("save: IO Exception");
            ex.printStackTrace ();
        } // try
    } // save

    //----------------------------------------------------------------------------------
    // Private Methods
    //----------------------------------------------------------------------------------

    /************************************************************************************
     * Determine whether the two tables (this and table2) are compatible, i.e., have
     * the same number of attributes each with the same corresponding domain.
     *
     * @param table2  the rhs table
     * @return  whether the two tables are compatible
     */
    private boolean compatible (Table table2)
    {
        if (domain.length != table2.domain.length) {
            out.println ("compatible ERROR: table have different arity");
            return false;
        } // if
        for (var j = 0; j < domain.length; j++) {
            if (domain [j] != table2.domain [j]) {
                out.println ("compatible ERROR: tables disagree on domain " + j);
                return false;
            } // if
        } // for
        return true;
    } // compatible

    /************************************************************************************
     * Match the column and attribute names to determine the domains.
     *
     * @param column  the array of column names
     * @return  an array of column index positions
     */
    private int [] match (String [] column)
    {
        int [] colPos = new int [column.length];

        for (var j = 0; j < column.length; j++) {
            var matched = false;
            for (var k = 0; k < attribute.length; k++) {
                if (column [j].equals (attribute [k])) {
                    matched = true;
                    colPos [j] = k;
                } // for
            } // for
            if ( ! matched) out.println ("match: domain not found for " + column [j]);
        } // for

        return colPos;
    } // match

    /************************************************************************************
     * Extract the attributes specified by the column array from tuple t.
     *
     * @param t       the tuple to extract from
     * @param column  the array of column names
     * @return  a smaller tuple extracted from tuple t 
     */
    private Comparable [] extract (Comparable [] t, String [] column)
    {
        var tup    = new Comparable [column.length];
        var colPos = match (column);
        for (var j = 0; j < column.length; j++) tup [j] = t [colPos [j]];
        return tup;
    } // extract

    /************************************************************************************
     * Check the size of the tuple (number of elements in array) as well as the type of
     * each value to ensure it is from the right domain. 
     *
     * @param t  the tuple as a array of attribute values
     * @return  whether the tuple has the right size and values that comply
     *          with the given domains
     */
    private boolean typeCheck (Comparable [] t)
    {
        if (t.length != domain.length) {
            out.println("ERROR: Tuple length mismatch. Expected " + domain.length + " but got " + t.length);
            return false;
        }

        for (int i = 0; i < t.length; i++) {
            if (!domain[i].isInstance(t[i])) {  // Ensure the type matches the expected domain
                out.println("ERROR: Type mismatch at column " + attribute[i] +
                        ". Expected " + domain[i].getSimpleName() +
                        " but got " + (t[i] == null ? "null" : t[i].getClass().getSimpleName()));
                return false;
            }
        }
        return true;
    } // typeCheck

    /************************************************************************************
     * Find the classes in the "java.lang" package with given names.
     *
     * @param className  the array of class name (e.g., {"Integer", "String"})
     * @return  an array of Java classes
     */
    private static Class [] findClass (String [] className)
    {
        var classArray = new Class [className.length];

        for (var i = 0; i < className.length; i++) {
            try {
                classArray [i] = Class.forName ("java.lang." + className [i]);
            } catch (ClassNotFoundException ex) {
                out.println ("findClass: " + ex);
            } // try
        } // for

        return classArray;
    } // findClass

    /************************************************************************************
     * Extract the corresponding domains.
     *
     * @param colPos  the column positions to extract.
     * @param group   where to extract from
     * @return  the extracted domains
     */
    private Class [] extractDom (int [] colPos, Class [] group)
    {
        var obj = new Class [colPos.length];

        for (var j = 0; j < colPos.length; j++) {
            obj [j] = group [colPos [j]];
        } // for

        return obj;
    } // extractDom

    /**
     * Creates a deep copy of this Table.
     * Copies the schema, all tuples, and rebuilds the primary index.
     *
     * @return a new Table object that is a deep copy of this Table.
     */
    public Table copy() {
        // Create a new Table with the same schema (and a new name if desired)
        Table copy = new Table(this.name + "_copy", this.attribute, this.domain, this.key);

        // Deep copy each tuple
        for (Comparable[] row : this.tuples) {
            copy.tuples.add(Arrays.copyOf(row, row.length));
        }

        // Rebuild the primary index
        for (Comparable[] row : copy.tuples) {
            KeyType pk = copy.extractPrimaryKey(row);
            copy.index.put(pk, row);
        }

        // (Optional) If you need to copy secondary indices, do it here.
        // For now, we can leave secondaryIndices empty or rebuild them as needed.

        return copy;
    }

} // Table

