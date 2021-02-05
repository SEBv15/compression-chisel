package lcomp

import org.scalatest._
import chiseltest._
import chisel3._

import scala.collection.mutable.ListBuffer
import scala.math.max

class PatternEncoderTest extends FlatSpec with ChiselScalatestTester with Matchers {
    it should "test-merge-weird" in {
        // test case body here
        test(new PatternEncoder) { c =>
            c.io.in.poke(Integer.parseInt("111111111111111", 2).U)
            println(c.io.canencode.peek().litValue())
            println(c.io.out.peek().litValue().toInt.toBinaryString)

            c.io.in.poke(Integer.parseInt("000011111111111", 2).U)
            println(c.io.canencode.peek().litValue())
            println(c.io.out.peek().litValue().toInt.toBinaryString)

            c.io.in.poke(Integer.parseInt("111111111110000", 2).U)
            println(c.io.canencode.peek().litValue())
            println(c.io.out.peek().litValue().toInt.toBinaryString)

            c.io.in.poke(Integer.parseInt("000000001111000", 2).U)
            println(c.io.canencode.peek().litValue())
            println(c.io.out.peek().litValue().toInt.toBinaryString)

            c.io.in.poke(Integer.parseInt("000000000000000", 2).U)
            println(c.io.canencode.peek().litValue())
            println(c.io.out.peek().litValue().toInt.toBinaryString)

            c.io.in.poke(Integer.parseInt("011111111110001", 2).U)
            println(c.io.canencode.peek().litValue())
            println(c.io.out.peek().litValue().toInt.toBinaryString)


        }
    }
}