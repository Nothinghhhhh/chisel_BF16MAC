package macunit

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import hardfloat.MulAddRecFN

class SimpleDebugTest extends AnyFlatSpec {

  behavior of "MulAddRecFN Simple Debug Test"

  it should "test debug signals with simple case" in {
    simulate(new MulAddRecFN(8, 8)) { dut =>
      
      // 使用LSB.csv中的第一行数据：io_a=807f, io_b=37d2, io_c=7f
      val a = 0x807f  // 输入A
      val b = 0x37d2  // 输入B  
      val c = 0x7f    // 输入C

      println(s"=== Simple Debug Test ===")
      println(s"Input A: 0x${a.toHexString}")
      println(s"Input B: 0x${b.toHexString}")
      println(s"Input C: 0x${c.toHexString}")

      // 设置输入
      dut.io.a.poke(a.U)
      dut.io.b.poke(b.U)
      dut.io.c.poke(c.U)
      
      // 获取结果和debug信号（组合逻辑，无需时钟）
      val result = dut.io.out.peek()
      val debugAlignedSigC = dut.io.dbg_alignedSigC.peek()
      val debugRawSig = dut.io.dbg_rawSig.peek()
      
      println(s"Result: 0x${result.litValue.toString(16)}")
      println(s"Debug AlignedSigC: 0x${debugAlignedSigC.litValue.toString(16)}")
      println(s"Debug RawSig: 0x${debugRawSig.litValue.toString(16)}")
      
      // 验证debug信号不为零（基本功能验证）
      assert(debugAlignedSigC.litValue > 0, "Debug AlignedSigC should not be zero")
      assert(debugRawSig.litValue > 0, "Debug RawSig should not be zero")
      
      println(s"SUCCESS: Debug signals are working!")
    }
  }
} 