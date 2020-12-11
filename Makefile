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
