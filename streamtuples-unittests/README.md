The unit tests have been separated from the source project
in order to be able to use Java 10 syntax in tests while having the
library be compiled for Java 8.

This is a limitation in `javac` where "-source X" and "-target Y" cannot have
different values for X and Y.


