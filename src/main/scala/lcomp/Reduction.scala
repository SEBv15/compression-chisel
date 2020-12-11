//
// reduction logic that combines Merge modules
//
// written by Sebastian Strempfer <sebastian@strempfer.com>
//
package lcomp

import chisel3._
import chisel3.util._

class Reduction(val ncompressors:Int = 64) extends Module {
    require(isPow2(ncompressors))
    require(ncompressors == 64)

    val io = IO(new Bundle {
        val datain = Input(Vec(ncompressors, Vec(10, UInt(16.W))))
        val headerin = Input(Vec(ncompressors, UInt(4.W)))
        val out = Output(UInt((ncompressors * 164).W))
    })

    val startData = Wire(Vec(ncompressors, Vec(41, UInt(4.W))))
    for (i <- 0 until ncompressors) {
        startData(i)(0) := io.headerin(i)
        val datb = io.datain(i).asUInt().asBools()
        for (j <- 0 until 40) {
            startData(i)(j + 1) := Cat(datb(j*4), datb(j*4+1), datb(j*4+2), datb(j*4+3))
        }
    }

    val stage1 = List.fill(ncompressors/2)(Module(new Merge(41, 4, 1)))
    for (i <- 0 until ncompressors/2) {
        stage1(i).io.len1 := io.headerin(2*i)*4.U + 1.U // data length + header
        stage1(i).io.len2 := io.headerin(2*i+1)*4.U + 1.U
        stage1(i).io.data1 := startData(2*i)
        stage1(i).io.data2 := startData(2*i+1)
    }

    // There is definitely a better way to do this, but I don't know how
    val stage2 = List.fill(ncompressors/4)(Module(new Merge(41, 8, 1)))
    for (i <- 0 until ncompressors/4) {
        stage2(i).io.len1 := stage1(2*i).io.outlen / 2.U // 82 4-bit to 41 8-bit
        stage2(i).io.len2 := stage1(2*i+1).io.outlen / 2.U
        for (j <- 0 until 41) {
            stage2(i).io.data1(j) := Cat(stage1(2*i).io.out(2*j), stage1(2*i).io.out(2*j+1))
            stage2(i).io.data2(j) := Cat(stage1(2*i+1).io.out(2*j), stage1(2*i+1).io.out(2*j+1))
        }
    }

    val stage3 = List.fill(ncompressors/8)(Module(new Merge(41, 16, 1)))
    for (i <- 0 until ncompressors/8) {
        stage3(i).io.len1 := stage2(2*i).io.outlen / 2.U // 82 8-bit to 41 16-bit
        stage3(i).io.len2 := stage2(2*i+1).io.outlen / 2.U
        for (j <- 0 until 41) {
            stage3(i).io.data1(j) := Cat(stage2(2*i).io.out(2*j), stage2(2*i).io.out(2*j+1))
            stage3(i).io.data2(j) := Cat(stage2(2*i+1).io.out(2*j), stage2(2*i+1).io.out(2*j+1))
        }
    }

    val stage4 = List.fill(ncompressors/16)(Module(new Merge(41*2, 16, 2)))
    for (i <- 0 until ncompressors/16) {
        stage4(i).io.len1 := stage3(2*i).io.outlen
        stage4(i).io.len2 := stage3(2*i+1).io.outlen
        stage4(i).io.data1 := stage3(2*i).io.out
        stage4(i).io.data2 := stage3(2*i+1).io.out
    }

    val stage5 = List.fill(ncompressors/32)(Module(new Merge(41*4, 16, 4)))
    for (i <- 0 until ncompressors/32) {
        stage5(i).io.len1 := stage4(2*i).io.outlen
        stage5(i).io.len2 := stage4(2*i+1).io.outlen
        stage5(i).io.data1 := stage4(2*i).io.out
        stage5(i).io.data2 := stage4(2*i+1).io.out
    }

    val stage6 = List.fill(ncompressors/64)(Module(new Merge(41*8, 16, 8)))
    for (i <- 0 until ncompressors/64) {
        stage6(i).io.len1 := stage5(2*i).io.outlen
        stage6(i).io.len2 := stage5(2*i+1).io.outlen
        stage6(i).io.data1 := stage5(2*i).io.out
        stage6(i).io.data2 := stage5(2*i+1).io.out
    }

    /*
    var stages = List.fill(log2Up(ncompressors))(List[Module])
    var stageout = Vec
    stages(0) = stage1
    for (i <- 1 until log2Up(ncompressors)) {
        val power = (1 << (i+1))
        stages(i) = List.fill(ncompressors/power)(Module(new Merge(41*power/2, min(2*power, 16), 2*power/min(2*power, 16))))
        for (i <- 0 until ncompressors/power) {
            stages(i).io.len1 := stages(i-1)(2*i).io.outlen
            stages(i).io.len2 := stages(i-1)(2*i+1).io.outlen
            stages(i).io.data1 := stages(i-1)(2*i).io.out
            stages(i).io.data2 := stages(i-1)(2*i+1).io.out
        }
    }
    */

    io.out := stage6(0).io.out.asUInt()
}

object Reduction extends App {
  chisel3.Driver.execute(args, () => new Reduction)
}