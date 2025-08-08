package hardfloat

import chisel3._
import chisel3.util._

class BF16ToRawBF16_flushDenormal(expWidth: Int, sigWidth: Int) extends RawModule {
    val io = IO(new Bundle {
        val in = Input(Bits((expWidth + sigWidth).W))
        val out = Output(new RawBF16(expWidth, sigWidth))
    })

    val sign = io.in(expWidth + sigWidth - 1)
    val expIn = io.in(expWidth + sigWidth - 2, expWidth + sigWidth - 9)
    val fractIn = io.in(expWidth + sigWidth - 10, 0)

    val isZeroExpIn = (expIn === 0.U)
    val isZeroFractIn = fractIn === 0.U

    val isZero = isZeroExpIn && isZeroFractIn
    val isDenormal = isZeroExpIn && !isZeroFractIn
    val isSpecial = expIn === 255.U
    val Exp = Mux(isDenormal, 0.U(expWidth.W), expIn)

    io.out.isNaN := isSpecial && !isZeroFractIn
    io.out.isInf := isSpecial && isZeroFractIn
    io.out.isZero := isZero         
    io.out.sign := sign

    io.out.sExp := Exp.zext
    io.out.sig := 0.U(1.W) ## !isZero ## Mux(isDenormal, 0.U((sigWidth - 1).W), fractIn)
}