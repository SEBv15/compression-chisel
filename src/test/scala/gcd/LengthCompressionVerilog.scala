package lcomp
import chisel3._

object LengthCompressionVerilog extends App {
  chisel3.Driver.execute(args, () => new LengthCompress)
}