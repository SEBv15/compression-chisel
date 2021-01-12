package lcomp

import chisel3._
import chisel3.util._

import scala.collection.mutable.ListBuffer
import scala.math.pow

/** Wrapper for the hierarchical reduction module with inputs/outputs as single UInts instead of vectors.
 *  
 *  @constructor Create a compression module
 *  @param ncompressors The number of input compressor (expected to have vec of ten 16-bit UInts as output)
 *  @param maxblocks The maximum number of blocks/elements/words any merge stage should have as input (0 = no limit)
 *  @param set_all_unused_to_default Gurantee that only used values are nonzero. Setting this true makes testing easier, but vastly increases the number of gates used.
 *  @param bits_per_pixel Define the number of bits per pixel
 */
class HierarchicalReductionWrapper(val ncompressors:Int = 64, val maxblocks:Int = 128, val set_all_unused_to_default:Boolean = false, val bits_per_pixel:Int = 10) extends Module {
    val io = IO(new Bundle {
        val datain = Input(UInt((ncompressors*bits_per_pixel*16).W))
        val headerin = Input(UInt((4*ncompressors).W))
        val out = Output(UInt(((ncompressors*bits_per_pixel + (ncompressors/16*6).ceil.toInt)*16).W))
        val outlen = Output(UInt((log2Floor(ncompressors*bits_per_pixel + (ncompressors/16*6).ceil.toInt) + 1).W))
    })

    val reducer = Module(new HierarchicalReduction(ncompressors, maxblocks, set_all_unused_to_default, bits_per_pixel))
    //reducer.io.datain := (0 until ncompressors).reverse.map(x => (0 until bits_per_pixel).reverse.map(y => io.datain(x*bits_per_pixel*16 + 16*(y+1)-1, x*bits_per_pixel*16 + 16*y)).toSeq).toSeq
    for (i <- 0 until ncompressors) {
        for (j <- 0 until bits_per_pixel) {
            reducer.io.datain(i)(j) := io.datain((ncompressors - i - 1)*bits_per_pixel*16 + 16*(bits_per_pixel - j) - 1, (ncompressors - i - 1)*bits_per_pixel*16 + 16*(bits_per_pixel - j - 1))
        }
    }
    reducer.io.headerin := (0 until ncompressors).reverse.map(x => io.headerin(4*(x+1)-1, 4*x))
    io.out := Cat(reducer.io.out)
    io.outlen := reducer.io.outlen
}

object HierarchicalReductionWrapper extends App {
    chisel3.Driver.execute(args, () => new HierarchicalReductionWrapper)
}
