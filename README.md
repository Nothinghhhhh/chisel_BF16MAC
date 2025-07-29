### BF16MAC
以UCB开源运算器hardfloat为基础，为BF16格式运算定制修改，期望实现
1. 支持混合精度计算
2. 保证混合精度计算时运算数据位宽无冗余，用尽可能小位宽的运算器

### 环境使用的版本：
```
sbt         1.8.2
scala       2.13.12
chisel      6.5.0
chiselTest  6.0.0
scalatest   3.2.+
```

### 运行测试：

```
sbt test
sbt "testOnly demo.MyAdderTester"
```

已修改 `EphemeralSimulator`，运行时在 `test_run_dir` 文件夹中能生成仿真相关文件，包括生成的Verilog代码、Verilator编译文件、仿真波形。


### 单独生成Verilog文件：

```
sbt "runMain demo.GenerateVerilog"
```

默认生成在 `generated` 文件夹