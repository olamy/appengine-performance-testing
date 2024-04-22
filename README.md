# AppEngine Performance Testing

This is a benchmark to test the performance improvements of the work done in AppEngine to bypass the RPC layer.

The benchmark is written as a parameterized junit test and the results are output to a directory called `output` under the project root directory.

The benchmark has been configured to run on the WebTide machines on Jenkins.
See https://jenkins.webtide.net/job/load_testing/job/appengine-benchmark/

## Testing Results

### Latency
- Reduction in median latency 222µs to 153µs (31% reduction).
- Reduction of mean latency 14111µs to 208µs (98.5% reduction).
- Reduction in min latency 153µs to 127µs (16% reduction).
- Reduction of latency in 99th percentile 254541µs to 752µs (99.7% reduction).

### Memory
- Reduction in total committed memory 682MB to 202MB (70% reduction).
- Reduction is RSS 688MB to 253MB (63% reduction).
- Reduction in Java Heap committed size 422MB to 70MB (83% reduction).

### CPU
The new mode tended to use a lot less CPU, here is the measurements of CPU usage taken at 5 second increments throughout the load test.

| **NewMode** | 212% | 119% | 87% | 69% | 60% | 55% | 50% | 47% | 45% | 43%  | 41%  |
|---------|------|------|------|------|------|------|------|------|------|-------|-------|
| **OldMode** | 174% | 226% | 169% | 139% | 123% | 110% | 101% | 92% | 86% | 82%  | 79%  |

## Understanding the Stats

How the stats are measured.
 - **CPU** and **RSS** measured with `ps` at 5s intervals.
 - **Total Committed Memory** and **Java Heap Committed Size** are measured with `jcmd VM.native_memory summary` at 5s intervals.
 - Latency stats are measured by the `LoadGenerator` on the Client machine.

What the stats mean:
 - **RSS** - resident set size, the non-swapped physical memory that the process has used.
 - **Total Committed Memory** - the total amount of memory that the JVM has committed to the Java application, including heap and non-heap memory.
 - **Java Heap Committed Size** - the amount of memory committed to the heap.