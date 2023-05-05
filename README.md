# RSA key generation
The source code here is a proof of concept of a new test for distributed RSA key generation.

Compilation is done through Gradle.

## Build

You need the following installed:

- JDK (version 19 or higher)
- Gradle (version 8.0.2 has been used in testing)

Once you have them installed, run:

    $ gradle build

The build script will run a few tests, resulting a few pem files created in `build/test-results/` directory.
Testing will take several minutes on a modern machine. 

## Benchmark
Benchmarks are specified in `ProtocolBenchmark.java` and uses JMH. 
By default only our protocol is benchmarked. To execute the benchmark run:

    $ gradle jmh