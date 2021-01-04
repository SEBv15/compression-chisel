package lcomp

import org.scalatest._
import chiseltest._
import chisel3._

import scala.math.pow

class Reduction3Test extends FlatSpec with ChiselScalatestTester with Matchers {
    // Number of compressors to test the reduction stage with (32 is max for me before running out of memory)
    val ncompressors = 64

    it should "test-reduction3" in {
        // test case body here
        test(new Reduction3(ncompressors, 128)) { c =>
            // Set every compressor to output 1 word containing the compressor number
            for (nc <- 0 until ncompressors) {
                c.io.headerin(nc).poke(1.U)
                c.io.datain(nc)(0).poke(nc.U)
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
                c.io.out(nc + ncompressors/4).expect(nc.U)
            }

            // Check if the rest is zero
            for (i <- ncompressors + ncompressors/4 until ncompressors*164/16) {
                c.io.out(i).expect(0.U)
            }

            //========= TEST 2 ===========
            // Set only the first and last compressor input to one 3
            for (nc <- 0 until ncompressors) {
                c.io.headerin(nc).poke(0.U)
                for (i <- 0 until 10) {
                    c.io.datain(nc)(i).poke(0.U)
                }
            }       
            c.io.headerin(0).poke(1.U)
            c.io.datain(0)(0).poke(3.U)     
            c.io.headerin(ncompressors - 1).poke(1.U)
            c.io.datain(ncompressors - 1)(0).poke(3.U)     

            // Check if headers are correct
            for (nc <- 1 until ncompressors/4 - 1) {
                c.io.out(nc).expect(0.U)
            }
            c.io.out(0).expect((1 << 12).U)
            c.io.out(ncompressors/4 - 1).expect(1.U)

            // Check if the data is correct. Because of the 128 block limit, there should be 7 unused blocks between the 3s
            for (nc <- 0 until ncompressors*10) {
                if (nc != 0 && nc != 4)
                    c.io.out(nc + ncompressors/4).expect(0.U)
                else
                    c.io.out(nc + ncompressors/4).expect(3.U)
            }    
        }
    }
}