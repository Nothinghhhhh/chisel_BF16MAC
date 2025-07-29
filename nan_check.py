#!/usr/bin/env python3
"""
验证RoundAnyRawFNToRecFN生成NaN时的输出
"""

# BF16参数
outExpWidth = 8
outSigWidth = 7

print("=== RoundAnyRawFNToRecFN NaN输出分析 ===")
print(f"outExpWidth = {outExpWidth}, outSigWidth = {outSigWidth}")
print()

# 1. 计算常量
outNaNExp = 7 << (outExpWidth - 2)  # 7 << 6
outInfExp = 6 << (outExpWidth - 2)  # 6 << 6

print(f"outNaNExp = 7 << {outExpWidth - 2} = {outNaNExp} = 0x{outNaNExp:x}")
print(f"outInfExp = 6 << {outExpWidth - 2} = {outInfExp} = 0x{outInfExp:x}")
print()

# 2. 当isNaNOut=true时的输出
signOut = 0  # 1位
expOut = outNaNExp  # 9位宽度
fractOut = 1 << (outSigWidth - 2)  # 1 << 5

print("当isNaNOut=true时:")
print(f"signOut = {signOut}")
print(f"expOut = {expOut} = 0x{expOut:x} (9位宽度)")
print(f"fractOut = 1 << {outSigWidth - 2} = {fractOut} = 0x{fractOut:x} (7位宽度)")
print()

# 3. RecFN格式输出 (17位)
recfn_output = (signOut << 16) | (expOut << 7) | fractOut
print(f"RecFN输出 (17位): 0x{recfn_output:05x}")
print(f"二进制: {recfn_output:017b}")
print(f"格式: {recfn_output >> 16:01b}_{(recfn_output >> 7) & 0x1FF:09b}_{recfn_output & 0x7F:07b}")
print()

# 4. 分析期望的BF16 NaN: 0x7fc0
expected_nan = 0x7fc0
exp_sign = (expected_nan >> 15) & 1
exp_exp = (expected_nan >> 7) & 0xFF
exp_frac = expected_nan & 0x7F

print(f"期望的BF16 NaN: 0x{expected_nan:04x}")
print(f"二进制: {expected_nan:016b}")
print(f"格式: {exp_sign}_{exp_exp:08b}_{exp_frac:07b}")
print(f"符号: {exp_sign}, 指数: {exp_exp} (0x{exp_exp:02x}), 尾数: {exp_frac} (0x{exp_frac:02x})")
print()

# 5. 分析fNFromRecFN的转换
print("=== fNFromRecFN转换分析 ===")
print("当rawIn.isNaN = true时:")
print(f"expOut = Fill({outExpWidth}, true) = 0x{'FF'[:outExpWidth//4*2] if outExpWidth == 8 else 'FF'}")
print(f"fractOut = rawIn.sig({outSigWidth-2}, 0) = rawIn.sig(5, 0)")
print()

# 6. 检查问题
print("=== 问题分析 ===")
print(f"RoundAnyRawFNToRecFN的fractOut = 0x{fractOut:02x} = {fractOut:07b}")
print(f"期望的BF16 fractOut = 0x{exp_frac:02x} = {exp_frac:07b}")
print()

if fractOut == exp_frac:
    print("✅ fractOut匹配!")
else:
    print(f"❌ fractOut不匹配!")
    print(f"   实际: 第{fractOut.bit_length()-1}位为1")
    print(f"   期望: 第{exp_frac.bit_length()-1}位为1")
    print()
    print("可能的问题:")
    print("1. sigWidth索引理解错误")
    print("2. NaN尾数的MSB位置定义不同")
    print("3. RecFN到FN转换中的位偏移")

print()
print("=== IEEE 754 BF16 NaN标准 ===")
print("NaN格式: S EEEEEEEE FFFFFFF")
print("qNaN: 尾数MSB=1 (第6位)")
print("sNaN: 尾数MSB=0, 但尾数≠0")
print()
print(f"标准qNaN尾数: 1000000 = 0x{0x40:02x}")
print(f"当前生成尾数: {fractOut:07b} = 0x{fractOut:02x}") 