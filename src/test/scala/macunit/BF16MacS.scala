package macunit

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import hardfloat.BF16MAC
import scala.io.Source
import java.io.{File, PrintWriter}

class BF16MacS extends AnyFlatSpec {

  behavior of "BF16MAC"

  // BF16格式辅助函数：将浮点数转换为BF16
  def floatToBF16(f: scala.Float): Int = {
    val bits = java.lang.Float.floatToIntBits(f)
    (bits >>> 16) & 0xFFFF  // 取高16位作为BF16
  }
  def makeBF16(sign: Int, exp: Int, frac: Int): Int = {
    ((sign & 1) << 15) | ((exp & 0xFF) << 7) | (frac & 0x7F)
  }

  it should "handle positive infinity" in {
    simulate(new BF16MAC(8, 8)) { dut =>
      
      // 测试: ∞ * 1.0 + 0.0 = ∞
      val negInf = makeBF16(1, 255, 0)  // +∞
      val c = makeBF16(1, 254, 127)     // 1.0
      val zero = floatToBF16(0.0f)      // 0.0
      val nan = makeBF16(1, 255, 127)    // NaN

      dut.io.a.poke(zero.U)
      dut.io.b.poke(negInf.U)
      dut.io.c.poke(c.U)
      //dut.io.roundingMode.poke(0.U)
      //dut.io.detectTininess.poke(0.U)
      
      val result = dut.io.out.peek()
      
      println(s"Infinity test: ∞ * 1.0 + 0.0 = ∞")
      println(s"A (0): 0x${negInf.toHexString}, B (-Inf): 0x${c.toHexString}, C (ff7f big nm): 0x${zero.toHexString}")
      println(s"Result: 0x${result.litValue.toString(16)}")
      
      // 期望结果应该是正无穷: 0x7F80
      val expectedInf = makeBF16(0, 255, 0)
      println(s"Expected ∞: 0x${expectedInf.toHexString}")
    }
  }
}