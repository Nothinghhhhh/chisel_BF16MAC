package macunit

import chisel3._
import chisel3.stage.ChiselGeneratorAnnotation
import _root_.circt.stage.{ChiselStage, FirtoolOption}

// object GenerateVerilog extends App {
//   (new ChiselStage).execute(
//     Array("--target-dir", "generated"),
//     Seq(ChiselGeneratorAnnotation(() => new MyAdderExample(32)))
//   )
// }

import macunit._

object GenerateVerilog extends App {
  (new ChiselStage).execute(
    Array("--target", "systemverilog", "--target-dir", "generated"),
    //Seq(ChiselGeneratorAnnotation(() => new MacUnit(Float(4, 4), Float(4, 4), Float(8, 24), Float(8, 24))),
    //Seq(ChiselGeneratorAnnotation(() => new hardfloat.AddRecFN(8,24)),
    //Seq(ChiselGeneratorAnnotation(() => new HardfloatAdd(Float(8, 24), Float(8, 24), Float(8, 24))),
    //Seq(ChiselGeneratorAnnotation(() => new HardfloatMul(Float(8, 24), Float(8, 24), Float(8, 24))),
    //Seq(ChiselGeneratorAnnotation(() => new hardfloat.AddAfter(8, 24)),
    Seq(ChiselGeneratorAnnotation(() => new hardfloat.BF16MAC(8, 8)),
    //Seq(ChiselGeneratorAnnotation(() => new hardfloat.BF16ToRawBF16(8, 8)),
    //Seq(ChiselGeneratorAnnotation(() => new hardfloat.BF16ToRawFloat),
      FirtoolOption("--disable-all-randomization"))
  )
}

// object GenerateVerilog extends App {
//   (new ChiselStage).execute(
//     Array("--target", "systemverilog", "--target-dir", "generated/all_fp32"),
//     Seq(ChiselGeneratorAnnotation(() => new MacUnit(Float(8, 24), Float(8, 24), Float(8, 24), Float(8, 24))),
//       FirtoolOption("--disable-all-randomization"))
//   )
// }