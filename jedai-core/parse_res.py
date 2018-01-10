import os
import time
import sys
import matplotlib.pyplot as plt

if len(sys.argv) == 1:
    print("Give results file")
    exit(1)

resfile =  sys.argv[1].strip()
res = {}
with open(resfile) as f:
    for i,line in enumerate(f):
        line = line.strip()
        if not line:
            continue
        if not line.startswith("RES: "):
            print("Ignoring line:[%s]" % line)
            continue
        line = line[5:]
        elements = [ e for e in line.split() if e != "|" ]
        elements = [ float(e) for e in line.split() if e != "|" ]
        if not elements or len(elements) == 1:
            print("No results parsed from config line: [%s] " % line)
            continue
        thresh, values = elements[0], elements[1:]
        res[thresh] = values

if not res:
    print("Empty res file")
    exit(1)

do_dirty = len(next(iter(res.values()))) == 2
threshes = sorted([t for t in res])
index = threshes
barwidth=0.01

print("Index is: " , index)
print("precs:",[res[x][0] for x in threshes])
print("Res is:", res)

# non-dirty: print wrt refs and sums by themselves, as well
if not do_dirty:

    fig, axes  = plt.subplots(3,  sharey=True)
    (ax1, ax2, ax3) = axes
    # total prec, rec
    plt.xticks([i + barwidth/2 for i in index] , threshes)
    [ax.grid() for ax in axes]
    [ax.set_xlabel("sim. threshold") for ax in axes]

    ax1.set_xlabel("Similarity threshold")
    ax1.set_ylim([0,1.1])
    xend=threshes[-1] + 0.1
    ax1.set_xlim([0,xend])
    ax1.set_title("Total")

    ax1.bar([i  for i in index], [ res[t][0] for t in threshes],
            width=barwidth,
            color = "r", label="precision")
    ax1.bar([i+barwidth  for i in index], [ res[t][1] for t in threshes],
            width=barwidth,
            color = "b", label="recall")
    ax1.plot([0,xend],[1,1],"k--")

    # by refs
    ax2.set_title("Reference summaries")
    ax2.bar([i  for i in index], [ res[t][2] for t in threshes],
            width=barwidth,
            color = "r", label="precision")
    ax2.bar([i+barwidth  for i in index], [ res[t][3] for t in threshes],
            width=barwidth,
            color = "b", label="recall")
    ax2.plot([0,xend],[1,1],"k--")


    # by sums
    ax3.set_title("non-Reference summaries")
    ax3.bar([i  for i in index], [ res[t][4] for t in threshes],
            width=barwidth,
            color = "r", label="precision")
    ax3.bar([i+barwidth  for i in index], [ res[t][5] for t in threshes],
            width=barwidth,
            color = "b", label="recall")
    ax3.plot([0,xend],[1,1],"k--")
    ax1.legend(loc='upper right')

else:

    # total prec, rec
    threshes = sorted([t for t in res])
    barwidth=0.01
    index = threshes
    fig = plt.figure(figsize=(12.0, 5.0))
    ax = plt.gca()
    plt.xticks([i + barwidth/2 for i in index] , threshes)
    ax.grid()
    ax.set_xlabel("sim. threshold")

    ax.set_xlabel("Similarity threshold")
    ax.set_ylim([0,1.1])
    xend=threshes[-1] + 0.1
    ax.set_xlim([0,xend])
    ax.set_title("Total")

    ax.bar([i  for i in index], [ res[t][0] for t in threshes],
            width=barwidth,
            color = "r", label="precision")
    ax.bar([i+barwidth  for i in index], [ res[t][1] for t in threshes],
            width=barwidth,
            color = "b", label="recall")
    ax.plot([0,xend],[1,1],"k--")
    ax.legend(loc='upper right')

plt.subplots_adjust(hspace=0.5)
#plt.show()
#filename = "barcharts/barchart_" + time.strftime("%d%m%y_%H%M%S") 
filename = "barcharts/image"
os.makedirs("barcharts", exist_ok = True)

plt.savefig(filename, dpi = fig.dpi)

