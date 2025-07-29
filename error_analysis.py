#!/usr/bin/env python3
"""
BF16 MAC错误案例分析脚本
"""

def decode_bf16(hex_str):
    """解码BF16格式"""
    val = int(hex_str, 16)
    sign = (val >> 15) & 1
    exp = (val >> 7) & 0xFF
    frac = val & 0x7F
    
    # 判断数值类型
    if exp == 0:
        if frac == 0:
            return "zero", sign, exp, frac
        else:
            return "subnormal", sign, exp, frac
    elif exp == 0xFF:
        if frac == 0:
            return "infinity", sign, exp, frac
        else:
            if frac & 0x40:  # 检查第6位
                return "qnan", sign, exp, frac
            else:
                return "snan", sign, exp, frac
    else:
        return "normal", sign, exp, frac

def analyze_error_case(line):
    """分析单个错误案例"""
    parts = line.strip().split(',')
    a_hex = parts[0].split('=')[1]
    b_hex = parts[1].split('=')[1] 
    c_hex = parts[2].split('=')[1]
    dut_hex = parts[3].split('=')[1]
    rm_hex = parts[4].split('=')[1]
    
    a_type, a_sign, a_exp, a_frac = decode_bf16(a_hex)
    b_type, b_sign, b_exp, b_frac = decode_bf16(b_hex)
    c_type, c_sign, c_exp, c_frac = decode_bf16(c_hex)
    dut_type, dut_sign, dut_exp, dut_frac = decode_bf16(dut_hex)
    rm_type, rm_sign, rm_exp, rm_frac = decode_bf16(rm_hex)
    
    return {
        'a': (a_hex, a_type, a_sign, a_exp, a_frac),
        'b': (b_hex, b_type, b_sign, b_exp, b_frac),
        'c': (c_hex, c_type, c_sign, c_exp, c_frac),
        'dut': (dut_hex, dut_type, dut_sign, dut_exp, dut_frac),
        'rm': (rm_hex, rm_type, rm_sign, rm_exp, rm_frac)
    }

def classify_error(case):
    """对错误案例进行分类"""
    a_type = case['a'][1]
    b_type = case['b'][1]
    c_type = case['c'][1]
    dut_type = case['dut'][1]
    rm_type = case['rm'][1]
    
    classifications = []
    
    # 1. 检查是否涉及特殊值
    special_types = ['infinity', 'qnan', 'snan']
    if any(t in special_types for t in [a_type, b_type, c_type]):
        if 'infinity' in [a_type, b_type, c_type]:
            classifications.append("特殊值-无穷大")
        if any(t in ['qnan', 'snan'] for t in [a_type, b_type, c_type]):
            classifications.append("特殊值-NaN")
    
    # 2. 检查是否涉及非规格化数
    if any(t == 'subnormal' for t in [a_type, b_type, c_type]):
        classifications.append("非规格化数")
    
    # 3. 检查C是否占主导（rm == c）
    if case['rm'][0] == case['c'][0]:
        classifications.append("C占主导")
    
    # 4. 分析错误大小
    dut_val = int(case['dut'][0], 16)
    rm_val = int(case['rm'][0], 16)
    error_magnitude = abs(dut_val - rm_val)
    
    if error_magnitude == 1:
        classifications.append("1-ULP误差")
    elif error_magnitude <= 10:
        classifications.append("小误差")
    elif error_magnitude <= 100:
        classifications.append("中等误差") 
    else:
        classifications.append("大误差")
    
    # 5. 检查零值相关
    if any(t == 'zero' for t in [a_type, b_type, c_type]):
        classifications.append("零值相关")
        
    return classifications

# 读取CSV文件并分析
with open('mismatch_other.csv', 'r') as f:
    lines = f.readlines()

print("=" * 80)
print("BF16 MAC 错误案例分类分析")
print("=" * 80)

# 分类统计
categories = {
    "特殊值-无穷大": [],
    "特殊值-NaN": [],
    "非规格化数": [],
    "C占主导": [],
    "1-ULP误差": [],
    "小误差": [],
    "中等误差": [],
    "大误差": [],
    "零值相关": []
}

# 逐行分析
for i, line in enumerate(lines, 1):
    case = analyze_error_case(line)
    classifications = classify_error(case)
    
    print(f"\n案例 {i:2d}: {case['a'][0]} * {case['b'][0]} + {case['c'][0]} = {case['dut'][0]} (期望: {case['rm'][0]})")
    print(f"       类型: {case['a'][1]:<10} * {case['b'][1]:<10} + {case['c'][1]:<10}")
    print(f"       分类: {', '.join(classifications)}")
    
    # 统计分类
    for cat in classifications:
        if cat in categories:
            categories[cat].append(i)

print("\n" + "=" * 80)
print("分类统计汇总:")
print("=" * 80)

for category, cases in categories.items():
    if cases:
        print(f"{category:<15}: {len(cases):2d}个案例 - {cases}")

print(f"\n总计: {len(lines)} 个错误案例") 