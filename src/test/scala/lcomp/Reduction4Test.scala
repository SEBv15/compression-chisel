package lcomp

import org.scalatest._
import chiseltest._
import chisel3._

import scala.math.pow
import scala.math.floor

import scala.collection.mutable.ListBuffer
import scala.collection.breakOut

class Reduction4Test extends FlatSpec with ChiselScalatestTester with Matchers {
    // Number of compressors to test the reduction stage with (32 is max for me before running out of memory)
    val ncompressors = 64

    it should "test-reduction4" in {
        // test case body here
        test(new Reduction4(ncompressors, 128)) { c =>
            // Generate a 2D list of random values which will be used as inputs for the 64 compressors
            val r = new scala.util.Random
            val lengths:ListBuffer[Int] = (0 until ncompressors).map(i => r.nextInt(10))(breakOut)
            var inp:ListBuffer[ListBuffer[Int]] = new ListBuffer[ListBuffer[Int]]
            for (i <- 0 until ncompressors) {
                inp.append(new ListBuffer[Int])
                for (j <- 0 until lengths(i)) {
                    inp(i) = inp(i) :+ r.nextInt(1024)
                }
            }

            println(inp)

            // Input the data into the reduction stage
            for (i <- 0 until inp.length) {
                for (j <- 0 until 10) {
                    c.io.datain(i)(j).poke((if (inp(i).length <= j) 0 else inp(i)(j)).U)
                }
                c.io.headerin(i).poke(lengths(i).U)
            }

            // Slowly turn the 2D list into essentially a 1D list with the data in the expected order and filler zeros where needed
            var mod = 2
            var len = 10
            while (inp.length != 1) {
                // When the reduction stage in the module merges two adjacent data, add filler zeros where needed
                if (len > 128) {
                    len /= 2
                    for (i <- 0 until inp.length) {
                        if (inp(i).length % mod != 0) {
                            val toadd = (mod - (inp(i).length % mod))
                            for (j <- 0 until toadd) {
                                inp(i) = inp(i) :+ 0
                            }
                        }
                    }
                    mod *= 2
                }
                inp = (0 until inp.length / 2).map(i => inp(2*i) ++ inp(2*i+1))(breakOut)
                len *= 2
            }
            // Count the number of headers > 2 and generate a list of headers > 2.
            var numheaders = 0
            var bigheaders:ListBuffer[Int] = new ListBuffer[Int]
            for (i <- 0 until lengths.length) {
                if (lengths(i) > 2) {
                    numheaders += 1
                    bigheaders = bigheaders :+ lengths(i)
                }
            }
            numheaders += (16 - numheaders % 16) // Since there might be a gap between where the headers end and the data starts, adjust the "numheaders" accordingly

            println(bigheaders.length)

            // Print the output of the reduction stage
            var out: ListBuffer[BigInt] = new ListBuffer[BigInt]
            for (i <- 0 until ncompressors * 10 + (ncompressors/8 + numheaders/4).ceil.toInt) {
                out = out :+ c.io.out(i).peek().litValue()
            }
            println(out)

            // Check if the 2-bit headers are correct
            val twobit_headers = (0 until ncompressors).map(x => if (lengths(x) > 2) 2 else lengths(x))
            for (i <- 0 until ncompressors/8) {
                c.io.out(i).expect(((twobit_headers(8*i) << 14) + (twobit_headers(8*i+1) << 12) + (twobit_headers(8*i+2) << 10) + (twobit_headers(8*i+3) << 8) + (twobit_headers(8*i+4) << 6) + (twobit_headers(8*i+5) << 4) + (twobit_headers(8*i+6) << 2) + (twobit_headers(8*i+7) << 0)).U)
            }

            // Check if the big 4-bit headers are correct
            for (i <- 0 until bigheaders.length / 4) {
                c.io.out(ncompressors/8 + i).expect(((bigheaders(4*i) << 0) + (bigheaders(4*i+1) << 4) + (bigheaders(4*i+2) << 8) + (bigheaders(4*i+3) << 12)).U)
            }

            // Check if the data is correct
            for (i <- 0 until inp(0).length) {
                c.io.out(i + (numheaders/4).ceil.toInt + (ncompressors/8).toInt).expect(inp(0)(i).U)
            }
        }
    }
}