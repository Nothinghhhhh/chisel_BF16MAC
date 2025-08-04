
/*============================================================================

This Chisel source file is part of a pre-release version of the HardFloat IEEE
Floating-Point Arithmetic Package, by John R. Hauser (with some contributions
from Yunsup Lee and Andrew Waterman, mainly concerning testing).

Copyright 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 The Regents of the
University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice,
    this list of conditions, and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions, and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. Neither the name of the University nor the names of its contributors may
    be used to endorse or promote products derived from this software without
    specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS "AS IS", AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, ARE
DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

=============================================================================*/

package hardfloat

import chisel3._
import chisel3.util._
import consts._

class MulAddRecFN_interIo(expWidth: Int, sigWidth: Int) extends Bundle
{
//*** 编码这些情况是否可以使用更少的位数？:
    val isSigNaNAny     = Bool()  // 任何输入是否为信号NaN（sNaN）
    val isNaNAOrB       = Bool()  // 输入A或B是否为NaN
    val isInfMul        = Bool()  // 乘积是否为无穷大
    val isZeroMul       = Bool()  // 乘积是否为零
    val isInvalidMul    = Bool()  // 乘积是否为无效操作
    val signProd        = Bool()  // 乘积的符号
    val isNaNC          = Bool()  // 输入C是否为NaN
    val isInfC          = Bool()  // 输入C是否为无穷大
    val isZeroC         = Bool()  // 输入C是否为零
    val doSubMags       = Bool()  // 是否执行减法运算（乘积与C符号不同）
    val sExpSum         = SInt((expWidth + 2).W)  // 有符号指数和
    val CIsDominant     = Bool()  // C是否占主导地位（决定最终结果的主要因素）
    val CDom_CAlignDist = UInt(log2Ceil(sigWidth + 1).W)  // C占主导时的对齐距离
    val highAlignedSigC = UInt((sigWidth + 2).W)  // 对齐后的C的高位部分
    val bit0AlignedSigC = UInt(1.W)  // 对齐后的C的最低位

}
class BF16ToRawFloat extends RawModule {
    val io = IO(new Bundle {
        val in = Input(Bits(16.W))
        val isZeroFract = Input(Bool())
        val out = Output(new RawFloat(8, 8))
    })

    val sign = io.in(15)
    val expIn = io.in(14, 7)
    val fractIn = io.in(6, 0)

    val isZeroExpIn = (expIn === 0.U)
    val isZeroFractIn = io.isZeroFract

    val normDist = countLeadingZeros(fractIn)
    val subnormFract = (fractIn << normDist)(5, 0) << 1

    val adjustedExp = 
        Mux(isZeroExpIn,
            normDist ^ ((BigInt(1) << 9) - 1).U,
            expIn
        ) + (
            (BigInt(1) << 7).U |
            Mux(isZeroExpIn, 2.U, 1.U)
        )

    val isZero = isZeroExpIn && isZeroFractIn
    val isSpecial = adjustedExp(8, 7) === 3.U

    io.out.isNaN := isSpecial && !isZeroFractIn
    io.out.isInf := isSpecial && isZeroFractIn
    io.out.isZero := isZero         
    io.out.sign := sign
    io.out.sExp := adjustedExp(8, 0).zext

    io.out.sig := 0.U(1.W) ## !isZero ## Mux(isZeroExpIn, subnormFract, fractIn)
} 

class MulAddRecFNToRaw_preMul(expWidth: Int, sigWidth: Int) extends RawModule
{
    val io = IO(new Bundle {
        val a = Input(Bits((16).W))
        val b = Input(Bits((16).W))
        val c = Input(Bits((expWidth + sigWidth).W))
        val mulAddA = Output(UInt(8.W))  // 传递给乘法器的A的尾数
        val mulAddB = Output(UInt(8.W))  // 传递给乘法器的B的尾数
        val mulAddC = Output(UInt((sigWidth * 2).W))  // 对齐后的C，用于与乘积相加
        val toPostMul = Output(new MulAddRecFN_interIo(expWidth, sigWidth))  // 传递给后处理阶段的信息、
        
        //val preDebug = Output(UInt(16.W))
    })

