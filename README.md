
示例项目：加法器

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

默认生成在 `generated` 文件夹。