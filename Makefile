lengthtest:
	sbt 'testOnly lcomp.LengthCompressionTest --'

lengthverilog:
	sbt 'runMain lcomp.LengthCompress'

mergetest:
	sbt 'testOnly lcomp.MergeTest --'

mergeverilog:
	sbt 'runMain lcomp.Merge'

reductiontest:
	sbt 'testOnly lcomp.ReductionTest --'

reductionverilog:
	sbt 'runMain lcomp.Reduction'

reduction2test:
	sbt 'testOnly lcomp.Reduction2Test --'

reduction2verilog:
	sbt 'runMain lcomp.Reduction2'

reduction3test:
	sbt 'testOnly lcomp.Reduction3Test --'

reduction3verilog:
	sbt 'runMain lcomp.Reduction3'

reduction4test:
	sbt 'testOnly lcomp.Reduction4Test --'

reduction4verilog:
	sbt 'runMain lcomp.Reduction4'
