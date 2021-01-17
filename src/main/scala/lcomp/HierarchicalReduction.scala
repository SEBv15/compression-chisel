package lcomp

import chisel3._
import chisel3.util._

import scala.collection.mutable.ListBuffer
import scala.math.pow

/** Merges the output of many compressors into one semi-continous word using hierarchical reduction.
 *  
 *  By setting the maxblocks parameter, the reduction module limits the number of input elements a single stage (merge module) can have.
 *  Whenever the number would exceed the maxmimum number of blocks/elements, adjacent elements get merged together, which halves the number of elements.
 *  This possibly means grouping a used element with an unused one, reducing the compression ratio. 
 *  Even though there are gaps in the data, it is still possible to reconstruct the original data since the location of the gaps can be inferred from the size of the output from every compressor, which is transmitted first.
 *
 *  @constructor Create a compression module
 *  @param ncompressors The number of input compressor (expected to have vec of ten 16-bit UInts as output)
 *  @param maxblocks The maximum number of blocks/elements/words any merge stage should have as input (0 = no limit)
 *  @param set_all_unused_to_default Gurantee that only used values are nonzero. Setting this true makes testing easier, but vastly increases the number of gates used.
 *  @param bits_per_pixel Define the number of bits per pixel
 */
class HierarchicalReduction(val ncompressors:Int = 64, val maxblocks:Int = 128, val set_all_unused_to_default:Boolean = false, val bits_per_pixel:Int = 10) extends Module {
    require(isPow2(ncompressors))
    require(ncompressors >= 8)
    require(ncompressors % 8 == 0)
    require(maxblocks > bits_per_pixel || maxblocks == 0)

    val io = IO(new Bundle {
        val datain = Input(Vec(ncompressors, Vec(bits_per_pixel, UInt(16.W))))
        val headerin = Input(Vec(ncompressors, UInt((log2Floor(bits_per_pixel) + 1).W)))
        val out = Output(Vec(ncompressors*bits_per_pixel + (ncompressors*6/16).ceil.toInt, UInt(16.W)))
        val outlen = Output(UInt((log2Floor(ncompressors*bits_per_pixel + (ncompressors*6/16).ceil.toInt) + 1).W))
    })

    // Make 2-bit headers
    val twobit_headers = Wire(Vec(ncompressors, UInt(2.W)))
    for (i <- 0 until ncompressors) {
        when (io.headerin(i) > 2.U) {
            twobit_headers(i) := 3.U
        }.otherwise {
            twobit_headers(i) := io.headerin(i)
        }
    }
    //val twobit_headers_16 = (0 until ncompressors/8).map(x => twobit_headers.asUInt()(16*(x + 1) - 1, 16*x))
    val twobit_headers_16 = (0 until ncompressors/8).map(x => Cat(twobit_headers.slice(8*x, 8*(x+1))))

    // Reduce Headers
    val header_reducer = Module(new DataReduction(ncompressors, 1, 4, 0, false, set_all_unused_to_default))
    for (i <- 0 until ncompressors) {
        header_reducer.io.in(i)(0) := io.headerin(i)
        header_reducer.io.inlengths(i) := io.headerin(i) > 2.U
    }
    val headers_16 = (0 until ncompressors/4).map(x => Cat(header_reducer.io.out.slice(4*x, 4*(x+1))))
    val headers_16_length = (header_reducer.io.outlength +& 3.U) / 4.U

    // Reduce Data
    val data_reducer = Module(new DataReduction(ncompressors, bits_per_pixel, 16, maxblocks, true, set_all_unused_to_default))
    data_reducer.io.in := io.datain
    data_reducer.io.inlengths := io.headerin

    // Merge Headers and Data
    val merger = Module(new MergeWeird(16, ncompressors/4, ncompressors*bits_per_pixel))
    merger.io.len1 := headers_16_length
    merger.io.data1 := headers_16
    merger.io.len2 := data_reducer.io.outlength
    merger.io.data2 := data_reducer.io.out

    io.out := twobit_headers_16 ++ merger.io.out
    io.outlen := (ncompressors/8).U +& merger.io.outlen
}

object HierarchicalReduction extends App {
    chisel3.Driver.execute(args, () => new HierarchicalReduction)
}