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
 */
class Reduction4(val ncompressors:Int = 64, val maxblocks:Int = 128) extends Module {
    require(isPow2(ncompressors))
    require(ncompressors % 8 == 0)
    require(ncompressors >= 8)
    require(maxblocks >= 10 || maxblocks == 0)

    val io = IO(new Bundle {
        val datain = Input(Vec(ncompressors, Vec(10, UInt(16.W))))
        val headerin = Input(Vec(ncompressors, UInt(4.W)))
        //val out = Output(UInt((ncompressors * 164).W))
        val out = Output(Vec(ncompressors*10 + (ncompressors/16*6).ceil.toInt, UInt(16.W))) // First ncompressors/4 words are the headers and the rest is the data
        val outlen = Output(UInt((log2Floor(ncompressors*10 + ncompressors/4) + 1).W))
    })

    // ------- DATA REDUCTION -------

    // Make the first stage of compressors
    val stage1 = ListBuffer.fill(ncompressors/2)(Module(new Merge(10, 16, 0)))
    for (i <- 0 until ncompressors/2) {
        stage1(i).io.len1 := io.headerin(2*i)
        stage1(i).io.len2 := io.headerin(2*i+1)
        stage1(i).io.data1 := io.datain(2*i)
        stage1(i).io.data2 := io.datain(2*i+1)
    }

    // Use a list for all the other stages
    var stages:ListBuffer[ListBuffer[lcomp.Merge]] = new ListBuffer[ListBuffer[lcomp.Merge]]()
    stages.append(stage1)

    var nb = 20; // number of blocks out of the first merge stage
    var div = 1; // what the number of blocks should be divided by relative to the number of blocks without a block limit

    // Add stages as needed and make them take the previous stage as input
    for (n <- 1 until log2Up(ncompressors)) {
        var merge = false;
        if (nb > maxblocks && maxblocks != 0) {
            div *= 2;
            nb /= 2;
            merge = true;
        }
        stages.append(ListBuffer.fill(ncompressors/pow(2, n+1).toInt)(Module(new Merge(10*pow(2, n).toInt/div, 16*div, 0))))

        // If number of blocks needs to be divided, group two inputs together before feeding it into the next stage
        if (merge) {
            for (i <- 0 until ncompressors/pow(2, n+1).toInt) {
                stages(n)(i).io.len1 := (stages(n-1)(2*i).io.outlen +& 1.U) / 2.U // Ceil division
                stages(n)(i).io.len2 := (stages(n-1)(2*i+1).io.outlen +& 1.U) / 2.U
                stages(n)(i).io.data1 := (0 until nb).map(x => Cat(stages(n-1)(2*i).io.out(2*x+1), stages(n-1)(2*i).io.out(2*x)))
                stages(n)(i).io.data2 := (0 until nb).map(x => Cat(stages(n-1)(2*i+1).io.out(2*x+1), stages(n-1)(2*i+1).io.out(2*x)))
            }
        } else {
            for (i <- 0 until ncompressors/pow(2, n+1).toInt) {
                stages(n)(i).io.len1 := stages(n-1)(2*i).io.outlen
                stages(n)(i).io.len2 := stages(n-1)(2*i+1).io.outlen
                stages(n)(i).io.data1 := stages(n-1)(2*i).io.out
                stages(n)(i).io.data2 := stages(n-1)(2*i+1).io.out
            }
        }
        nb *= 2;
    }

    // ------- HEADER REDUCTION --------

    val header_stage1 = ListBuffer.fill(ncompressors/2)(Module(new Merge(1, 4, 0)))
    for (i <- 0 until ncompressors/2) {
        when(io.headerin(2*i) > 2.U) {header_stage1(i).io.len1 := 1.U}.otherwise {header_stage1(i).io.len1 := 0.U}
        when(io.headerin(2*i+1) > 2.U) {header_stage1(i).io.len2 := 1.U}.otherwise {header_stage1(i).io.len2 := 0.U}
        //header_stage1(i).io.len1 := io.headerin(2*i) > 2.U
        //header_stage1(i).io.len2 := io.headerin(2*i+1) > 2.U
        header_stage1(i).io.data1(0) := io.headerin(2*i)
        header_stage1(i).io.data2(0) := io.headerin(2*i+1)
    }

    var header_stages:ListBuffer[ListBuffer[lcomp.Merge]] = new ListBuffer[ListBuffer[lcomp.Merge]]()
    header_stages.append(header_stage1)

    for (n <- 1 until log2Up(ncompressors)) {
        header_stages.append(ListBuffer.fill(ncompressors/pow(2, n+1).toInt)(Module(new Merge(pow(2, n).toInt, 4, 0))))
        for (i <- 0 until ncompressors/pow(2, n+1).toInt) {
            header_stages(n)(i).io.len1 := header_stages(n-1)(2*i).io.outlen
            header_stages(n)(i).io.len2 := header_stages(n-1)(2*i+1).io.outlen
            header_stages(n)(i).io.data1 := header_stages(n-1)(2*i).io.out
            header_stages(n)(i).io.data2 := header_stages(n-1)(2*i+1).io.out
        }
    }

    // Is there a better way to do this???
    // Turn the headers from a 4-bit Vec into a 16-bit Vec
    val headerout = Wire(Vec((ncompressors/16*2).ceil.toInt, UInt(16.W)))

    // Construct the 2 bit headers
    val twobit_headers = Wire(Vec(ncompressors, UInt(2.W)))
    for (i <- 0 until ncompressors) {
        when (io.headerin(i) > 2.U) {
            twobit_headers(i) := 2.U
        }.otherwise {
            twobit_headers(i) := io.headerin(i)(1, 0)
        }
    }
    for (i <- 0 until ncompressors/8) {
        headerout(i) := Cat(twobit_headers(8*i), twobit_headers(8*i+1), twobit_headers(8*i+2), twobit_headers(8*i+3), twobit_headers(8*i+4), twobit_headers(8*i+5), twobit_headers(8*i+6), twobit_headers(8*i+7))
    }
    
    // And the optional 4-bit header
    val fourbit_header_16xdivblocks = (0 until ncompressors/4/div).map(x => header_stages(header_stages.length - 1)(0).io.out.asUInt()(div*16*(x+1)-1, div*16*x))
    val fourbit_header_16xdivblocks_length = (((header_stages(header_stages.length - 1)(0).io.outlen +& 3.U) / 4.U) +& (div-1).U) / div.U // Ceil divide length

    // ------ MERGE HIERARCHICAL HEADERS AND DATA ------
    val merger = Module(new MergeAsymmetric(16*div, ncompressors/4/div, 10*ncompressors/div))
    merger.io.data1 := fourbit_header_16xdivblocks
    merger.io.len1 := fourbit_header_16xdivblocks_length
    merger.io.data2 := stages(stages.length-1)(0).io.out
    merger.io.len2 := stages(stages.length-1)(0).io.outlen

    // Turn the output from the last stage back into 16-bit words
    val data_one_uint = merger.io.out.asUInt()
    val data_16_bit_blocks = (0 until 10*ncompressors + (ncompressors/4).toInt).map(x => data_one_uint(16*(x+1) - 1, 16*x))
    val data_16_bit_blocks_length = merger.io.outlen * div.U

    // Output the headers and the data
    io.out := headerout ++ data_16_bit_blocks
    io.outlen := (ncompressors/8).U +& data_16_bit_blocks_length
}

object Reduction4 extends App {
  chisel3.Driver.execute(args, () => new Reduction4)
}