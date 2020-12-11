package lcomp

import org.scalatest._
import chiseltest._
import chisel3._

import scala.collection.mutable.ListBuffer

class MergeTest extends FlatSpec with ChiselScalatestTester with Matchers {
    it should "test-merge" in {
        // test case body here
        test(new Merge) { c =>
            // Test all 0
            c.io.len1.poke(1.U) // min length because header
            c.io.len2.poke(1.U)
            for (i <- 0 until 41) {
                c.io.data1(i).poke(0.U)
                c.io.data2(i).poke(0.U)
            }
            c.io.outlen.expect(2.U)
            for (i <- 0 until 82) {
                c.io.out(i).expect(0.U)
            }

            // Test all full length
            c.io.len1.poke(41.U)
            c.io.len2.poke(41.U)
            for (i <- 0 until 41) {
                c.io.data1(i).poke(1.U)
                c.io.data2(i).poke(2.U)
            }
            c.io.outlen.expect(82.U)
            for (i <- 0 until 41) {
                c.io.out(i).expect(1.U)
            }            
            for (i <- 41 until 82) {
                c.io.out(i).expect(2.U)
            }            

            // Test 100 random datasets
            val r = scala.util.Random
            for (testn <- 0 until 100) {
                val len1 = r.nextInt(11)
                val len2 = r.nextInt(11)

                var data1:List[Int] = List.fill(len1*4)(r.nextInt(16))
                var data2:List[Int] = List.fill(len2*4)(r.nextInt(16))

                data1 = r.nextInt(16) +: data1
                data2 = r.nextInt(16) +: data2

                c.io.len1.poke(data1.length.U)
                c.io.len2.poke(data2.length.U)

                for (i <- 0 until 41) {
                    if (i < data1.length) {
                        c.io.data1(i).poke(data1(i).U)
                    } else {
                        c.io.data1(i).poke(0.U)
                    }
                    if (i < data2.length) {
                        c.io.data2(i).poke(data2(i).U)
                    } else {
                        c.io.data2(i).poke(0.U)
                    }
                }

                val outdat:List[Int] = data1 ++ data2

                
                var mout = new ListBuffer[BigInt]()
                for (i <- 0 until 82) {
                    mout += c.io.out(i).peek().litValue()
                }
                //println(data1)
                //println(data2)
                //println(mout)
                //println(outdat)

                for (i <- 0 until 82) {
                    if (i < outdat.length) {
                        c.io.out(i).expect(outdat(i).U)
                    } else {
                        c.io.out(i).expect(0.U)
                    }
                }

                c.io.outlen.expect(outdat.length.U)
            }
        }
    }
}