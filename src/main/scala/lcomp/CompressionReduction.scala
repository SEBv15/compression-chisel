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
class CompressionReduction(val npixels:Int = 128*8, val maxblocks:Int = 128, val set_all_unused_to_default:Boolean = false, val bits_per_pixel:Int = 10) extends Module {
    require(isPow2(npixels))

    val io = IO(new Bundle {
        val pixels = Input(Vec(npixels, UInt(bits_per_pixel.W)))
        val out = Output(Vec(npixels/16*bits_per_pixel + (npixels/16/16*6).ceil.toInt, UInt(16.W)))
        val outlen = Output(UInt((log2Floor(npixels/16*bits_per_pixel + (npixels/16/16*6).ceil.toInt) + 1).W))
    })

    // Compress the data
    var cdata = Wire(Vec(npixels/16, Vec(bits_per_pixel, UInt(16.W))))
    var cheaders = Wire(Vec(npixels/16, UInt((log2Floor(bits_per_pixel) + 1).W)))

    var compressors = List.fill(npixels/16)(Module(new LengthCompress(16, bits_per_pixel)))
    for (i <- 0 until compressors.length) {
        compressors(i).io.in := io.pixels.slice(16*i, 16*(i+1))
        cdata(i) := compressors(i).io.data
        cheaders(i) := compressors(i).io.header
    }

    // Reduce the data
    var reducer = Module(new HierarchicalReduction(npixels/16, maxblocks, set_all_unused_to_default, bits_per_pixel))
    reducer.io.datain := cdata
    reducer.io.headerin := cheaders
    io.out := reducer.io.out
    io.outlen := reducer.io.outlen
}

object CompressionReduction extends App {
    chisel3.Driver.execute(args, () => new CompressionReduction)
}