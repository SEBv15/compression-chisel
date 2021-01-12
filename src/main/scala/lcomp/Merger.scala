package lcomp

import chisel3._
import chisel3.util._

import scala.math.pow

/** Wrapper for MergeAsymmetric and MergeWeird
 *
 *  @constructor Create a new merge module with custom parameters
 *  @param inwords Number of elements for the two input vecs
 *  @param wordsize UInt width of the vec elements
 *  @param minwords Number of elements that are guaranteed to be used (min length)
 *  @param merge_weird Use the weird (but less gatey) merger
 *  @param set_all_unused_to_default If true, all elements outside of the used length will be set to the default value. Otherwise only elements outside of `input_1_length + #inwords` will be set to default. The second option probably produces simpler logic since it only depends on one variable.
 *  @param default The value bits with no input will be assigned. true = all ones, false = all zeros
 */
class Merger(val wordsize:Int = 16, val inwords1:Int = 10, val inwords2:Int = 10, val merge_weird:Boolean = true, val set_all_unused_to_default:Boolean = false, val default:Boolean = false) extends Module {
    val io = IO(new Bundle {
        val len1 = Input(UInt((log2Ceil(inwords1) + 1).W))
        val data1 = Input(Vec(inwords1, UInt(wordsize.W)))
        val len2 = Input(UInt((log2Floor(inwords2) + 1).W))
        val data2 = Input(Vec(inwords2, UInt(wordsize.W)))
        val outlen = Output(UInt((log2Floor(inwords1 + inwords2) + 1).W))
        val out = Output(Vec(inwords1 + inwords2, UInt(wordsize.W)))
    })

    if (merge_weird) {
        val merger = Module(new MergeWeird(wordsize, inwords1, inwords2))
        merger.io.len1 := io.len1
        merger.io.data1 := io.data1
        merger.io.len2 := io.len2
        merger.io.data2 := io.data2
        io.outlen := merger.io.outlen

        val defaultval = if (default) (pow(2, wordsize) - 1).toInt else 0

        if (set_all_unused_to_default) {
            for (i <- 0 until inwords1 + inwords2) {
                when (i.U < merger.io.outlen) {
                    io.out(i) := merger.io.out(i)
                }.otherwise {
                    io.out(i) := defaultval.U
                }
            }
        } else {
            io.out := merger.io.out
        }
    } else {
        val merger = Module(new MergeAsymmetric(wordsize, inwords1, inwords2, set_all_unused_to_default, default))
        merger.io.len1 := io.len1
        merger.io.data1 := io.data1
        merger.io.len2 := io.len2
        merger.io.data2 := io.data2
        io.outlen := merger.io.outlen
        io.out := merger.io.out
    }

}