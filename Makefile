lengthtest:
	sbt 'testOnly lcomp.LengthCompressionTest --'

lengthverilog:
	sbt 'runMain lcomp.LengthCompress'

mergetest:
	sbt 'testOnly lcomp.MergeTest --'

mergeverilog:
	sbt 'runMain lcomp.Merge'

mergeasymmetrictest:
	sbt 'testOnly lcomp.MergeAsymmetricTest --'

mergeasymmetricverilog:
	sbt 'runMain lcomp.MergeAsymmetric'

mergeweirdtest:
	sbt 'testOnly lcomp.MergeWeirdTest --'

mergeweirdverilog:
	sbt 'runMain lcomp.MergeWeird'

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

hreductiontest:
	sbt 'testOnly lcomp.HierarchicalReductionTest --'

hreductionverilog:
	sbt 'runMain lcomp.HierarchicalReduction'

creductionwrapperverilog:
	sbt 'runMain lcomp.CompressionReductionWrapper'

creductionverilog:
	sbt 'runMain lcomp.CompressionReduction'

deductionverilog:
	sbt 'runMain lcomp.Deduction'

deductiontest:
	sbt 'testOnly lcomp.DeductionTest --'

encoderverilog:
	sbt 'runMain lcomp.PatternEncoder'

encodertest:
	sbt 'testOnly lcomp.PatternEncoderTest --'