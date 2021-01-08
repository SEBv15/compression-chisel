# Length Compression and Reduction

## Length Compression

Length-Compresses a variable amount of pixels with a variable amount of bits, and outputs a header and shuffled data.

Main file: [`LengthCompress.scala`](src/main/scala/lcomp/LengthCompress.scala)

Running tests: `make lengthtest`

Generating Verilog: `make lengthverilog`

## Single Reduction Stage (Merge)

Takes in in two equally sized Vecs of data and their lengths (as number of words not bits) and outputs the merged data and its length

Main file: [`Merge.scala`](src/main/scala/lcomp/Merge.scala)

Running tests: `make mergetest`

Generating Verilog: `make mergeverilog`

## Full Reduction v1

Takes in headers and data from 64 compressors and spits out a single UInt 10496 bit wide containing the merged data

Main file: [`Reduction.scala`](src/main/scala/lcomp/Reduction.scala)

Running tests: Not possible because of memory issues

Generating Verilog: `make reductionverilog`

## Full Reduction v2 (better)

Takes in headers and data from at least 4 compressors and outputs a Vec of 16-bit UInts with all the headers grouped together in the front and the merged data after. Also outputs a UInt containing the number of used output Vec elements.

Main file: [`Reduction2.scala`](src/main/scala/lcomp/Reduction2.scala)

Running tests: `make reduction2test` (If you run into memory issues, reduce ncompressors in the test file)

Generating Verilog: `make reduction2verilog`

## Full Reduction v3

Same as v2, but it has an option to specify the maximum number of vec elements for every stage. This results in worse compression, but simpler logic.

Main file: [`Reduction3.scala`](src/main/scala/lcomp/Reduction3.scala)

Running tests: `make reduction3test`

Generating Verilog: `make reduction3verilog`

## Full Reduction v4

Uses the same method as v3 for data reduction, but uses hierarchical headers with their own reduction logic instead of a static 4-bit header.

To run the test case, you might have to increase your java heap memory or change the range of the for loop on [line 21](src/test/scala/lcomp/Reduction4Test.scala) to run less tests.

Main file: [`Reduction4.scala`](src/main/scala/lcomp/Reduction4.scala)

Running tests: `make reduction4test`

Generating Verilog: `make reduction4verilog`