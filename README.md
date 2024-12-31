
# SimpleDB - Lightweight Database Implementation

## Overview
This repository contains a Java-based implementation of a lightweight database system called **SimpleDB**. It serves as an educational tool to understand the fundamental principles of database systems, including query processing, transaction management, and storage structures. The project includes both source code and test cases to verify functionality.

## Features
- **Heap File Storage**: Implements storage and retrieval of tuples using heap files.
- **Buffer Pool**: Manages in-memory pages for efficient data access.
- **Query Execution**: Includes sequential scans and predicate evaluations.
- **Transaction Support**: Basic transaction management with ACID properties.
- **Catalog Management**: Metadata management for database schema.
- **JUnit Test Coverage**: Comprehensive test cases to validate functionality.

## Project Structure
```
simpledb/
├── src/                # Source code files
│   └── simpledb/       # Core database classes (e.g., HeapFile, BufferPool, Tuple)
├── test/               # Test cases
│   └── simpledb/       # Unit and system tests
├── lib/                # External libraries (e.g., JUnit)
├── build.xml           # Build configuration for Apache Ant
├── .idea/              # IntelliJ project configuration files
├── .metadata/          # Eclipse project metadata
└── README.md           # Project documentation (this file)
```

## Installation and Setup
### Prerequisites
- Java Development Kit (JDK) 8 or higher.
- Apache Ant (optional, for automated build).

### Compilation
Use the provided `build.xml` to compile the project:
```bash
ant compile
```
Alternatively, you can use an IDE like IntelliJ or Eclipse to import the project and build it.

### Running the Tests
To run all test cases:
```bash
ant test
```
Individual tests can also be executed from your IDE or command line.

## Key Components
- **HeapFile**: Implements storage of tuples in heap pages.
- **BufferPool**: Manages page caching and replacement.
- **Catalog**: Handles metadata for tables and schemas.
- **Tuple**: Represents a row in a table.
- **Transaction Management**: Supports concurrent transactions with isolation.

## Example Usage
### Creating and Querying a Table
1. Define a schema for your table using `TupleDesc`.
2. Create a `HeapFile` to store tuples.
3. Use `BufferPool` to read and write pages.
4. Execute a sequential scan to retrieve tuples.

### Sample Code
```java
// Define a schema
Type[] types = new Type[]{Type.INT_TYPE, Type.STRING_TYPE};
String[] fieldNames = new String[]{"id", "name"};
TupleDesc td = new TupleDesc(types, fieldNames);

// Create a heap file
HeapFile heapFile = new HeapFile(new File("students.dat"), td);

// Insert a tuple
Tuple tuple = new Tuple(td);
tuple.setField(0, new IntField(1));
tuple.setField(1, new StringField("Alice", 10));
heapFile.insertTuple(transactionId, tuple);

// Scan the table
SeqScan seqScan = new SeqScan(transactionId, heapFile.getId(), "students");
while (seqScan.hasNext()) {
    Tuple result = seqScan.next();
    System.out.println(result);
}
```

## Limitations
- Simplified transaction model.
- Not optimized for large-scale databases.
- No support for complex queries or joins.

## License
This project is licensed under the MIT License. See the `LICENSE` file for details.

## Author
Kavin Parakh
Drexel University
