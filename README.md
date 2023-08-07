# RSA key generation
The source code here is a proof of concept of a new test for distributed RSA key generation.

Compilation is done through Gradle.

## Build
### Simple (without JNI)
As a minimum you need the following installed:

- JDK (version 19 or higher)
- Gradle (version 8.0.2 has been used in testing)

To compile and run (without the use of optimized operations using the JNI), run:

    $ ./gradlew build

The build script will run a few tests, resulting a few pem files created in `build/test-results/` directory.
Testing will take several minutes on a modern machine.
Observe that _all_ tests involving the JNI will fail in this simple setup.

### Run with JNI
To be able to run the fastest version of the code you need to use GMP through the JNI.
This requires a bit of setup.

First ensure you have `make`, `gcc` and GMP installed. 
On a Linux box, the following steps have been confirmed to be sufficient: 
```
sudo apt-get update
sudo apt install openjdk-19-jdk gradle make gcc libgmp-dev
```
On a Mac the necessarily tools are included in XCode, with the exception of GMP.
GMP can be installed using Brew
```
brew install gmp
```

Next clone the JNI wrapper repo. The following has been confirmed to work:
```
git clone https://github.com/jot2re/GMP-java.git
```
If you are on linux do the following:
```
git checkout linux-conf
```
If you are on a Mac (only tested for an M1) do the following:
```
git checkout mac-conf
```
Follow the README in the GMP-java, and compile the wrappers, and then copy these to a location where the JDK can find them.
E.g.
```
cp GMP-java/lib* rsa/
sudo cp GMP-java/lib* /usr/lib
```

Now you can again compile and run the tests for this project, which should now all pass.

## Benchmark
Benchmarks are specified in `ProtocolBenchmark.java` and uses JMH.
By default only our protocol is benchmarked. To execute the benchmark run:

    $ ./gradlew jmh