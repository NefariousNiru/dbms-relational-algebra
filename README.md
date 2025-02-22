
# Project 2

This project implements a basic relational database system that requires `JDK 23` for managing movie-related data. The system supports operations such as **selection**, **projection**, **union**, **minus**, and **join**, allowing users to query and manipulate the database tables.

---
# **Project 2 README: Database Management System Implementation**

## **Overview**
This project implements a simple database management system with indexing, relational algebra operations, and query optimizations. The system includes **efficient indexing techniques**, **query execution improvements**, and **various optimizations** to speed up operations like `select`, `join`, `union`, and `minus`.

---
## **Key Changes and Features**

### **1. KeyType.java**
- **Modified `compareTo` Method**:
    - Improved comparison logic to prevent incorrect comparisons due to type mismatches.
    - Ensured **consistent ordering** for composite keys.
- **Enhanced Hash Function**:
    - Prevented negative values when computing hash codes.
    - Ensured uniform distribution of hash values.

---
### **2. LinHashMap.java**
- **Serialization Support**:
    - Made **buckets serializable** to enable saving and restoring data.
- **Implemented `entrySet()`**:
    - Allows **iteration over key-value pairs** in the hash map.
- **Implemented `split()` Functionality**:
    - Ensures dynamic reallocation of entries when a **bucket overflows**.

---
### **3. MovieDB.java**
Implemented **various test cases** to validate the correctness of indexing and relational operations.

#### **Test Cases Implemented:**
- **Indexed Join:**
  ```java
  test_indexed_join(movie, studio);
  ```
- **Indexed Select:**
  ```java
  test_indexed_select(movie, movieStar);
  ```
- **Union Operation:**
  ```java
  test_indexed_union(movie, cinema);
  ```
- **Minus Operation:**
  ```java
  test_indexed_minus(movie, cinema);
  ```
- **Index Methods Testing:**
  ```java
  test_index_methods(movie);
  ```
- **Projection Operation:**
  ```java
  test_indexed_project(movie, cinema, movieStar, starsIn, studio, movieExec);
  ```

---
### **4. Table.java**

**Enhancements to Table Operations:**
- **`Select` - Implement Indexed Select**:
    - Optimized by using **primary and secondary indices** for faster lookups.
- **`Project` - Eliminate Duplicate Tuples Efficiently Using Indexing**:
    - Uses **hash-based indexing** to quickly detect and remove duplicate tuples.
- **`Union` - Eliminate Duplicate Tuples Efficiently Using Indexing**:
    - **Index-based deduplication** for efficient set union operations.
- **`Minus` - For Each Tuple in R, Use Index to See if It is in S**:
    - Index-based filtering to remove matching tuples.
- **`Join` - Implement Indexed Join**:
    - Uses **secondary and primary indices** for efficient joins.

---
### **5. Indexing Improvements**
- **Secondary Indices: Implemented `secondaryIndices (Map<String, Map<Comparable, List<Comparable[]>>>)` to support efficient lookups and retrievals for non-primary key attributes, enabling optimized indexed selects, joins, and projections.**
- **Support for Both Unique and Non-Unique Indices**.
- **By Default, Creates a Unique Index for the Primary Key**.
- **For Other Indices, Use Explicit Creation Methods:**
  ```java
  create_index("column_name");
  create_unique_index("column_name");
  drop_index("column_name");
  ```
  


---

## Project Structure

```
MovieDB/
├── src/
│   ├── MovieDB.java     # Main program with test cases for database features
│   ├── MovieDB2.java    # Loads the Movie Database
│   ├── Table.java       # Core implementation of the Table class and operations
│   ├── LinHashMap.java  # Core implementation of the Linear HashinG based Map
│   └── KeyType.java     # (Optional) Helper class for key-based selection
├── store/               # Directory to save table data
└── README.md            # Project documentation
```

## Usage

1. Clone the repository [link](https://github.com/NefariousNiru/dbms-relational-algebra) or create a new project with the provided structure.
2. Create the `store` directory if not exists by referring to the folder structure above.
3. Modify `Table.java` to contribute changes.
4. Compile and run `MovieDB.java` to execute the test cases for the implemented features.
5. Verify output in the console

## **Conclusion**
This project enhances the performance and functionality of relational database operations by implementing efficient **indexing, query optimization, and serialization support**. The improvements significantly **reduce lookup times, optimize joins, and provide faster set operations** using indexing techniques.
