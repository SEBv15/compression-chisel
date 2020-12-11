package lcomp

import chisel3._
import chisel3.util._

import scala.collection.mutable.ListBuffer
import scala.math.pow

class Reduction2(val ncompressors:Int = 64) extends Module {
    require(isPow2(ncompressors))
    require(ncompressors >= 4) // 4-bit headers need to be grouped into 16-bit UInts

    val io = IO(new Bundle {
        val datain = Input(Vec(ncompressors, Vec(10, UInt(16.W))))
        val headerin = Input(Vec(ncompressors, UInt(4.W)))
        //val out = Output(UInt((ncompressors * 164).W))
        val out = Output(Vec(ncompressors*10 + ncompressors/4, UInt(16.W))) // First ncompressors/4 words are the headers and the rest is the data
    })

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

    // Add stages as needed and make them take the previous stage as input
    for (n <- 1 until log2Up(ncompressors)) {
        stages.append(ListBuffer.fill(ncompressors/pow(2, n+1).toInt)(Module(new Merge(10*pow(2, n).toInt, 16, 0))))
        for (i <- 0 until ncompressors/pow(2, n+1).toInt) {
            stages(n)(i).io.len1 := stages(n-1)(2*i).io.outlen
            stages(n)(i).io.len2 := stages(n-1)(2*i+1).io.outlen
            stages(n)(i).io.data1 := stages(n-1)(2*i).io.out
            stages(n)(i).io.data2 := stages(n-1)(2*i+1).io.out
        }
    }

    // Is there a better way to do this???
    // Turn the headers from a 4-bit Vec into a 16-bit Vec
    val headerout = Wire(Vec(ncompressors/4, UInt(16.W)))
    for (i <- 0 until ncompressors/4) {
        headerout(i) := Cat(io.headerin(4*i), io.headerin(4*i+1), io.headerin(4*i+2), io.headerin(4*i+3))
    }

    // Output the headers and the data
    io.out := headerout ++ stages(stages.length-1)(0).io.out
}

object Reduction2 extends App {
  chisel3.Driver.execute(args, () => new Reduction2)
}