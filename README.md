
# Relational Operator Mock Database

This project implements a basic relational database system that requires `JDK 23` for managing movie-related data. The system supports operations such as **selection**, **projection**, **union**, **minus**, and **join**, allowing users to query and manipulate the database tables.

---

## Project Structure

```
MovieDB/
├── src/
│   ├── MovieDB.java     # Main program with test cases for database features
│   ├── MovieDB2.java    # Loads the Movie Database
│   ├── Table.java       # Core implementation of the Table class and operations
│   └── KeyType.java     # (Optional) Helper class for key-based selection
├── store/               # Directory to save table data
└── README.md            # Project documentation
```

---

## Features

### 1. **Select**

- Filters rows based on a given condition.
- Supports:
    - Lambda expressions, e.g., `t -> t[col].equals(value)`
    - String conditions, e.g., `"year < 1980"`
- **Example:**
  ```java
  var t_select = movie.select("year < 1980");
  t_select.print();
  ```

### 2. **Project (To Be Implemented)**

- Returns a new table with only specified columns.
- **Example:**
  ```java
  var t_project = movie.project("title year");
  t_project.print();
  ```

### 3. **Union (To Be Implemented)**

- Combines rows from two tables, eliminating duplicates.
- **Example:**
  ```java
  var t_union = movie.union(cinema);
  t_union.print();
  ```

### 4. **Minus (To Be Implemented)**

- Subtracts rows in one table from another.
- **Example:**
  ```java
  var t_minus = movie.minus(cinema);
  t_minus.print();
  ```

### 5. **Join (To Be Implemented)**

- Combines rows from two tables based on a common column.
- Supports:
    - **Equi-Join:** Joins based on specific column equality.
      ```java
      var t_join = movie.join("studioName", "name", studio);
      t_join.print();
      ```
    - **Natural Join:** Joins based on columns with matching names and types.
      ```java
      var t_join2 = movie.join(cinema);
      t_join2.print();
      ```

---

## Data Persistence

- All tables can be saved to the `store/` directory using the `save()` method.
- **Example:**
  ```java
  movie.save();
  cinema.save();
  ```

---

## Usage

1. Clone the repository or create a new project with the provided structure.
2. Create the `store` directory if not exists by referring to the folder structure above.
3. Modify `Table.java` to contribute changes.
4. Compile and run `MovieDB.java` to execute the test cases for the implemented features.
5. Verify output in the console and check the `store/` directory for saved tables.
6. After tests, commit changes to a new branch that begins with `feature-<name>`. Add the name of the feature you worked on.
7. Issue a Pull Request. 