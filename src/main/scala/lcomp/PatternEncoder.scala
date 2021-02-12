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
    val encw = Wire(Vec(15, UInt(4.W)))
    encw(0) := 0.U
    for (i <- 0 until 14) {
        encw(i+1) := encw(i) + (io.in(i)^io.in(i+1))
    }
    //enc := (encw(14) === 2.U && io.in(14) === 0.U) || (encw(14) === 1.U && (io.in(14) === 1.U || io.in(0) === 1.U)) || io.in === 0.U
    io.canencode := encw(14) <= 2.U && ~(encw(14) === 2.U && io.in(14) === 1.U && io.in(0) === 1.U) && io.in != 0.U // bits need to only have changed at most 2 times, and check for the 100000000000001 case, and cannot be zero

    // Calculate the position
    val posw = Wire(Vec(15, UInt(4.W)))
    when (io.in(0)) {
        posw(0) := 0.U
    }.otherwise {
        posw(0) := (0xf).U // Use 0xf as a placeholder since it is not a valid position
    }
    for (i <- 1 until 15) {
        // When the bit is 1 and 0xf indicates that it is the first 1, save it as the start of the run
        when (io.in(i) && posw(i-1) === (0xf).U) {
            posw(i) := i.U
        }.otherwise {
            posw(i) := posw(i-1)
        }
    }

    // Get the run length by simply counting the number of 1s
    val len = Wire(UInt(4.W))
    len := PopCount(io.in) - 1.U

    when (len(3) === 0.U) {
        io.out := posw(14) ## len(2, 0)
    }.otherwise {
        io.out := (15.U - posw(14))(3, 0) ## (6.U - len(2, 0))(2, 0)
    }
}

object PatternEncoder extends App {
    chisel3.Driver.execute(args, () => new PatternEncoder)
}