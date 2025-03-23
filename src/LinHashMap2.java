
/************************************************************************************
 * @file LinHashMap2.java
 *
 * @author  John Miller
 *
 * compile javac --enable-preview --release 21 LinHashMap2.java
 * run     java --enable-preview LinHashMap2
 */

import java.io.*;
import java.lang.reflect.Array;
import static java.lang.System.out;
import java.util.*;
import java.util.function.Function;

/************************************************************************************
 * The `LinHashMap2` class provides hash maps that use the Linear Hashing
 * algorithm.
 * A hash table is created that is an expandable array-list of buckets.
 */
public class LinHashMap2<K, V>
        extends AbstractMap<K, V>
        implements Serializable, Cloneable, Map<K, V> {

    /**
     * The debug flag
     */
    private static final boolean DEBUG = true;

    /**
     * The number of slots (for key-value pairs) per bucket.
     */
    private static final int SLOTS = 4;

    /**
     * The threshold/upper bound on the load factor
     */
    private static final double THRESHOLD = 1.1;

    /**
     * The class for type K.
     */
    private final Class<K> classK;

    /**
     * The class for type V.
     */
    private final Class<V> classV;

    // -----------------------------------------------------------------------------------
    // Bucket inner class
    // -----------------------------------------------------------------------------------

    /********************************************************************************
     * The `Bucket` inner class defines buckets that are stored in the hash table.
     */
    private class Bucket implements Serializable {
        int keys; // number of active keys
        K[] key; // array of keys
        V[] value; // array of values
        Bucket next; // link to next bucket in chain

        @SuppressWarnings("unchecked")
        Bucket() {
            keys = 0;
            key = (K[]) Array.newInstance(classK, SLOTS);
            value = (V[]) Array.newInstance(classV, SLOTS);
            next = null;
        } // constructor

        V find(K k) {
            for (var j = 0; j < keys; j++)
                if (key[j].equals(k))
                    return value[j];
            return null;
        } // find

        void add(K k, V v) {
            key[keys] = k;
            value[keys] = v;
            keys += 1;
        } // add

        void print() {
            out.print("[ ");
            for (var j = 0; j < keys; j++)
                out.print(key[j] + " . ");
            out.println("]");
        } // print

    } // Bucket

    // -----------------------------------------------------------------------------------
    // Fields and constructors for Linear Hash Map class
    // -----------------------------------------------------------------------------------

    /**
     * The list of buckets making up the hash table.
     */
    private final List<Bucket> hTable;

    /**
     * The modulus for low resolution hashing
     */
    private int mod1;

    /**
     * The modulus for high resolution hashing
     */
    private int mod2;

    /**
     * The index of the next bucket to split.
     */
    private int isplit = 0;

    /**
     * Counter for the number buckets accessed (for performance testing).
     */
    private int count = 0;

    /**
     * The counter for the total number of keys in the LinHash Map
     */
    private int kCount = 0;

    /********************************************************************************
     * Construct a hash table that uses Linear Hashing.
     * 
     * @param classK the class for keys (K)
     * @param classV the class for values (V)
     */
    public LinHashMap2(Class<K> _classK, Class<V> _classV) {
        classK = _classK;
        classV = _classV;
        mod1 = 4; // initial size
        mod2 = 2 * mod1;
        hTable = new ArrayList<>();
        for (var i = 0; i < mod1; i++)
            hTable.add(new Bucket());
    } // constructor

    /********************************************************************************
     * Return the size (SLOTS * number of home buckets) of the hash table.
     * 
     * @return the size of the hash table
     */
    public int size() {
        return SLOTS * (mod1 + isplit);
    }

    /********************************************************************************
     * Return the load factor for the hash table.
     * 
     * @return the load factor
     */
    private double loadFactor() {
        return kCount / (double) size();
    }

    // -----------------------------------------------------------------------------------

    // Retrieve values or entry set
    // -----------------------------------------------------------------------------------

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * 
     * @return the set view of the map
     */
    public Set<Map.Entry<K, V>> entrySet() {
        var enSet = new HashSet<Map.Entry<K, V>>();

        for (var bucket : hTable) {
            for (var b = bucket; b != null; b = b.next) {
                for (var j = 0; j < b.keys; j++) {
                    enSet.add(new AbstractMap.SimpleEntry<>(b.key[j], b.value[j]));
                }
            }
        }

        return enSet;
    } // entrySet

    /********************************************************************************
     * Hash the key using the low resolution hash function.
     * 
     * @param key the key to hash
     * @return the location of the bucket chain containing the key-value pair
     */
    private int h(Object key) {
        return Math.abs(key.hashCode() % mod1);
    }

    /********************************************************************************
     * Hash the key using the high resolution hash function.
     * 
     * @param key the key to hash
     * @return the location of the bucket chain containing the key-value pair
     */
    private int h2(Object key) {
        return Math.abs(key.hashCode() % mod2);
    }

    /********************************************************************************
     * Given the key, look up the value in the hash table.
     * 
     * @param key the key used for look up
     * @return the value associated with the key
     */
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        var i = h(key); // Low-resolution hash
        if (i < isplit) {
            i = h2(key); // Use high-resolution hash for split buckets
        }
        return find((K) key, hTable.get(i % hTable.size()), true);
    }// get

    /********************************************************************************
     * Find the key in the bucket chain that starts with home bucket bh.
     * 
     * @param key    the key to find
     * @param bh     the given home bucket
     * @param by_get whether 'find' is called from 'get' (performance monitored)
     * @return the current value stored for the key
     */
    private V find(K key, Bucket bh, boolean by_get) {
        for (var b = bh; b != null; b = b.next) {
            if (by_get)
                count += 1;
            V result = b.find(key);
            if (result != null)
                return result;
        } // for
        return null;
    } // find

    // -----------------------------------------------------------------------------------
    // Put key-value pairs into the Linear Hash Map
    // -----------------------------------------------------------------------------------

    /********************************************************************************
     * Put the key-value pair in the hash table. Split the 'isplit' bucket chain
     * when the load factor is exceeded.
     * 
     * @param key   the key to insert
     * @param value the value to insert
     * @return the old/previous value, null if none
     */
    public V put(K key, V value) {
        var i = h(key); // hash to i-th bucket chain
        var bh = hTable.get(i); // start with home bucket
        var oldV = find(key, bh, false); // find old value associated with key

        // Build a string representation of the value
        String valueStr;
        if (value.getClass().isArray()) {
            StringBuilder sb = new StringBuilder("[");
            int length = java.lang.reflect.Array.getLength(value);
            for (int j = 0; j < length; j++) {
                sb.append(java.lang.reflect.Array.get(value, j));
                if (j < length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            valueStr = sb.toString();
        } else {
            valueStr = value.toString();
        }

        out.println("LinearHashMap.put: key " + key + ", h() = " + i + ", value = " + valueStr.toString());

        kCount += 1; // increment the key count
        var lf = loadFactor(); // compute the load factor
        if (DEBUG)
            out.println("put: load factor = " + lf);
        if (lf > THRESHOLD)
            split(); // split beyond THRESHOLD

        var b = bh;
        while (true) {
            if (b.keys < SLOTS) {
                b.add(key, value);
                return oldV;
            }
            if (b.next != null)
                b = b.next;
            else
                break;
        } // while

        var bn = new Bucket();
        bn.add(key, value);
        b.next = bn; // add new bucket at end of chain
        return oldV;
    } // put

    /********************************************************************************
     * Split bucket chain 'isplit' by creating a new bucket chain at the end of the
     * hash table and redistributing the keys according to the high resolution hash
     * function 'h2'. Increment 'isplit'. If current split phase is complete,
     * reset 'isplit' to zero, and update the hash functions.
     */
    private void split() {
        out.println("split: bucket chain " + isplit);

        // Add a new bucket at the end of the hash table
        hTable.add(new Bucket());

        // Get the bucket to split
        var bucketToSplit = hTable.get(isplit);

        // Collect all entries from the bucket chain
        var toRedistribute = new ArrayList<Map.Entry<K, V>>();
        for (var b = bucketToSplit; b != null; b = b.next) {
            for (var j = 0; j < b.keys; j++) {
                toRedistribute.add(new AbstractMap.SimpleEntry<>(b.key[j], b.value[j]));
            }
        }

        // Clear the original bucket chain
        bucketToSplit.keys = 0;
        bucketToSplit.next = null;

        // Redistribute entries
        for (var entry : toRedistribute) {
            var key = entry.getKey();
            var value = entry.getValue();
            var i = h2(key); // High-resolution hash
            var bh = hTable.get(i < hTable.size() ? i : h(key)); // Fallback to low-res if i exceeds current size
            var b = bh;
            while (true) {
                if (b.keys < SLOTS) {
                    b.add(key, value);
                    break;
                }
                if (b.next != null) {
                    b = b.next;
                } else {
                    var bn = new Bucket();
                    bn.add(key, value);
                    b.next = bn;
                    break;
                }
            }
        }

        // Update split index
        isplit++;
        if (isplit == mod1) {
            isplit = 0;
            mod1 = mod2;
            mod2 = 2 * mod1;
        }
    } // split

    // -----------------------------------------------------------------------------------
    // Print/show the Linear Hash Map
    // -----------------------------------------------------------------------------------

    /********************************************************************************
     * Print the linear hash table.
     */
    public void printT() {
        out.println("LinHashMap2");
        out.println("-------------------------------------------");

        for (var i = 0; i < hTable.size(); i++) {
            out.print("Bucket [ " + i + " ] = ");
            var j = 0;
            for (var b = hTable.get(i); b != null; b = b.next) {
                if (j > 0)
                    out.print(" \t\t --> ");
                b.print();
                j++;
            } // for
        } // for

        out.println("-------------------------------------------");
    } // printT

    // -----------------------------------------------------------------------------------
    // Main method for running/testing the Linear Hash Map
    // -----------------------------------------------------------------------------------

    /********************************************************************************
     * The main method used for testing. Also test for more keys and with RANDOMLY
     * true.
     * 
     * @param the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main(String[] args) {
        var totalKeys = 40;
        var RANDOMLY = false;

        LinHashMap2<Integer, Integer> ht = new LinHashMap2<>(Integer.class, Integer.class);
        if (args.length == 1)
            totalKeys = Integer.valueOf(args[0]);

        if (RANDOMLY) {
            var rng = new Random();
            for (var i = 1; i <= totalKeys; i += 2)
                ht.put(rng.nextInt(2 * totalKeys), i * i);
        } else {
            for (var i = 1; i <= totalKeys; i += 2)
                ht.put(i, i * i);
        } // if

        ht.printT();
        for (var i = 0; i <= totalKeys; i++) {
            out.println("key = " + i + ", value = " + ht.get(i));
        } // for
        out.println("-------------------------------------------");
        out.println("Average number of buckets accessed = " + (ht.count / (double) totalKeys));
    } // main

} // LinHashMap2
