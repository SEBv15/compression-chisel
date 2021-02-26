package lcomp

import chisel3._
import chisel3.util._

import scala.math.pow

/** Encoding module that takes in a 15-bit word and outputs a 7-bit encoded version if possible. The encoded word is made up of 4 bits saying where a run of 1s starts, and 3-bits indicating how long it is.
 *  This module is not as efficiently using the full 7 bits, but I cannot figure out a simple way to use all possible 7-bit numbers.
 */
class CompressWithEncoder extends Module {
    val io = IO(new Bundle {
        val in = Input(Vec(16, UInt(10.W)))
        val shouldencode = Input(Bool())
        val data = Output(Vec(20, UInt(8.W)))
        val length = Output(UInt(5.W))
        val header = Output(UInt(4.W))
    })

    val compressor = Module(new LengthCompress(16, 10))
    for (i <- 0 until 15) {
        compressor.io.in(i) := io.in(i)
    }
    when (io.shouldencode) {
        compressor.io.in(15) := 0.U
    }.otherwise {
        compressor.io.in(15) := io.in(15)
    }

    io.header := compressor.io.header

    val wordlen = Wire(Vec(10, UInt(2.W)))
    val out8 = Wire(Vec(20, UInt(8.W)))

    for (i <- 0 until 10) {
        val encoder = Module(new PatternEncoder())
        val thislen = Wire(UInt(2.W))
        encoder.io.in := compressor.io.data(i)(15, 0)
        when (io.shouldencode && encoder.io.canencode && i.U < compressor.io.header) {
            out8(2*i) := Cat(1.U, encoder.io.out)
            thislen := 1.U
        }.otherwise {
            when (io.shouldencode) {
                out8(2*i) := Cat(0.U, compressor.io.data(i)(15, 8))
            }.otherwise {
                out8(2*i) := compressor.io.data(i)(15, 8)
            }
            thislen := 2.U
        }
        when (i.U < compressor.io.header) {
            wordlen(i) := thislen
        }.otherwise {
            wordlen(i) := 0.U
        }
        out8(2*i+1) := compressor.io.data(i)(7, 0)
    }

    var stage1 = List.fill(4)(Module(new Merge(2, 8, 1)))
    for (i <- 0 until 4) {
        stage1(i).io.len1 := wordlen(2*i)
        stage1(i).io.len2 := wordlen(2*i+1)
        stage1(i).io.data1(0) := out8(4*i)
        stage1(i).io.data1(1) := out8(4*i+1)
        stage1(i).io.data2(0) := out8(4*i+2)
        stage1(i).io.data2(1) := out8(4*i+3)
    }
    var stage2 = List.fill(2)(Module(new Merge(4, 8, 2)))
    for (i <- 0 until 2) {
        stage2(i).io.len1 := stage1(2*i).io.outlen
        stage2(i).io.len2 := stage1(2*i+1).io.outlen
        stage2(i).io.data1 := stage1(2*i).io.out
        stage2(i).io.data2 := stage1(2*i+1).io.out
    }
    var stage3 = Module(new Merge(8, 8, 4))
    stage3.io.len1 := stage2(0).io.outlen
    stage3.io.len2 := stage2(1).io.outlen
    stage3.io.data1 := stage2(0).io.out
    stage3.io.data2 := stage2(1).io.out
    var lasttwo = Module(new Merge(2, 8, 1))
    lasttwo.io.len1 := wordlen(8)
    lasttwo.io.len2 := wordlen(9)
    lasttwo.io.data1(0) := out8(16)
    lasttwo.io.data1(1) := out8(17)
    lasttwo.io.data2(0) := out8(18)
    lasttwo.io.data2(1) := out8(19)
    var finalmerge = Module(new MergeAsymmetric(8, 16, 4, false, false, 8))
    finalmerge.io.len1 := stage3.io.outlen
    finalmerge.io.data1 := stage3.io.out
    finalmerge.io.len2 := lasttwo.io.outlen
    finalmerge.io.data2 := lasttwo.io.out

    io.length := finalmerge.io.outlen
    io.data := finalmerge.io.out
}

object CompressWithEncoder extends App {
    chisel3.Driver.execute(args, () => new CompressWithEncoder)
}