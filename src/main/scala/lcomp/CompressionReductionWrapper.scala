package lcomp

import chisel3._
import chisel3.util._

import scala.collection.mutable.ListBuffer
import scala.math.pow

/** Wrapper for the hierarchical compression reduction module with inputs/outputs as single UInts instead of vectors.
 *  
 *  @constructor Create a compression module
 *  @param npixels The number of input compressor (expected to have vec of ten 16-bit UInts as output)
 *  @param maxblocks The maximum number of blocks/elements/words any merge stage should have as input (0 = no limit)
 *  @param set_all_unused_to_default Gurantee that only used values are nonzero. Setting this true makes testing easier, but vastly increases the number of gates used.
 *  @param bits_per_pixel Define the number of bits per pixel
 */
class CompressionReductionWrapper(val npixels:Int = 128*8, val maxblocks:Int = 128, val set_all_unused_to_default:Boolean = false, val bits_per_pixel:Int = 10) extends Module {
    val io = IO(new Bundle {
        val pixels = Input(UInt((npixels*bits_per_pixel).W))
        val bypass_compression = Input(Bool())
        val out = Output(UInt((npixels*bits_per_pixel + (npixels*6/16).ceil.toInt).W))
        val outlen = Output(UInt((log2Floor(npixels*bits_per_pixel/16 + (npixels*6/16/16).ceil.toInt) + 1).W))
    })

    val reducer = Module(new CompressionReduction(npixels, maxblocks, set_all_unused_to_default, bits_per_pixel))
    for (i <- 0 until npixels) {
        reducer.io.pixels(npixels - i - 1) := io.pixels(bits_per_pixel*(i+1)-1, bits_per_pixel*i)
    }

    val outreg = RegInit(0.U)
    val outlenreg = RegInit(0.U)

    when (io.bypass_compression) {
        outreg := io.pixels
        outlenreg := 0.U
    }.otherwise {
        outreg := Cat(reducer.io.out)
        outlenreg := reducer.io.outlen
    }

    io.out := outreg
    io.outlen := outlenreg
}

object CompressionReductionWrapper extends App {
    chisel3.Driver.execute(args, () => new CompressionReductionWrapper)
}
