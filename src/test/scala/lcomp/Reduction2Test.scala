package lcomp

import org.scalatest._
import chiseltest._
import chisel3._

import scala.math.pow

class Reduction2Test extends FlatSpec with ChiselScalatestTester with Matchers {
    // Number of compressors to test the reduction stage with (32 is max for me before running out of memory)
    val ncompressors = 64

    it should "test-reduction2" in {
        // test case body here
        test(new Reduction2(ncompressors)) { c =>
            // Set every compressor to output 1 word containing the value 3
            for (nc <- 0 until ncompressors) {
                c.io.headerin(nc).poke(1.U)
                c.io.datain(nc)(0).poke(3.U)
                for (i <- 1 until 10) {
                    c.io.datain(nc)(i).poke(0.U)
                }
            }

            // Check if headers are correct
            for (nc <- 0 until ncompressors/4) {
                val exp: Int = (1 << 12) + (1 << 8) + (1 << 4) + 1
                c.io.out(nc).expect(exp.U)
            }

            // Check if the length is correct
            c.io.outlen.expect((ncompressors/4 + ncompressors).U)

            // Check if the data is all at the beginning
            for (nc <- 0 until ncompressors) {
                c.io.out(nc + ncompressors/4).expect(3.U)
            }

            // Check if the rest is zero
            for (i <- ncompressors + ncompressors/4 until ncompressors*164/16) {
                c.io.out(i).expect(0.U)
            }
        }
    }
}