    // 还没看懂
    val sigSumWidth = sigWidth + 16 + 3  // 信号和的总位宽

    // >>> 格式转换
    val rawA_module = Module(new BF16ToRawFloat())
    rawA_module.io.in := io.a
    rawA_module.io.isZeroFract := io.a(6, 0) === 0.U
    val rawA = rawA_module.io.out

    val rawB_module = Module(new BF16ToRawFloat())
    rawB_module.io.in := io.b
    rawB_module.io.isZeroFract := io.b(6, 0) === 0.U
    val rawB = rawB_module.io.out

    val rawC_module = Module(new BF16ToRawFloat())
    rawC_module.io.in := io.c
    rawC_module.io.isZeroFract := io.c(6, 0) === 0.U
    val rawC = rawC_module.io.out
    // <<< 格式转换
    
    // >>> 特殊情况判断
    val isZeroMul = rawA.isZero || rawB.isZero
    val isInvalidMul = (rawA.isNaN && rawB.isZero) || (rawA.isZero && rawB.isNaN) || (rawA.isInf && rawB.isZero) || (rawA.isZero && rawB.isInf)
    val isInfMul = rawA.isInf || rawB.isInf

    val signProd = rawA.sign ^ rawB.sign

    val doSubMags = signProd ^ rawC.sign 

    // <<< 特殊情况判断

    // 计算乘积的对齐指数（考虑偏置和额外的保护位）
    val sExpAlignedProd =
        rawA.sExp +& rawB.sExp + (-(BigInt(1)<<expWidth) + sigWidth + 3).S

    //------------------------------------------------------------------------
    // 计算C的对齐距离和对齐后的值
    //------------------------------------------------------------------------
    val sNatCAlignDist = sExpAlignedProd - rawC.sExp  // C相对于乘积的自然对齐距离，有符号
    val posNatCAlignDist = sNatCAlignDist(expWidth + 1, 0)  // 去掉符号位
    val isMinCAlign = isZeroMul || (sNatCAlignDist < 0.S)  // A*B为0或C的指数大，c不移位
    // C不为0,且c不需要移位或移位距离小于sigWidth，C主导
    val CIsDominant =
        ! rawC.isZero && (isMinCAlign || (posNatCAlignDist <= sigWidth.U))
    // 实际的对齐距离
    
