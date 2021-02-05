package lcomp

import chisel3._
import chisel3.util._

import scala.math.pow

/** Encoding module that takes in a 15-bit word and outputs a 7-bit encoded version if possible. The encoded word is made up of 4 bits saying where a run of 1s starts, and 3-bits indicating how long it is.
 *  This module is not as efficiently using the full 7 bits, but I cannot figure out a simple way to use all possible 7-bit numbers.
 */
class PatternEncoder extends Module {
    val io = IO(new Bundle {
        val in = Input(UInt(15.W))
        val canencode = Output(Bool())
        val out = Output(UInt(7.W))
    })

    // Calculate how many times consecutive bits are different to determine if the input can be encoded
    val enc = Wire(Bool())
    val encw = Wire(Vec(15, UInt(4.W)))
    encw(0) := 0.U
    for (i <- 0 until 14) {
        encw(i+1) := encw(i) + (io.in(i)^io.in(i+1))
    }
    enc := (encw(14) === 2.U && io.in(14) === 0.U) || (encw(14) === 1.U && (io.in(14) === 1.U || io.in(0) === 1.U)) || io.in === 0.U

    // Calculate the position and length
    val posw = Wire(Vec(15, UInt(4.W)))
    val lenw = Wire(Vec(15, UInt(4.W)))
    posw(0) := 0.U
    lenw(0) := io.in(0)
    for (i <- 1 until 15) {
        when (io.in(i)) {
            when (lenw(i-1) === 0.U) {
                posw(i) := i.U
                lenw(i) := 1.U
            }.otherwise {
                posw(i) := posw(i-1)
                lenw(i) := lenw(i-1) + 1.U
            }
        }.otherwise {
            lenw(i) := lenw(i-1)
            posw(i) := posw(i-1)
        }
    }

    io.canencode := enc && lenw(14) <= 7.U
    io.out := posw(14) ## lenw(14)(2, 0)
}

object PatternEncoder extends App {
    chisel3.Driver.execute(args, () => new PatternEncoder)
}