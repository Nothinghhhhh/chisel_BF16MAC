package macunit

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import hardfloat.MulAddRecFN

class MulAddRecFNDebugTest extends AnyFlatSpec {

  behavior of "MulAddRecFN Debug Test"

  // BF16格式辅助函数
  def makeBF16(sign: Int, exp: Int, frac: Int): Int = {
    ((sign & 1) << 15) | ((exp & 0xFF) << 7) | (frac & 0x7F)
  }

  def parseHex(hexStr: String): Int = {
    Integer.parseInt(hexStr.trim, 16)
  }

  it should "test debug signals with LSB case 1" in {
    simulate(new MulAddRecFN(8, 8)) { dut =>
      
      // 使用LSB.csv中的第一行数据
      // io_a=807f, io_b=37d2, io_c=7f, dut_actual=7f, rm_expected=7e
      val a = parseHex("807f")  // 输入A
      val b = parseHex("37d2")  // 输入B  
      val c = parseHex("7f")    // 输入C
      val expected = parseHex("7e")  // 期望结果

      println(s"=== MulAddRecFN Debug Test ===")
      println(s"Input A: 0x${a.toHexString}")
      println(s"Input B: 0x${b.toHexString}")
      println(s"Input C: 0x${c.toHexString}")
      println(s"Expected: 0x${expected.toHexString}")

      // 设置输入
      dut.io.a.poke(a.U)
      dut.io.b.poke(b.U)
      dut.io.c.poke(c.U)
      
      // 获取结果和debug信号（组合逻辑，无需时钟）
      val result = dut.io.out.peek()
      val debugAlignedSigC = dut.io.dbg_alignedSigC.peek()
      val debugRawSig = dut.io.dbg_rawSig.peek()
      
      println(s"Actual Result: 0x${result.litValue.toString(16)}")
      println(s"Debug AlignedSigC: 0x${debugAlignedSigC.litValue.toString(16)}")
      println(s"Debug RawSig: 0x${debugRawSig.litValue.toString(16)}")
      
      // 检查结果是否匹配期望值
      val actualResult = result.litValue.toInt
      println(s"Result matches expected: ${actualResult == expected}")
      
      // 输出详细的debug信息
      println(s"\n=== Debug Information ===")
      println(s"Result: 0x${actualResult.toHexString} (${actualResult})")
      println(s"Expected: 0x${expected.toHexString} (${expected})")
      println(s"Difference: ${actualResult - expected}")
      
      // 验证结果（这里我们只是打印，不强制断言失败）
      if (actualResult != expected) {
        println(s"WARNING: Result mismatch! Expected 0x${expected.toHexString}, got 0x${actualResult.toHexString}")
      } else {
        println(s"SUCCESS: Result matches expected value!")
      }
    }
  }

  it should "test debug signals with LSB case 2" in {
    simulate(new MulAddRecFN(8, 8)) { dut =>
      
      // 使用LSB.csv中的第二行数据
      // io_a=7799, io_b=68, io_c=7f, dut_actual=3879, rm_expected=3878
      val a = parseHex("7799")  // 输入A
      val b = parseHex("68")    // 输入B  
      val c = parseHex("7f")    // 输入C
      val expected = parseHex("3878")  // 期望结果

      println(s"\n=== MulAddRecFN Debug Test Case 2 ===")
      println(s"Input A: 0x${a.toHexString}")
      println(s"Input B: 0x${b.toHexString}")
      println(s"Input C: 0x${c.toHexString}")
      println(s"Expected: 0x${expected.toHexString}")

      // 设置输入
      dut.io.a.poke(a.U)
      dut.io.b.poke(b.U)
      dut.io.c.poke(c.U)
      
      // 获取结果和debug信号（组合逻辑，无需时钟）
      val result = dut.io.out.peek()
      val debugAlignedSigC = dut.io.dbg_alignedSigC.peek()
      val debugRawSig = dut.io.dbg_rawSig.peek()
      
      println(s"Actual Result: 0x${result.litValue.toString(16)}")
      println(s"Debug AlignedSigC: 0x${debugAlignedSigC.litValue.toString(16)}")
      println(s"Debug RawSig: 0x${debugRawSig.litValue.toString(16)}")
      
      // 检查结果
      val actualResult = result.litValue.toInt
      println(s"Result matches expected: ${actualResult == expected}")
      
      if (actualResult != expected) {
        println(s"WARNING: Result mismatch! Expected 0x${expected.toHexString}, got 0x${actualResult.toHexString}")
      } else {
        println(s"SUCCESS: Result matches expected value!")
      }
    }
  }
} 