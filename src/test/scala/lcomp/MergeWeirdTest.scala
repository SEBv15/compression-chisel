package lcomp

import org.scalatest._
import chiseltest._
import chisel3._

import scala.collection.mutable.ListBuffer
import scala.math.max

class MergeWeirdTest extends FlatSpec with ChiselScalatestTester with Matchers {
    it should "test-merge-weird" in {
        // test case body here
        test(new MergeWeird) { c =>
            // Test all 0
            c.io.len1.poke(0.U)
            c.io.len2.poke(0.U)
            for (i <- 0 until 10) {
                c.io.data1(i).poke(0.U)
                c.io.data2(i).poke(0.U)
            }
            c.io.outlen.expect(0.U)
            for (i <- 0 until 20) {
                c.io.out(i).expect(0.U)
            }

            // Test all full length
            c.io.len1.poke(10.U)
            c.io.len2.poke(10.U)
            for (i <- 0 until 10) {
                c.io.data1(i).poke(1.U)
                c.io.data2(i).poke(2.U)
            }
            c.io.outlen.expect(20.U)
            for (i <- 0 until 10) {
                c.io.out(i).expect(1.U)
            }            
            for (i <- 10 until 20) {
                c.io.out(i).expect(2.U)
            }            

            // Test 100 random datasets
            val r = scala.util.Random
            for (testn <- 0 until 100) {
                val len1 = r.nextInt(11)
                val len2 = r.nextInt(11)

                var data1:List[Int] = List.fill(len1)(r.nextInt(16))
                var data2:List[Int] = List.fill(len2)(r.nextInt(16))

                c.io.len1.poke(data1.length.U)
                c.io.len2.poke(data2.length.U)

                for (i <- 0 until 10) {
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

                data2 = data2 ++ List.fill(10 - data2.length)(0)
                val pivot = max(0, len1 + len2 - 10)
                val outdat:List[Int] = data1 ++ data2.slice(pivot, 10 - len1 + pivot) ++ data2

                
                var mout = new ListBuffer[BigInt]()
                for (i <- 0 until 20) {
                    mout += c.io.out(i).peek().litValue()
                }
                println(data1)
                println(data2)
                println(mout)
                println(outdat)

                for (i <- 0 until 20) {
                    if (i < outdat.length) {
                        c.io.out(i).expect(outdat(i).U)
                    } else {
                        c.io.out(i).expect(0.U)
                    }
                }

                c.io.outlen.expect((len1 + len2).U)
            }
        }
    }
}