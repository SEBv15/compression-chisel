//
// merger logic
//
// written by Sebastian Strempfer <sebastian@strempfer.com>
//
package lcomp

import chisel3._
import chisel3.util._

class Merge(val inwords:Int = 41, val wordsize:Int = 4, val minwords:Int = 1) extends Module {
    val io = IO(new Bundle {
        val len1 = Input(UInt(log2Ceil(inwords).W))
        val data1 = Input(Vec(inwords, UInt(wordsize.W)))
        val len2 = Input(UInt(log2Ceil(inwords).W))
        val data2 = Input(Vec(inwords, UInt(wordsize.W)))
        val outlen = Output(UInt((log2Ceil(inwords*2)).W))
        val out = Output(Vec(2*inwords, UInt(wordsize.W)))
    })

    io.outlen := io.len1 +& io.len2

    for (i <- 0 until minwords) {
        io.out(i) := io.data1(i)
    }

    for (i <- minwords until inwords) {
        when (i.U < io.len1) {
            io.out(i) := io.data1(i)
        }.elsewhen(i.U < io.len1 +& io.len2) {
            io.out(i) := io.data2(i.U - io.len1)
        }.otherwise {
            io.out(i) := 0.U
        }
    }

    for (i <- inwords until 2*inwords) {
        when (i.U < io.len1 +& io.len2) {
            io.out(i) := io.data2(i.U - io.len1)
        }.otherwise {
            io.out(i) := 0.U
        }
    }
}

object Merge extends App {
    chisel3.Driver.execute(args, () => new Merge)
}