    val CAlignDist =
        Mux(isMinCAlign,
            0.U,  // 不移位，或按照实际距离移位（不超过总位宽-1）
            Mux(posNatCAlignDist < (sigSumWidth - 1).U,
                posNatCAlignDist(log2Ceil(sigSumWidth) - 1, 0),  // 使用计算的距离
                (sigSumWidth - 1).U  // 限制最大移位距离
            )
        )
    // C的尾数处理：减法时取反，加法时保持原值，然后拼接填充位并右移对齐
    val mainAlignedSigC =
        (Mux(doSubMags, ~rawC.sig, rawC.sig) ## Fill(sigSumWidth - sigWidth + 2, doSubMags)).asSInt>>CAlignDist
    // 计算移位丢失的位的OR归约（用于粘滞位计算）
    val reduced4CExtra =
        (orReduceBy4(rawC.sig<<((sigSumWidth - sigWidth - 1) & 3)) &
             lowMask(
                 CAlignDist>>2,
//*** 不需要？:
//                 (sigSumWidth + 2)>>2,
                 (sigSumWidth - 1)>>2,
                 (sigSumWidth - sigWidth - 1)>>2
             )
        ).orR
    // 最终对齐的C（包含粘滞位）
    val alignedSigC =
        Cat(mainAlignedSigC>>3,
            Mux(doSubMags,
                mainAlignedSigC(2, 0).andR && ! reduced4CExtra,  // 减法时的粘滞位计算
                mainAlignedSigC(2, 0).orR  ||   reduced4CExtra   // 加法时的粘滞位计算
            )
        )

    io.mulAddA := rawA.sig
    io.mulAddB := rawB.sig
    io.mulAddC := alignedSigC(sigWidth * 2, 1)

    // 传递给后处理阶段的信息
    io.toPostMul.isSigNaNAny :=
        isSigNaNRawFloat(rawA) || isSigNaNRawFloat(rawB) || isSigNaNRawFloat(rawC)  // 检查是否有sNaN
    io.toPostMul.isNaNAOrB := rawA.isNaN || rawB.isNaN                              // A或B是否为NaN
    io.toPostMul.isInfMul  := isInfMul                                              // 乘积是否为无穷大
    io.toPostMul.isZeroMul := isZeroMul                                             // 乘积是否为零
    io.toPostMul.isInvalidMul := isInvalidMul                                       // 乘积是否为无效操作
    io.toPostMul.signProd  := signProd     // 乘积的符号
    io.toPostMul.isNaNC    := rawC.isNaN   // C是否为NaN
    io.toPostMul.isInfC    := rawC.isInf   // C是否为无穷大
    io.toPostMul.isZeroC   := rawC.isZero  // C是否为零
    io.toPostMul.doSubMags := doSubMags    // 是否执行减法运算
    io.toPostMul.sExpSum   :=
        Mux(CIsDominant, rawC.sExp, sExpAlignedProd - sigWidth.S)  // 结果的指数
    io.toPostMul.CIsDominant := CIsDominant       // C是否占主导
    io.toPostMul.CDom_CAlignDist := CAlignDist(log2Ceil(sigWidth + 1) - 1, 0)  // C主导时的对齐距离
    io.toPostMul.highAlignedSigC :=
        alignedSigC(sigSumWidth - 1, sigWidth * 2 + 1)  // 对齐C的高位部分
    io.toPostMul.bit0AlignedSigC := alignedSigC(0)      // 对齐C的最低位

    //io.preDebug := Cat(rawA.sig, rawB.sig, rawC.sig)
}

class MulAddRecFNToRaw_postMul(expWidth: Int, sigWidth: Int) extends RawModule
{
    override def desiredName = s"MulAddRecFNToRaw_postMul_e${expWidth}_s${sigWidth}"
    val io = IO(new Bundle {
        val fromPreMul = Input(new MulAddRecFN_interIo(expWidth, sigWidth))  // 来自前处理阶段的信息
        val mulAddResult = Input(UInt((sigWidth * 2 + 1).W))  // 乘法器的结果：A*B + alignedC
        val invalidExc  = Output(Bool())     // 无效操作异常标志
        val rawOut = Output(new RawFloat(expWidth, sigWidth + 2))  // 原始格式的输出结果

        //val postDebug = Output(UInt(20.W))
    })

    val sigSumWidth = sigWidth * 3 + 3  // 信号和的总位宽


    val opSignC = io.fromPreMul.signProd ^ io.fromPreMul.doSubMags  // C的操作符号：考虑减法时的符号
    // 组合最终的信号和：处理进位 + 乘法结果 + C的最低位
    val sigSum =
        Cat(Mux(io.mulAddResult(sigWidth * 2),
                io.fromPreMul.highAlignedSigC + 1.U,  // 有进位时加1
                io.fromPreMul.highAlignedSigC         // 无进位时保持原值
               ),
            io.mulAddResult(sigWidth * 2 - 1, 0),     // 乘法结果的主要部分
            io.fromPreMul.bit0AlignedSigC             // C的最低位
        )

