package lcomp

import org.scalatest._
import chiseltest._
import chisel3._

import scala.collection.mutable.ListBuffer
import scala.io.Source

class DeductionTest extends FlatSpec with ChiselScalatestTester with Matchers {
    it should "test-deduction" in {
        // test case body here
        test(new Deduction(64)) { c =>
            val reduced = new ListBuffer[Int]()
            val src = Source.fromFile("reduced.txt")
            for (n <- src.getLines) {
                reduced.append(n.toInt)
            }
            src.close()

            val solution = new ListBuffer[Int]()
            val src2 = Source.fromFile("solution.txt")
            for (n <- src2.getLines) {
                solution.append(n.toInt)
            }
            src2.close()

            val out = ListBuffer.fill((reduced.length/(1024.0/16.0)).ceil.toInt * 64 + 64)(0)

            for (i <- 0 until (reduced.length/(1024/16)).ceil.toInt) {
                val w = ListBuffer.fill(64)(-1)
                val d = ListBuffer.fill(64)(-1)
                c.io.number.poke(i.U)
                for (j <- 0 until 1024/16) {
                    c.io.in(j).poke(reduced(64*i+j).U)
                }
                for (j <- 0 until 64) {
                    w(j) = if (c.io.write(j).peek().litToBoolean) 1 else 0
                    d(j) = c.io.out(j).peek().litValue.toInt
                    if (c.io.write(j).peek().litToBoolean) {
                        out(c.io.pos(j).peek().litValue.toInt) = c.io.out(j).peek().litValue.toInt
                    }
                }
                println(w)
                println(d)

                c.clock.step()
            }

            println(out)

            for (i <- 0 until solution.length) {
                assert(out(i) == solution(i))
            }
        }
    }
}