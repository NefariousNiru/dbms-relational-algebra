
# Project 3

This project does a query performance metrics for running a join query.

---

# Steps to run

- Run Project3.java to performance test
- Test on three different maps to find the times in project3.log file 
- The project uses a non-unique index on sid and cid for the takes table
- We don't create unique-indexes on Primary Keys are they are automatically indexed during insert.
- If there are duplicate primary keys generated then re-run the code as tuples are generated randomly. We expanded the random generation space from 1M to 100M but it still might encounter duplicates sometimes.

---

## Project Structure

```
MovieDB/
├── src/
│   ├── MovieDB.java                  # Main program with test cases for database features
│   ├── MovieDB2.java                 # Loads the Movie Database
│   ├── Table.java                    # Core implementation of the Table class and operations
│   ├── LinHashMap.java               # Core implementation of the Linear HashinG based Map
│   └── KeyType.java                  # (Optional) Helper class for key-based selection
│   ├── Project3.java                 # Loads the Movie Database
│   ├── TestTupleGenerator.java       # Core implementation of the Table class and operations
│   ├── TupleGenerator.java           # Core implementation of the Linear HashinG based Map
│   └── TupleGeneratorImpl.java       # (Optional) Helper class for key-based selection
├── store/               # Directory to save table data
└── README.md            # Project documentation
└── project3.log         # Log for Query Time
```

## Usage

1. Clone the repository [link](https://github.com/NefariousNiru/dbms-relational-algebra) or create a new project with the provided structure.
2. Create the `store` directory if not exists by referring to the folder structure above.
3. Modify `Table.java` to contribute changes.
4. Compile and run `Project3.java` to execute the performance for the implemented query.
5. Verify output in the console
