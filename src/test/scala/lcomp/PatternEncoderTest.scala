package lcomp

import org.scalatest._
import chiseltest._
import chisel3._

import scala.collection.mutable.ListBuffer
import scala.math.max

class PatternEncoderTest extends FlatSpec with ChiselScalatestTester with Matchers {
    def testPattern(c: PatternEncoder, pattern: String) {
        c.io.in.poke(Integer.parseInt(pattern, 2).U)
        val encoded = c.io.out.peek().litValue().toInt
        val pos = encoded >> 3
        val len = encoded & 7

        // Reconstruct input from encoded output
        var rstring = ""
        for (i <- 0 until 15) {
            if (14-i >= pos && 14-i < pos + len) {
                rstring += "1"
            } else {
                rstring += "0"
            }
        }

        // Check if reconstructed input matches input
        val canenc = Integer.parseInt(rstring, 2).toBinaryString == pattern
        //assert((c.io.canencode.peek().litValue > 0) == canenc)

        // Check if the module agrees
        c.io.canencode.expect(canenc.B)
        println("Checked " + pattern + " -> " + (if (canenc) "yes" else "no"))
    }

    it should "test-merge-weird" in {
        // test case body here
        test(new PatternEncoder) { c =>
            // Simply check every single possible input
            for (i <- 0 to 32767) {
                testPattern(c, i.toBinaryString)
            }

            /*
            c.io.in.poke(Integer.parseInt("111111111111111", 2).U)
            println(c.io.canencode.peek().litValue())
            println(c.io.out.peek().litValue().toInt.toBinaryString)
            c.io.canencode.expect(0.B)

            c.io.in.poke(Integer.parseInt("000011111111111", 2).U)
            println(c.io.canencode.peek().litValue())
            println(c.io.out.peek().litValue().toInt.toBinaryString)
            c.io.canencode.expect(0.B)

            c.io.in.poke(Integer.parseInt("111111111110000", 2).U)
            println(c.io.canencode.peek().litValue())
            println(c.io.out.peek().litValue().toInt.toBinaryString)
            c.io.canencode.expect(0.B)

            c.io.in.poke(Integer.parseInt("000000001111000", 2).U)
            println(c.io.canencode.peek().litValue())
            println(c.io.out.peek().litValue().toInt.toBinaryString)
            c.io.canencode.expect(1.B)

            c.io.in.poke(Integer.parseInt("000000000000000", 2).U)
            println(c.io.canencode.peek().litValue())
            println(c.io.out.peek().litValue().toInt.toBinaryString)
            c.io.canencode.expect(1.B)

            c.io.in.poke(Integer.parseInt("011110000000001", 2).U)
            println(c.io.canencode.peek().litValue())
            println(c.io.out.peek().litValue().toInt.toBinaryString)
            c.io.canencode.expect(0.B)

            c.io.in.poke(Integer.parseInt("000000000000001", 2).U)
            println(c.io.canencode.peek().litValue())
            println(c.io.out.peek().litValue().toInt.toBinaryString)
            c.io.canencode.expect(1.B)

            c.io.in.poke(Integer.parseInt("100000000000000", 2).U)
            println(c.io.canencode.peek().litValue())
            println(c.io.out.peek().litValue().toInt.toBinaryString)
            c.io.canencode.expect(1.B)*/
        }
    }
}