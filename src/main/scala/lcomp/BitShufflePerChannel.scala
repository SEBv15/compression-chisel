//
// bit shuffle logic
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package lcomp

import chisel3._
import chisel3.util._  // Cat() is there

// nelems : the number elems per channel
// elemsize : the number of bits per elem
/*
shuffle bits between pixels to group bits by significance into "pixels"
*/
class BitShufflePerChannel(val nelems:Int = 16, val elemsize:Int = 10) extends Module {

  val io = IO(new Bundle {
    val in  = Input( Vec(nelems, UInt(elemsize.W)))
    val out = Output(Vec(elemsize, UInt(nelems.W)))
  })

  for (i <- 0 until elemsize) {
    val tmp = Wire(Vec(nelems, Bits(1.W)))
    for (j <- 0 until nelems) {
      tmp(j) := io.in(j)(i)
    }
    io.out(i) := Reverse(Cat(tmp))
  }
}