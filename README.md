### BF16MAC
以UCB开源运算器hardfloat为基础，为BF16格式运算定制修改，期望实现
1. 支持混合精度计算
2. 保证混合精度计算时运算数据位宽无冗余，用尽可能小位宽的运算器

### 当前问题
1. 1ULP精度偏差，经过验证，原hardfloat MulAdd模块也存在该问题。
2. a\*b绝对值小于BF16表示范围，但由于中间运算过程额外位宽，而未变成0,影响运算结果的符号，与pytorch模型产生0的符号不一致。
3. 混合精度的进一步优化可能需要比较彻底的重写

### 环境使用的版本：
```
sbt         1.8.2
scala       2.13.12
chisel      6.5.0
chiselTest  6.0.0
scalatest   3.2.+
```

### 运行测试：
提供了一个批量测试的测试文件，从result目录下的mismatch_cases.csv中读出输入，将测试结果写入csv_test_results.csv

```
sbt test
sbt "testOnly macunit.BF16MacTester"
```

已修改 `EphemeralSimulator`，运行时在 `test_run_dir` 文件夹中能生成仿真相关文件，包括生成的Verilog代码、Verilator编译文件、仿真波形。


### 单独生成Verilog文件：

```
sbt "runMain demo.GenerateVerilog"
```

默认生成在 `generated` 文件夹