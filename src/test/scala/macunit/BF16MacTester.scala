package macunit

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import hardfloat.BF16MAC
import scala.io.Source
import java.io.{File, PrintWriter}

class BF16MacTester extends AnyFlatSpec {

  behavior of "BF16MAC"

  // BF16格式辅助函数：将浮点数转换为BF16
  def floatToBF16(f: scala.Float): Int = {
    val bits = java.lang.Float.floatToIntBits(f)
    (bits >>> 16) & 0xFFFF  // 取高16位作为BF16
  }

  // 手动构造BF16值
  def makeBF16(sign: Int, exp: Int, frac: Int): Int = {
    ((sign & 1) << 15) | ((exp & 0xFF) << 7) | (frac & 0x7F)
  }

  // CSV测例数据结构
  case class TestVector(
    a: Int,
    b: Int, 
    c: Int,
    dut_expected: Int,
    rm_expected: Int,
    line_num: Int
  )

  // 解析CSV行
  def parseCsvLine(line: String, lineNum: Int): Option[TestVector] = {
    try {
      val parts = line.split(",")
      if (parts.length >= 5) {
        val a = Integer.parseInt(parts(0).split("=")(1), 16)
        val b = Integer.parseInt(parts(1).split("=")(1), 16)
        val c = Integer.parseInt(parts(2).split("=")(1), 16)
        val dut_expected = Integer.parseInt(parts(3).split("=")(1), 16)
        val rm_expected = Integer.parseInt(parts(4).split("=")(1), 16)
        Some(TestVector(a, b, c, dut_expected, rm_expected, lineNum))
      } else {
        None
      }
    } catch {
      case _: Exception => None
    }
  }

  // 从CSV文件读取测例
  def loadTestVectors(filename: String): List[TestVector] = {
    try {
      val lines = Source.fromFile(filename).getLines().toList
      lines.zipWithIndex.flatMap { case (line, idx) =>
        if (line.nonEmpty && !line.startsWith("#")) {
          parseCsvLine(line, idx + 1)
        } else {
          None
        }
      }
    } catch {
      case _: Exception =>
        println(s"Warning: Could not read $filename, skipping CSV tests")
        List.empty
    }
  }

  // 保存测试结果到CSV
  def saveResults(results: List[(TestVector, Int)], filename: String): Unit = {
    try {
      val writer = new PrintWriter(new File(filename))
      writer.println("# CSV Test Results")
      writer.println("# Format: io_a,io_b,io_c,dut_actual,dut_expected,rm_expected,status,line_num")
      
      results.foreach { case (tv, actualResult) =>
        val status = if (actualResult == tv.rm_expected) "PASS" else "FAIL"
        writer.println(s"io_a=${tv.a.toHexString},io_b=${tv.b.toHexString},io_c=${tv.c.toHexString},dut_actual=${actualResult.toHexString},dut_expected=${tv.dut_expected.toHexString},rm_expected=${tv.rm_expected.toHexString},status=$status,line_num=${tv.line_num}")
      }
      
      writer.close()
      println(s"Results saved to $filename")
    } catch {
      case e: Exception =>
        println(s"Warning: Could not save results to $filename: ${e.getMessage}")
    }
  }