    //------------------------------------------------------------------------
    // C占主导情况的处理
    // 当C的幅度比乘积A*B大得多时使用此路径
    //------------------------------------------------------------------------
    val CDom_sign = opSignC
    val CDom_sExp = io.fromPreMul.sExpSum - io.fromPreMul.doSubMags.zext
    val CDom_absSigSum =
        Mux(io.fromPreMul.doSubMags,
            ~sigSum(sigSumWidth - 1, sigWidth + 1),
            0.U(1.W) ##
//*** IF GAP IS REDUCED TO 1 BIT, MUST REDUCE THIS COMPONENT TO 1 BIT TOO:
                io.fromPreMul.highAlignedSigC(sigWidth + 1, sigWidth) ##
                sigSum(sigSumWidth - 3, sigWidth + 2)

        )
    val CDom_absSigSumExtra =
        Mux(io.fromPreMul.doSubMags,
            (~sigSum(sigWidth, 1)).orR,
            sigSum(sigWidth + 1, 1).orR
        )
    val CDom_mainSig =
        (CDom_absSigSum<<io.fromPreMul.CDom_CAlignDist)(
            sigWidth * 2 + 1, sigWidth - 3)
    val CDom_reduced4SigExtra =
        (orReduceBy4(CDom_absSigSum(sigWidth - 1, 0)<<(~sigWidth & 3)) &
             lowMask(io.fromPreMul.CDom_CAlignDist>>2, 0, sigWidth>>2)).orR
    val CDom_sig =
        Cat(CDom_mainSig>>3,
            CDom_mainSig(2, 0).orR || CDom_reduced4SigExtra ||
                CDom_absSigSumExtra
        )

    //------------------------------------------------------------------------
    // 非C占主导情况的处理
    // 当乘积A*B的幅度比C大时使用此路径
    //------------------------------------------------------------------------
    val notCDom_signSigSum = sigSum(sigWidth * 2 + 3)  // 信号和的符号位
    // 非C主导时的绝对值信号和
    val notCDom_absSigSum =
        Mux(notCDom_signSigSum,
            ~sigSum(sigWidth * 2 + 2, 0),  // 负数时取反
            sigSum(sigWidth * 2 + 2, 0) + io.fromPreMul.doSubMags
        )
    val notCDom_reduced2AbsSigSum = orReduceBy2(notCDom_absSigSum)  // 2位归约
    val notCDom_normDistReduced2 = countLeadingZeros(notCDom_reduced2AbsSigSum)  // 计算前导零
    val notCDom_nearNormDist = notCDom_normDistReduced2<<1  // 规范化距离
    val notCDom_sExp = io.fromPreMul.sExpSum - notCDom_nearNormDist.asUInt.zext  // 调整后的指数
    // 非C主导时的主信号（左移规范化）
    val notCDom_mainSig =
        (notCDom_absSigSum<<notCDom_nearNormDist)(
            sigWidth * 2 + 3, sigWidth - 1)
    // 非C主导时的归约额外位
    val notCDom_reduced4SigExtra =
        (orReduceBy2(
             notCDom_reduced2AbsSigSum(sigWidth>>1, 0)<<((sigWidth>>1) & 1)) &
             lowMask(notCDom_normDistReduced2>>1, 0, (sigWidth + 2)>>2)
        ).orR
    // 非C主导时的最终信号
    val notCDom_sig =
        Cat(notCDom_mainSig>>3,
            notCDom_mainSig(2, 0).orR || notCDom_reduced4SigExtra
        )
    // 检查是否完全抵消（结果为零）
    val notCDom_completeCancellation =
        (notCDom_sig(sigWidth + 2, sigWidth + 1) === 0.U)
    // 非C主导时的符号确定
    val notCDom_sign =
        Mux(notCDom_completeCancellation,
            false.B,  // 完全抵消时固定为正号（round_near_even模式）
            io.fromPreMul.signProd ^ notCDom_signSigSum  // 正常情况下的符号
        )

    // >>> 特殊情况处理和结果组装

