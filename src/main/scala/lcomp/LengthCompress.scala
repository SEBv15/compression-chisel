//
// compression logic
//
// written by Sebastian Strempfer <sebastian@strempfer.com>
//
package lcomp

import chisel3._
import chisel3.util._

class LengthCompress(val npixels:Int = 16, val pixelsize:Int = 10) extends Module {
    val io = IO(new Bundle {
        val in  = Input( Vec(npixels, UInt(pixelsize.W)))
        val data = Output(Vec(pixelsize, UInt(npixels.W)))
        val header = Output(UInt(log2Ceil(pixelsize).W))
    })

    val shuffle = Module(new BitShufflePerChannel(npixels, pixelsize))

    // Shuffle the bits and output
    shuffle.io.in := io.in
    io.data := shuffle.io.out

    // There is probably a better way, but this works
    val lengths = Wire(Vec(pixelsize, UInt(log2Ceil(pixelsize).W)))
    for (i <- 0 until pixelsize) {
        lengths(i) := Mux(shuffle.io.out(i) === 0.U, if (i > 0) lengths(i - 1) else {0.U}, (i+1).U)
    }
    io.header := lengths(pixelsize-1)
}

object LengthCompress extends App {
  chisel3.Driver.execute(args, () => new LengthCompress)
}