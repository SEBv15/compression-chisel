package lcomp

import org.scalatest._
import chiseltest._
import chisel3._

class ReductionTest extends FlatSpec with ChiselScalatestTester with Matchers {
    it should "test-reduction" in {
        // test case body here
        test(new Reduction) { c =>
        }
    }
}