    val notNaN_isInfOut = io.fromPreMul.isInfMul || io.fromPreMul.isInfC    // 输出Inf
    val notNaN_addZeros = io.fromPreMul.isZeroMul && io.fromPreMul.isZeroC  // 0+0

    io.invalidExc := io.fromPreMul.isSigNaNAny || io.fromPreMul.isInvalidMul || // 存在sNaN或0*inf
                    (! io.fromPreMul.isNaNAOrB &&
                        io.fromPreMul.isInfMul &&
                        io.fromPreMul.isInfC &&
                        io.fromPreMul.doSubMags)        // inf-inf
    
    io.rawOut.isNaN := io.fromPreMul.isNaNAOrB || io.fromPreMul.isNaNC  // 是否为NaN
    io.rawOut.isInf := notNaN_isInfOut  // 是否为无穷大
    io.rawOut.isZero := notNaN_addZeros || // 0+0
                        (! io.fromPreMul.CIsDominant && notCDom_completeCancellation)  // 相消
    
    // 确定符号
    io.rawOut.sign :=
        (io.fromPreMul.isInfMul && io.fromPreMul.signProd) ||           // 无穷乘积的符号
        (io.fromPreMul.isInfC && opSignC) ||                            // 无穷C的符号
        (notNaN_addZeros && io.fromPreMul.signProd && opSignC) ||       // 0+0的符号
        (! notNaN_isInfOut && ! notNaN_addZeros &&
             Mux(io.fromPreMul.CIsDominant, CDom_sign, notCDom_sign))   // 正常情况的符号，C占主导
    
    // 选择指数和尾数，C主导/非C主导
    io.rawOut.sExp := Mux(io.fromPreMul.CIsDominant, CDom_sExp, notCDom_sExp)
    io.rawOut.sig := Mux(io.fromPreMul.CIsDominant, CDom_sig, notCDom_sig)
    
    // <<< 特殊情况处理和结果组装

    //io.postDebug := CDom_sig
}

class MulAddRecFN(expWidth: Int, sigWidth: Int) extends RawModule
{
    override def desiredName = s"MulAddRecFN_e${expWidth}_s${sigWidth}"
    val io = IO(new Bundle {
        val a = Input(Bits((16).W))
        val b = Input(Bits((16).W))
        val c = Input(Bits((expWidth + sigWidth).W))
        val out = Output(Bits((expWidth + sigWidth).W))
    })

    val mulAddRecFNToRaw_preMul =
        Module(new MulAddRecFNToRaw_preMul(expWidth, sigWidth))  // 前处理模块
    mulAddRecFNToRaw_preMul.io.a  := io.a
    mulAddRecFNToRaw_preMul.io.b  := io.b
    mulAddRecFNToRaw_preMul.io.c  := io.c

    // 执行乘加运算：(A * B) + alignedC
    val mulAddResult = (mulAddRecFNToRaw_preMul.io.mulAddA * mulAddRecFNToRaw_preMul.io.mulAddB) +& mulAddRecFNToRaw_preMul.io.mulAddC

    val mulAddRecFNToRaw_postMul =
        Module(new MulAddRecFNToRaw_postMul(expWidth, sigWidth)) // 后处理模块
    mulAddRecFNToRaw_postMul.io.fromPreMul := mulAddRecFNToRaw_preMul.io.toPostMul  // 传递前处理的结果
    mulAddRecFNToRaw_postMul.io.mulAddResult := mulAddResult  // 传递乘加结果

    val roundRawFNToRecFN =
        Module(new RoundRawFNToRecFN(expWidth, sigWidth, 0))  // 舍入模块
    roundRawFNToRecFN.io.invalidExc   := mulAddRecFNToRaw_postMul.io.invalidExc
    roundRawFNToRecFN.io.in           := mulAddRecFNToRaw_postMul.io.rawOut

    val Bf16_out = fNFromRecFN(8, 8, roundRawFNToRecFN.io.out)
    io.out := Bf16_out
}

