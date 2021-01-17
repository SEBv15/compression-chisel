package lcomp

import chisel3._
import chisel3.util._

import scala.collection.mutable.ListBuffer
import scala.math.pow

/** Merges the output of many compressors into one semi-continous word using hierarchical reduction.
 *  
 *  @constructor Create a compression module
 *  @param ncompressors The number of input compressor (expected to have vec of ten 16-bit UInts as output)
 *  @param maxblocks The maximum number of blocks/elements/words any merge stage should have as input (0 = no limit)
 */
class Deduction(val ncompressors:Int = 64, val maxblocks:Int = 128, val bits_per_pixel:Int = 10, val insize:Int = 1024) extends Module {
    require(insize % 16 == 0)
    require(ncompressors/8 + ncompressors/4 <= insize/16)

    val poswidth = log2Floor(ncompressors*16*bits_per_pixel) + 1

    val io = IO(new Bundle {
        val in = Input(Vec(insize/16, UInt(16.W)))
        val number = Input(UInt((log2Floor(ncompressors*bits_per_pixel*16/insize)+1).W))
        val out = Output(Vec(insize/16, UInt(16.W))) // Elements to write to memory
        val pos = Output(Vec(insize/16, UInt(poswidth.W))) // Where to write them to memory
        val write = Output(Vec(insize/16, Bool())) // If you should really do it though
    })

    io.out := io.in

    // Registers to store the length data for the following blocks which use the same headers
    val lengthsreg = List.fill(ncompressors)(RegInit(0.U))
    // Wire which will contain the lengths for this clock tick's deduction operations
    val lengths = Wire(Vec(ncompressors, UInt(4.W)))
    val nheaderblocksreg = RegInit(0.U)
    val nheaderblocks = Wire(UInt())

    when (io.number === 0.U) {
        // Set up list of compressor output lengths from headers
        val slengths = Wire(Vec(ncompressors, UInt(2.W)))
        val blengths = Wire(Vec(ncompressors, UInt(4.W)))
        val lindices = Wire(Vec(ncompressors, UInt((log2Floor(ncompressors) + 1).W)))
        lindices(0) := 0.U

        // Put the 2 bit headers in a list
        for (i <- 0 until ncompressors/8) {
            for (j <- 0 until 8) {
                slengths(8*i+(8-j-1)) := (io.in(i))(2*(j+1)-1, 2*j)
            }
        }
        // Put the 4 bit headers in a list
        for (i <- 0 until ncompressors/4) {
            for (j <- 0 until 4) {
                blengths(4*i+(4-j-1)) := (io.in(ncompressors/8 + i))(4*(j+1)-1, 4*j)
            }
        }
        // Create a list which contains the index of the corresponding 4-bit header
        for (i <- 1 until ncompressors) {
            lindices(i) := lindices(i-1) +& (slengths(i-1) === 3.U)
        }
        // Write the final lengths (based on 2 and 4-bit headers) into registers
        for (i <- 0 until ncompressors) {
            when (slengths(i) === 3.U) {
                lengths(i) := blengths(lindices(i))
            }.otherwise {
                lengths(i) := slengths(i)
            }
            lengthsreg(i) := lengths(i)
        }

        nheaderblocks := (ncompressors/8).U +& (lindices(ncompressors-1) +& (slengths(ncompressors-1) === 3.U) +& 3.U) / 4.U
        nheaderblocksreg := nheaderblocks
    }.otherwise {
        // If the block doesn't have the headers, copy in the value from the registers
        for (i <- 0 until ncompressors) {
            lengths(i) := lengthsreg(i)
        }
        nheaderblocks := nheaderblocksreg
    }

    // Generate a table of the lengths for every stage
    val slengths = new ListBuffer[Vec[UInt]]()
    slengths.append(Wire(Vec(ncompressors, UInt(4.W))))
    for (i <- 0 until ncompressors) {
        slengths(0)(i) := lengths(i)
    }

    var n = 10
    var m = 2
    for (i <- 1 until log2Floor(ncompressors) + 1) {
        slengths.append(Wire(Vec(ncompressors/(1 << i), UInt((log2Floor(n) + 1).W))))
        for (j <- 0 until ncompressors/(1 << i)) {
            if (n > maxblocks) {
                val t = Wire(UInt())
                t := slengths(i-1)(2*j) +& slengths(i-1)(2*j+1)
                //slengths(i)(j) := t +& ((m.U - (t % m.U)) % m.U) // round up to a multiple of m
                slengths(i)(j) := ((t +& (m-1).U)/m.U)*m.U // round up to a multiple of m
            } else {
                slengths(i)(j) := slengths(i-1)(2*j) +& slengths(i-1)(2*j+1)
            }
        }
        if (n > maxblocks) {
            m *= 2
        }
        n *= 2
    }

