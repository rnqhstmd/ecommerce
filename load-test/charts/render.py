# Before/After 비교 차트 생성기.
# 사용(Docker): docker run --rm -v "/d/SQ/ecommerce-loadtest/load-test:/lt" -w /lt python:3-slim \
#                 sh -c "pip install -q matplotlib && python charts/render.py"
import json, os
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

R = 'k6/results'
OUT = 'charts/out'
os.makedirs(OUT, exist_ok=True)

def v(f, name, stat):
    return json.load(open(os.path.join(R, f)))['metrics'][name]['values'][stat]

def hk(f):
    r = json.load(open(os.path.join(R, f)))['data']['result']
    return float(r[0]['value'][1]) if r else 0.0

def bar_chart(title, fname, latency_metric, base, opt, base_hk, opt_hk):
    metrics = [
        ('TPS (req/s)', v(base, 'http_reqs', 'rate'), v(opt, 'http_reqs', 'rate'), 'higher better'),
        ('median (ms)', v(base, latency_metric, 'med'), v(opt, latency_metric, 'med'), 'lower better'),
        ('p95 (ms)', v(base, latency_metric, 'p(95)'), v(opt, latency_metric, 'p(95)'), 'lower better'),
        ('p99 (ms)', v(base, latency_metric, 'p(99)'), v(opt, latency_metric, 'p(99)'), 'lower better'),
    ]
    fig, axes = plt.subplots(1, len(metrics), figsize=(15, 4))
    for ax, (mt, bv, ov, note) in zip(axes, metrics):
        ax.bar(['Before\n(Lock)', 'After\n(Redis)'], [bv, ov], color=['#d62728', '#2ca02c'])
        ax.set_title('%s\n(%s)' % (mt, note), fontsize=10)
        for x, val in enumerate([bv, ov]):
            ax.text(x, val, '%.0f' % val, ha='center', va='bottom', fontsize=10)
        # 개선율 표기
        if 'higher' in note and bv > 0:
            ax.set_xlabel('x%.1f' % (ov / bv), fontsize=11)
        elif 'lower' in note and bv > 0:
            ax.set_xlabel('-%.0f%%' % ((1 - ov / bv) * 100), fontsize=11)
    fig.suptitle(title, fontsize=13)
    fig.tight_layout()
    fig.savefig(os.path.join(OUT, fname), dpi=130)
    print('saved', fname)

bar_chart('Order Hot-Product (50 VU): Pessimistic Lock vs Redis Pre-Decrement',
          '02-before-after.png', 'order_latency',
          '02-baseline.json', '02-optimized.json',
          '02-baseline-hikari.json', '02-optimized-hikari.json')

bar_chart('Order Stress (ramp to 120 VU): Pessimistic Lock vs Redis Pre-Decrement',
          '03-before-after.png', 'order_stress_latency',
          '03-baseline.json', '03-optimized.json',
          '03-baseline-hikari.json', '03-optimized-hikari.json')
