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
 *  @param inwords Number of elements for the two input vecs
 *  @param wordsize UInt width of the vec elements
 *  @param minwords Number of elements that are guaranteed to be used (min length)
 *  @param set_all_unused_to_default If true, all elements outside of the used length will be set to the default value. Otherwise only elements outside of `input_1_length + #inwords` will be set to default. The second option probably produces simpler logic since it only depends on one variable.
 *  @param default The value bits with no input will be assigned. true = all ones, false = all zeros
 */
class MergeAsymmetric(val wordsize:Int = 16, val inwords1:Int = 16, val inwords2:Int = 16, val set_all_unused_to_default:Boolean = false, val default:Boolean = false) extends Module {
    require(wordsize > 0)
    require(inwords1 > 0)
    require(inwords2 > 0)

    val io = IO(new Bundle {
        val len1 = Input(UInt(log2Ceil(inwords1).W))
        val data1 = Input(Vec(inwords1, UInt(wordsize.W)))
        val len2 = Input(UInt(log2Ceil(inwords2).W))
        val data2 = Input(Vec(inwords2, UInt(wordsize.W)))
        val outlen = Output(UInt((log2Ceil(inwords1 + inwords2)).W))
        val out = Output(Vec(inwords1 + inwords2, UInt(wordsize.W)))
    })

    val defaultval = if (default) (pow(2, wordsize) - 1).toInt else 0

    io.outlen := io.len1 +& io.len2

    if (set_all_unused_to_default) {
        for (i <- 0 until inwords1) {
            when (i.U < io.len1) {
                io.out(i) := io.data1(i)
            }.elsewhen(i.U < io.len1 +& io.len2) {
                io.out(i) := io.data2(i.U - io.len1)
            }.otherwise {
                io.out(i) := defaultval.U
            }
        }
        for (i <- inwords1 until inwords1 + inwords2) {
            when (i.U < io.len1 +& io.len2) {
                io.out(i) := io.data2(i.U - io.len1)
            }.otherwise {
                io.out(i) := defaultval.U
            }
        }
    } else {
        for (i <- 0 until inwords1) {
            when (i.U < io.len1) {
                io.out(i) := io.data1(i)
            }.elsewhen(i.U < io.len1 +& inwords1.U) {
                io.out(i) := io.data2(i.U - io.len1)
            }.otherwise {
                io.out(i) := defaultval.U
            }
        }
        for (i <- inwords1 until inwords1 + inwords2) {
            when (i.U < io.len1 +& inwords1.U) {
                io.out(i) := io.data2(i.U - io.len1)
            }.otherwise {
                io.out(i) := defaultval.U
            }
        }
    }

}

object MergeAsymmetric extends App {
    chisel3.Driver.execute(args, () => new MergeAsymmetric)
}