    // UInt to store the position offset of this block
    val offset = Wire(UInt(poswidth.W))
    offset := io.number * (insize/16).U

    // Go through every element and calculate where it belongs
    for (i <- 0 until insize/16) {
        val poss = ListBuffer[UInt]()
        poss.append(Wire(UInt(poswidth.W)))
        poss(0) := offset +& i.U // Where the element is in the reduced output

        poss.append(Wire(UInt(poswidth.W)))
        // Check if it got moved in the final merge stage
        val finallen = Wire(UInt())
        val finalpivot = Wire(UInt())
        finallen := slengths(slengths.length - 1)(0) +& nheaderblocks
        when (finallen > (ncompressors/8 + ncompressors/4).U) {
            finalpivot := finallen
        }.otherwise {
            finalpivot := (ncompressors/8 + ncompressors/4).U
        }

        when (io.number === 0.U && poss(0) < (ncompressors/8 + ncompressors/4).U) {
            poss(1) := poss(0) -& nheaderblocks +& finalpivot
        }.otherwise {
            poss(1) := poss(0)
        }

        // Subtract the header length
        poss.append(Wire(UInt(poswidth.W)))
        poss(2) := poss(1) -& (ncompressors/8 + ncompressors/4).U

        // Go through every stage and calculate what the position was before that stage. Aka checking if the current position is in the gap and adding the length of the second input vec to it if it is.
        var nmergers = 1
        for (j <- 0 until log2Floor(ncompressors)) {
            val stage = log2Floor(ncompressors) - j - 1

            // Get the lengths of the input data in the merge stage
            val len1 = Wire(UInt())
            val len2 = Wire(UInt())

            len1 := slengths(stage)((poss(poss.length-1) / (ncompressors*10/nmergers).U) << 1)
            len2 := slengths(stage)(((poss(poss.length-1) / (ncompressors*10/nmergers).U) << 1) +& 1.U)

            // Calculate what the position of the data in the merge stage is
            val posinmerger = Wire(UInt())
            posinmerger := poss(poss.length-1) % (ncompressors*10/nmergers).U

            val len = Wire(UInt())
            val pivot = Wire(UInt())
            len := len1 +& len2
            when (len > (ncompressors*10/nmergers/2).U) {
                pivot := len
            }.otherwise {
                pivot := (ncompressors*10/nmergers/2).U
            }

            // Check if the position changed in the stage, and do the change
            poss.append(Wire(UInt(poswidth.W)))
            when (posinmerger >= len1 && posinmerger < (ncompressors*10/nmergers/2).U) {
                poss(poss.length-1) := poss(poss.length-2) -& len1 +& pivot
            }.otherwise {
                poss(poss.length-1) := poss(poss.length-2)
            }
            nmergers *= 2
        }

        io.pos(i) := poss(poss.length-1)
    }

    // Check if data should really be written to the calculated position
    when (io.number === 0.U) { // When it is the first part, there are headers in the beginning that need to be ignored
        for (i <- 0 until ncompressors/8) {
            io.write(i) := 0.B
        }
        for (i <- ncompressors/8 until ncompressors/8 + ncompressors/4) {
            when (i.U < nheaderblocks || i.U >= nheaderblocks +& slengths(slengths.length-1)(0)) {
                io.write(i) := 0.B
            }.otherwise {
                io.write(i) := 1.B
            }
        }
    }.otherwise {
        for (i <- 0 until ncompressors/8 + ncompressors/4) {
            when (i.U +& offset >= nheaderblocks +& slengths(slengths.length-1)(0)) { // Check if element is within the length
                io.write(i) := 0.B
            }.otherwise {
                io.write(i) := 1.B
            }            
        }
    }
    for (i <- ncompressors/8 + ncompressors/4 until insize/16) {
        when (i.U +& offset >= nheaderblocks +& slengths(slengths.length-1)(0)) { // Check if element is within the length
            io.write(i) := 0.B
        }.otherwise {
            io.write(i) := 1.B
        }   
    }
}

object Deduction extends App {
    chisel3.Driver.execute(args, () => new Deduction)
}
