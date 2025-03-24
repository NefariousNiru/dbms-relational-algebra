
/************************************************************************************
 * @file LinHashMap2.java
 *
 * @author  John Miller
 *
 * compile javac --enable-preview --release 21 LinHashMap2.java
 * run     java --enable-preview LinHashMap
 */

import java.io.*;
import java.lang.reflect.Array;
import static java.lang.System.out;
import java.util.*;

/************************************************************************************
 * The `LinHashMap` class provides hash maps that use the Linear Hashing algorithm.
 * A hash table is created that is an expandable array-list of buckets.
 */
public class LinHashMap <K, V>
       extends AbstractMap <K, V>
       implements Serializable, Cloneable, Map <K, V>
{
    /** The debug flag
     */
    private static final boolean DEBUG = false;

    /** The number of slots (for key-value pairs) per bucket.
     */
    private static final int SLOTS = 16;

    /** The threshold/upper bound on the load factor
     */
    private static final double THRESHOLD = 0.75;

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

//-----------------------------------------------------------------------------------
// Bucket inner class
//-----------------------------------------------------------------------------------

    /********************************************************************************
     * The `Bucket` inner class defines buckets that are stored in the hash table.
     */
    private class Bucket implements Serializable
    {
        int    keys;                                                         // number of active keys
        K []   key;                                                           // array of keys
        V []   value;                                                       // array of values
        Bucket next;                                                         // link to next bucket in chain

        @SuppressWarnings("unchecked")
        Bucket ()
        {
            keys = 0;
            key   = (K []) Array.newInstance (classK, SLOTS);
            value = (V []) Array.newInstance (classV, SLOTS);
            next  = null;
        } // constructor

        V find (K k)
        {
            for (var j = 0; j < keys; j++) if (key[j].equals (k)) return value[j];
            return null;
        } // find

        void add (K k, V v)
        {
            key[keys]   = k;
            value[keys] = v;
            keys += 1;
        } // add

        void print ()
        {
            out.print ("[ " );
            for (var j = 0; j < keys; j++) out.print (key[j] + " . ");
            out.println ("]" );
        } // print

    } // Bucket

//-----------------------------------------------------------------------------------
// Fields and constructors for Linear Hash Map class
//-----------------------------------------------------------------------------------

    /** The list of buckets making up the hash table.
     */
    private final List <Bucket> hTable;

    /** The modulus for low resolution hashing
     */
    private int mod1;

    /** The modulus for high resolution hashing
     */
    private int mod2;

    /** The index of the next bucket to split.
     */
    private int isplit = 0;

    /** Counter for the number buckets accessed (for performance testing).
     */
    private int count = 0;

    /** The counter for the total number of keys in the LinHash Map
     */
    private int kCount = 0;

    /********************************************************************************
     * Construct a hash table that uses Linear Hashing.
     * @param _classK  the class for keys (K)
     * @param _classV  the class for values (V)
     */
    public LinHashMap (Class <K> _classK, Class <V> _classV)
    {
        classK = _classK;
        classV = _classV;
        mod1   = 4;                                                          // initial size
        mod2   = 2 * mod1;
        hTable = new ArrayList <> ();
        for (var i = 0; i < mod1; i++) hTable.add (new Bucket ());
    } // constructor

    /********************************************************************************
     * Return the size (SLOTS * number of home buckets) of the hash table. 
     * @return  the size of the hash table
     */
    public int size () { return SLOTS * (mod1 + isplit); }

    /********************************************************************************
     * Return the load factor for the hash table.
     * @return  the load factor
     */
    private double loadFactor () { return kCount / (double) size (); }

//-----------------------------------------------------------------------------------
// Retrieve values or entry set
//-----------------------------------------------------------------------------------

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set <Map.Entry <K, V>> entrySet ()
    {
        var enSet = new HashSet <Map.Entry <K, V>> ();

        for (var bucket : hTable) {
            for (var b = bucket; b != null; b = b.next) {
                for (int j = 0; j < b.keys; j++) {
                    enSet.add(Map.entry(b.key[j], b.value[j]));
                }
            }
        }

        return enSet;
    } // entrySet

    /********************************************************************************
     * Hash the key using the low resolution hash function.
     * @param key  the key to hash
     * @return  the location of the bucket chain containing the key-value pair
     */
    private int h (Object key) { return Math.abs(key.hashCode() % mod1);  }

    /********************************************************************************
     * Hash the key using the high resolution hash function.
     * @param key  the key to hash
     * @return  the location of the bucket chain containing the key-value pair
     */
    private int h2 (Object key) { return Math.abs(key.hashCode () % mod2); }

    /********************************************************************************
     * Given the key, look up the value in the hash table.
     * @param key  the key used for look up
     * @return  the value associated with the key
     */
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        int i = h(key);

        // If we've split past this bucket, use the higher resolution hash
        if (i < isplit) {
            i = h2(key) % hTable.size();
        }

        Bucket bh = hTable.get(i);

        // Directly search in the bucket chain
        for (Bucket b = bh; b != null; b = b.next) {
            count++;  // Count bucket accesses
            for (int j = 0; j < b.keys; j++) {
                if (b.key[j].equals(key)) {
                    return b.value[j];
                }
            }
        }

        return null;  // Key not found
    }

    /********************************************************************************
     * Find the key in the bucket chain that starts with home bucket bh.
     * @param key     the key to find
     * @param bh      the given home bucket
     * @param by_get  whether 'find' is called from 'get' (performance monitored)
     * @return  the current value stored for the key
     */
    private V find (K key, Bucket bh, boolean by_get)
    {
        for (var b = bh; b != null; b = b.next) {
            if (by_get) count += 1;
            V result = b.find (key);
            if (result != null) return result;
        } // for
        return null;
    } // find