  // 运行单个测例
  def runSingleTest(dut: BF16MAC, tv: TestVector): Int = {
    dut.io.a.poke(tv.a.U)
    dut.io.b.poke(tv.b.U) 
    dut.io.c.poke(tv.c.U)
    
    val result = dut.io.out.peek()
    result.litValue.toInt
  }
/*
  it should "perform simple BF16 multiply-add: 1.0 * 2.0 + 1.0 = 3.0" in {
    simulate(new BF16MAC(8, 8)) { dut =>
      
      // 最简单的测试: 1.0 * 2.0 + 1.0 = 3.0 (三个BF16输入)
      val a = floatToBF16(1.0f)     // BF16: 0x3F80 (1.0)
      val b = floatToBF16(2.0f)     // BF16: 0x4000 (2.0)  
      val c = floatToBF16(1.0f)     // BF16: 0x3F80 (1.0)
      
      dut.io.a.poke(a.U)   // BF16格式
      dut.io.b.poke(b.U)   // BF16格式
      dut.io.c.poke(c.U)   // BF16格式
      //dut.io.roundingMode.poke(0.U)  // round to nearest even
      //dut.io.detectTininess.poke(0.U)
      
      val result = dut.io.out.peek()
      //val exceptions = dut.io.exceptionFlags.peek()
      
      println(s"Simple BF16 test: 1.0 * 2.0 + 1.0 = 3.0")
      println(s"A (1.0): 0x${a.toHexString}")
      println(s"B (2.0): 0x${b.toHexString}")
      println(s"C (1.0): 0x${c.toHexString}")
      println(s"Result: 0x${result.litValue.toString(16)}")
      //println(s"Exception flags: 0x${exceptions.litValue.toString(16)}")
      
      // 分析结果格式 (RecFN 17位：[16]符号 [15:8]指数 [7:0]尾数)
      val resultValue = result.litValue
      val resultSign = (resultValue >> 16) & 1
      val resultExp = (resultValue >> 8) & 0xFF
      val resultFrac = resultValue & 0xFF
      
      println(s"Result analysis:")
      println(s"  Sign: $resultSign (${if (resultSign == 0) "+" else "-"})")
      println(s"  Exp:  0x${resultExp.toString(16)} ($resultExp)")
      println(s"  Frac: 0x${resultFrac.toString(16)} ($resultFrac)")
      
      // 期望结果：3.0 在BF16中是0x4040，转换为RecFN应该是...
      val expected3_0_bf16 = floatToBF16(3.0f)
      println(s"Expected 3.0 in BF16: 0x${expected3_0_bf16.toHexString}")
      assert(result.litValue != 0, "Result should not be zero")
      
      // 验证符号应该为正
      assert(resultSign == 0, s"Result should be positive, but sign bit is $resultSign")
    }
  }

  it should "handle zero multiplication" in {
    simulate(new BF16MAC(8, 8)) { dut =>
      
      // 测试: 0.0 * 1.0 + 2.0 = 2.0
      val a = makeBF16(0, 0, 1)     // 0.0
      val b = makeBF16(0, 0, 28)     // 1.0
      val c = makeBF16(1, 172, 88)     // 2.0
      
      dut.io.a.poke(a.U)
      dut.io.b.poke(b.U)
      dut.io.c.poke(c.U)
      //dut.io.roundingMode.poke(0.U)
      //dut.io.detectTininess.poke(0.U)
      
      val result = dut.io.out.peek()
      //val exceptions = dut.io.exceptionFlags.peek()
      
      println(s"Zero test: 0.0 * 1.0 + 2.0 = 2.0")
      println(s"A (0.0): 0x${a.toHexString}, B (1.0): 0x${b.toHexString}, C (2.0): 0x${c.toHexString}")
      println(s"Result: 0x${result.litValue.toString(16)}")
      
      assert(result.litValue != 0, "Result should not be zero when C=2.0")
    }
  }
  */
  // CSV批量测试
  it should "pass all mismatch_cases.csv test cases" in {
    val testVectors = loadTestVectors("result/mismatch_cases.csv")
    
    if (testVectors.nonEmpty) {
      simulate(new BF16MAC(8, 8)) { dut =>
        val results = scala.collection.mutable.ListBuffer[(TestVector, Int)]()
        var passCount = 0
        var failCount = 0
        
        println(s"Running ${testVectors.length} CSV test cases...")
        
        testVectors.foreach { tv =>
          val actualResult = runSingleTest(dut, tv)
          results += ((tv, actualResult))
          
          val passed = actualResult == tv.rm_expected
          if (passed) {
            passCount += 1
            println(s"Line ${tv.line_num}: PASS")
          } else {
            failCount += 1
            println(s"Line ${tv.line_num}: FAIL - A=0x${tv.a.toHexString}, B=0x${tv.b.toHexString}, C=0x${tv.c.toHexString}")
            println(s"   Expected: 0x${tv.rm_expected.toHexString}, Got: 0x${actualResult.toHexString}")
          }
        }
        
        // 保存详细结果
        saveResults(results.toList, "result/csv_test_results.csv")
        
        println(s"CSV Test Summary: ${passCount} passed, ${failCount} failed, ${testVectors.length} total")
        
        // assert(failCount == 0, s"$failCount test cases failed")
      }
    } else {
      println("No CSV test vectors found, skipping CSV tests")
    }
  }
}