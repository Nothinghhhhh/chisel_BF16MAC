import csv
with open('BF16/mismatch_cases.csv') as f1, \
     open('BF16/mismatch_signonly.csv','w',newline='') as f2, \
     open('BF16/mismatch_other.csv','w',newline='') as f3:
    r = csv.reader(f1)
    w2 = csv.writer(f2)
    w3 = csv.writer(f3)
    for row in r:
        dut = row[3].split('=')[1].lower()
        rm = row[4].split('=')[1].lower()
        if len(dut) == len(rm) and len(dut) > 0:
            try:
                d = int(dut, 16)
                m = int(rm, 16)
                sign_mask = 1 << (len(dut)*4 - 1)
                if d == (m ^ sign_mask):
                    w2.writerow(row)
                else:
                    w3.writerow(row)
            except Exception:
                w3.writerow(row)
        else:
            w3.writerow(row)
