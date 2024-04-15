# AppEngine Performance Testing

This is a benchmark to test the performance improvements of the work done in AppEngine to bypass the RPC layer.

The benchmark is written as a parameterized junit test and the results are output to a directory called `output` under the project root directory.

## Local Testing

### Latency
- 23% reduction in mean latency.
- 37% reduction of latency in 99th percentile.

### Memory
- 43% reduction is RSS (resident set size, the non-swapped physical memory that a task has used).
- 51% reduction in total committed memory (observed with jcmd VM.native_memory summary).
- 74% reduction in the committed size of the Java Heap.