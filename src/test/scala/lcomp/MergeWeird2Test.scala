package lcomp

import org.scalatest._
import chiseltest._
import chisel3._

import scala.collection.mutable.ListBuffer

class MergeWeird2Test extends FlatSpec with ChiselScalatestTester with Matchers {
    def testwith(inwords1: Int, inwords2: Int, minwords1: Int, minwords2: Int) = {
        test(new MergeWeird2(16, inwords1, inwords2, minwords1, minwords2)) { c =>
            for (i <- 0 until inwords1) {
                c.io.data1(i).poke((i+1).U)
            }
            for (i <- 0 until inwords2) {
                c.io.data2(i).poke((i+11).U)
            }
            for (len1 <- minwords1 to inwords1) {
                for (len2 <- minwords2 to inwords2) {
                    println(len1.toString + ", " + len2.toString)
                    val l1 = List.range(1, len1+1)
                    val l2 = List.range(1+10, len2+1+10)
                    val pivot = if (len1 + len2 > inwords1) len1 + len2 - inwords1 else 0

                    val m = l1 ::: l2.slice(pivot, l2.length) ::: l2.slice(0, pivot)

                    c.io.len1.poke(len1.U)
                    c.io.len2.poke(len2.U)

                    for (i <- 0 until m.length) {
                        c.io.out(i).expect(m(i).U)
                        assert(c.io.out(i).peek().litValue == m(i))
                    }

                    c.io.outlen.expect((len1 + len2).U)
                }
            }
        }
    }
    it should "test-mergeweird2-symmetric" in {
        testwith(10, 10, 0, 0)
    }
    it should "test-mergeweird2-asymmetric1" in {
        testwith(10, 5, 0, 0)
    }
    it should "test-mergeweird2-asymmetric2" in {
        testwith(5, 10, 0, 0)
    }
    it should "test-mergeweird2-symmetric-minwords1" in {
        testwith(10, 10, 5, 0)
    }
    it should "test-mergeweird2-symmetric-minwords2" in {
        testwith(10, 10, 0, 5)
    }
}