//-----------------------------------------------------------------------------------
// Put key-value pairs into the Linear Hash Map
//-----------------------------------------------------------------------------------

    /********************************************************************************
     * Put the key-value pair in the hash table.  Split the 'isplit' bucket chain
     * when the load factor is exceeded.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  the old/previous value, null if none
     */
    public V put(K key, V value) {
        // First check if key already exists
        V oldValue = null;

        // Get the appropriate hash bucket
        int i = h(key);
        if (i < isplit) {
            i = h2(key) % hTable.size();
        }

        Bucket bh = hTable.get(i);

        // Search for existing key
        boolean keyExists = false;
        for (Bucket b = bh; b != null && !keyExists; b = b.next) {
            for (int j = 0; j < b.keys; j++) {
                if (b.key[j].equals(key)) {
                    oldValue = b.value[j];
                    b.value[j] = value;
                    keyExists = true;
                    break;
                }
            }
        }

        // If key doesn't exist, add it
        if (!keyExists) {
            kCount++;

            // Check if we need to split before adding
            double lf = loadFactor();
            if (DEBUG) out.println("put: load factor = " + lf);
            if (lf > THRESHOLD) {
                split();

                // Recalculate bucket after split
                i = h(key);
                if (i < isplit) {
                    i = h2(key) % hTable.size();
                }
                bh = hTable.get(i);
            }

            // Find a bucket with space
            Bucket b = bh;
            while (b.keys >= SLOTS && b.next != null) {
                b = b.next;
            }

            if (b.keys < SLOTS) {
                b.add(key, value);
            } else {
                Bucket bn = new Bucket();
                bn.add(key, value);
                b.next = bn;
            }
        }

        return oldValue;
    }

    /********************************************************************************
     * Split bucket chain 'isplit' by creating a new bucket chain at the end of the
     * hash table and redistributing the keys according to the high resolution hash
     * function 'h2'.  Increment 'isplit'.  If current split phase is complete,
     * reset 'isplit' to zero, and update the hash functions.
     */
    /**
     * Modified split() method with improved efficiency
     */
    private void split() {
        if (DEBUG) out.println("split: bucket chain " + isplit);

        // Add a new bucket at the end of the hash table
        hTable.add(new Bucket());
        int newIndex = hTable.size() - 1;

        // Get the bucket chain that needs to be split
        Bucket oldBucketChain = hTable.get(isplit);

        // Replace with empty bucket
        hTable.set(isplit, new Bucket());

        // Temp collection to hold all entries from the chain
        List<Map.Entry<K, V>> entries = new ArrayList<>();

        // Collect all entries from the chain
        for (Bucket b = oldBucketChain; b != null; b = b.next) {
            for (int j = 0; j < b.keys; j++) {
                entries.add(Map.entry(b.key[j], b.value[j]));
            }
        }

        // Redistribute all entries
        for (Map.Entry<K, V> entry : entries) {
            K key = entry.getKey();
            V val = entry.getValue();

            // Compute new hash location using high-resolution hash
            int newHash = h2(key);
            int index = newHash % hTable.size();

            Bucket targetBucket = hTable.get(index);

            // Find a bucket with space or create a new one
            Bucket b = targetBucket;
            while (b.keys >= SLOTS && b.next != null) {
                b = b.next;
            }

            if (b.keys < SLOTS) {
                b.add(key, val);
            } else {
                Bucket bn = new Bucket();
                bn.add(key, val);
                b.next = bn;
            }
        }

        // Update the split pointer
        isplit++;
        if (isplit == mod1) {
            isplit = 0;
            mod1 = mod2;
            mod2 = 2 * mod1;
        }
    }

    // split

//-----------------------------------------------------------------------------------
// Print/show the Linear Hash Map
//-----------------------------------------------------------------------------------

    /********************************************************************************
     * Print the linear hash table.
     */
    public void printT ()
    {
        if (DEBUG) out.println ("LinHashMap");
        if (DEBUG) out.println ("-------------------------------------------");

        for (var i = 0; i < hTable.size (); i++) {
            if (DEBUG) out.print ("Bucket [ " + i + " ] = ");
            var j = 0;
            for (var b = hTable.get (i); b != null; b = b.next) {
                if (j > 0) out.print (" \t\t --> ");
                b.print ();
                j++;
            } // for
        } // for

        if (DEBUG) out.println ("-------------------------------------------");
    } // printT

//-----------------------------------------------------------------------------------
// Main method for running/testing the Linear Hash Map
//-----------------------------------------------------------------------------------
 
    /********************************************************************************
     * The main method used for testing.  Also test for more keys and with RANDOMLY true.
     * @param  the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        var totalKeys = 40;
        var RANDOMLY  = false;

        LinHashMap <Integer, Integer> ht = new LinHashMap <> (Integer.class, Integer.class);
        if (args.length == 1) totalKeys = Integer.valueOf (args [0]);

        if (RANDOMLY) {
            var rng = new Random ();
            for (var i = 1; i <= totalKeys; i += 2) ht.put (rng.nextInt (2 * totalKeys), i * i);
        } else {
            for (var i = 1; i <= totalKeys; i += 2) ht.put (i, i * i);
        } // if

        ht.printT ();
        for (var i = 0; i <= totalKeys; i++) {
            out.println ("key = " + i + ", value = " + ht.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of buckets accessed = " + (ht.count / (double) totalKeys));
    } // main

} // LinHashMap

