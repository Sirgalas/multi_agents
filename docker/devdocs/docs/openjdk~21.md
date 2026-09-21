# Java 21 LTS Modern Conventions & Idioms

- **Data Modeling**: Use record classes for immutable data carriers and DTOs.
- **Type Hierarchies**: Use sealed interface and sealed class to restrict type hierarchies and enable compiler-verified exhaustive pattern matching.
- **Pattern Matching**: Utilize pattern matching for instanceof and pattern matching for switch statements and expressions.
- **Concurrency**: Leverage Virtual Threads (Thread.ofVirtual() / Executors.newVirtualThreadPerTaskExecutor()) for high-throughput I/O-bound workloads.
- **Collections**: Use sequenced collections (getFirst(), getLast(), reversed()) and immutable factory methods (List.of(), Set.of(), Map.of()).
- **Null Safety**: Use java.util.Optional strictly as a method return type for query results, never as parameters or entity fields.
