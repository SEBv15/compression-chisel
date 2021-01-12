//
// merger logic
//
// written by Sebastian Strempfer <sebastian@strempfer.com>
//
package lcomp

import chisel3._
import chisel3.util._

import scala.math.pow

/** Merge module which takes in two vecs of uints and their number of used elements, and outputs a single merged vec and its length.
 *
 *  @constructor Create a new merge module with custom parameters
 *  @param wordsize UInt width of the vec elements
 *  @param inwords1 Number of elements for the first input vec
 *  @param inwords2 Number of elements for the second input vec
 */
class MergeWeird(val wordsize:Int = 16, val inwords1:Int = 10, val inwords2:Int = 10) extends Module {
    require(wordsize > 0)
    require(inwords1 > 0)
    require(inwords2 > 0)
    require(inwords1 <= inwords2) // Could probably be removed by adding another if statement

    val io = IO(new Bundle {
        val len1 = Input(UInt((log2Ceil(inwords1) + 1).W))
        val data1 = Input(Vec(inwords1, UInt(wordsize.W)))
        val len2 = Input(UInt((log2Floor(inwords2) + 1).W))
        val data2 = Input(Vec(inwords2, UInt(wordsize.W)))
        val outlen = Output(UInt((log2Floor(inwords1 + inwords2) + 1).W))
        val out = Output(Vec(inwords1 + inwords2, UInt(wordsize.W)))
    })

    val len = Wire(UInt((log2Floor(inwords1 + inwords2) + 1).W))
    val pivot = Wire(UInt((log2Floor(inwords1 + inwords2) + 1).W))
    len := io.len1 +& io.len2
    when (len > inwords1.U) {
        pivot := len - inwords1.U
    }.otherwise {
        pivot := 0.U
    }

    for (i <- 0 until inwords1) {
        when (i.U < io.len1) {
            io.out(i) := io.data1(i)
        }.otherwise {
            io.out(i) := io.data2(pivot + i.U - io.len1)
        }
    }    

    for (i <- 0 until inwords2) {
        io.out(i + inwords1) := io.data2(i)
    }

    io.outlen := len
}

object MergeWeird extends App {
    chisel3.Driver.execute(args, () => new MergeWeird)
}