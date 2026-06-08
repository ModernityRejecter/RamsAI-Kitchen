// User Story 10 — Sales Reporting: visual dashboard + AI analyst.
(function () {
    const PALETTE = ['#6366f1', '#22c55e', '#f59e0b', '#ef4444', '#06b6d4', '#a855f7', '#ec4899', '#14b8a6', '#f97316', '#3b82f6'];
    const charts = [];

    document.addEventListener('DOMContentLoaded', () => {
        if (typeof authenticatedFetch !== 'function') return; // not logged in / auth.js missing
        wireAskForm();
        renderSuggestions();
        loadReport();
    });

    async function loadReport() {
        const scroller = document.getElementById('chartsScroller');
        try {
            const res = await authenticatedFetch('/api/v1/manager/analytics/report');
            const json = await res.json();
            const report = json.data;
            renderKpis(report);
            renderDefaultCharts(report);
        } catch (err) {
            console.error('Failed to load sales report:', err);
            scroller.innerHTML = '<div class="analytics-empty">Could not load the sales report.</div>';
        }
    }

    function renderKpis(report) {
        const el = document.getElementById('analyticsTotals');
        const kpis = [
            { label: 'Units Sold', value: (report.totalUnitsSold || 0).toLocaleString() },
            { label: 'Revenue', value: money(report.totalRevenue, 0) },
            { label: 'Avg Rating', value: (report.overallAverageRating || 0).toFixed(1) + '★' },
            { label: 'Categories', value: (report.categories ? report.categories.length : 0).toLocaleString() }
        ];
        el.innerHTML = kpis.map(k =>
            `<div class="kpi"><div class="kpi-value">${k.value}</div><div class="kpi-label">${k.label}</div></div>`
        ).join('');
    }

    function renderDefaultCharts(report) {
        const scroller = document.getElementById('chartsScroller');
        scroller.innerHTML = '';
        charts.splice(0).forEach(c => c.destroy());

        const top = report.topProducts || [];
        const cats = report.categories || [];

        if (top.length === 0) {
            scroller.innerHTML = '<div class="analytics-empty">No sales recorded yet. Once customers place orders, charts will appear here.</div>';
            return;
        }

        addChartCard({
            type: 'bar',
            title: 'Top products by units sold',
            labels: top.map(p => p.productName),
            datasets: [{ label: 'Units sold', data: top.map(p => p.quantitySold) }]
        });

        addChartCard({
            type: 'doughnut',
            title: 'Revenue share by category',
            labels: cats.map(c => c.categoryName),
            datasets: [{ label: 'Revenue', data: cats.map(c => Number(c.totalRevenue) || 0) }]
        });

        addChartCard({
            type: 'bar',
            title: 'Units sold by category',
            labels: cats.map(c => c.categoryName),
            datasets: [{ label: 'Units sold', data: cats.map(c => c.totalQuantitySold) }]
        });

        addChartCard({
            type: 'bar',
            title: 'Average rating — top products',
            labels: top.map(p => p.productName),
            datasets: [{ label: 'Avg rating', data: top.map(p => p.averageRating) }]
        }, { max: 5 });

        addTableCard(report);
    }

    function addTableCard(report) {
        const scroller = document.getElementById('chartsScroller');
        const card = document.createElement('div');
        card.className = 'chart-card table-card';

        let rows = '';
        (report.categories || []).forEach(cat => {
            rows += `<tr><td colspan="4" style="background:#f1f5f9;font-weight:700;">${escapeHtml(cat.categoryName)} — ${cat.totalQuantitySold} units · ${money(cat.totalRevenue, 2)}</td></tr>`;
            (cat.products || []).forEach(p => {
                rows += `<tr>
                    <td>${escapeHtml(p.productName)}</td>
                    <td>${p.quantitySold}</td>
                    <td>${money(p.revenue, 2)}</td>
                    <td>${p.averageRating ? p.averageRating.toFixed(1) + '★' : '—'}</td>
                </tr>`;
            });
        });

        card.innerHTML = `
            <h4><i class="fas fa-table" style="color:#4f46e5;"></i> Full report by category</h4>
            <div class="table-wrap">
                <table>
                    <thead><tr><th>Product</th><th>Units</th><th>Revenue</th><th>Rating</th></tr></thead>
                    <tbody>${rows || '<tr><td colspan="4">No data</td></tr>'}</tbody>
                </table>
            </div>`;
        scroller.appendChild(card);
    }

    function addChartCard(spec, opts) {
        const scroller = document.getElementById('chartsScroller');
        const ai = !!(opts && opts.ai);
        const card = document.createElement('div');
        card.className = 'chart-card' + (ai ? ' ai-generated' : '');

        const title = document.createElement('h4');
        title.innerHTML = (ai ? '<span class="ai-tag">AI</span> ' : '') +
            `<i class="fas fa-chart-simple"></i> ${escapeHtml(spec.title || 'Chart')}`;

        const wrap = document.createElement('div');
        wrap.className = 'chart-canvas-wrap';
        const canvas = document.createElement('canvas');
        wrap.appendChild(canvas);

        card.appendChild(title);
        card.appendChild(wrap);

        if (ai) scroller.insertBefore(card, scroller.firstChild);
        else scroller.appendChild(card);

        try {
            charts.push(new Chart(canvas.getContext('2d'), buildChartConfig(spec, opts)));
        } catch (err) {
            console.error('Chart render failed:', err, spec);
            wrap.innerHTML = '<div class="analytics-empty">Could not render this chart.</div>';
        }
        if (ai) scroller.scrollTo({ left: 0, behavior: 'smooth' });
        return card;
    }

    function buildChartConfig(spec, opts) {
        const type = ['bar', 'line', 'pie', 'doughnut', 'radar', 'polarArea'].includes(spec.type) ? spec.type : 'bar';
        const circular = ['pie', 'doughnut', 'polarArea'].includes(type);
        const labels = spec.labels || [];

        const datasets = (spec.datasets || []).map((ds, i) => {
            const data = ds.data || [];
            if (circular) {
                return {
                    label: ds.label || '',
                    data,
                    backgroundColor: data.map((_, j) => PALETTE[j % PALETTE.length]),
                    borderWidth: 1
                };
            }
            const color = PALETTE[i % PALETTE.length];
            return {
                label: ds.label || '',
                data,
                backgroundColor: type === 'line' ? color + '33' : color + 'cc',
                borderColor: color,
                borderWidth: 2,
                fill: type === 'line' || type === 'radar',
                tension: 0.3,
                pointRadius: type === 'line' ? 3 : undefined
            };
        });

        const singleSeries = datasets.length <= 1;
        const options = {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: circular || !singleSeries, position: 'bottom' } }
        };
        if (!circular && type !== 'radar') {
            options.scales = { y: { beginAtZero: true } };
            if (opts && typeof opts.max === 'number') options.scales.y.max = opts.max;
        }

        return { type, data: { labels, datasets }, options };
    }

    function wireAskForm() {
        const form = document.getElementById('analyticsAskForm');
        const input = document.getElementById('analyticsQuestion');
        const btn = document.getElementById('analyticsAskBtn');
        const answerEl = document.getElementById('analyticsAnswer');

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const question = input.value.trim();
            if (!question) return;

            btn.disabled = true;
            const original = btn.innerHTML;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Thinking…';
            answerEl.classList.remove('hidden');
            answerEl.innerHTML = '<div class="ai-label"><i class="fas fa-robot"></i> AI Analyst</div><div>Analyzing your sales data…</div>';

            try {
                const res = await authenticatedFetch('/api/v1/manager/analytics/ask', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ question })
                });
                const json = await res.json();
                const data = json.data || {};
                const text = escapeHtml(data.answer || 'No answer.').replace(/\n/g, '<br>').replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
                answerEl.innerHTML = `<div class="ai-label"><i class="fas fa-robot"></i> AI Analyst</div><div>${text}</div>`;
                if (data.chart && data.chart.labels) {
                    addChartCard(data.chart, { ai: true });
                }
                input.value = '';
            } catch (err) {
                console.error('Analytics ask failed:', err);
                answerEl.innerHTML = '<div class="ai-label"><i class="fas fa-robot"></i> AI Analyst</div><div>Sorry, something went wrong reaching the analyst.</div>';
            } finally {
                btn.disabled = false;
                btn.innerHTML = original;
            }
        });
    }

    function renderSuggestions() {
        const el = document.getElementById('analyticsSuggestions');
        const input = document.getElementById('analyticsQuestion');
        const suggestions = [
            'Which category brings the most revenue?',
            'What are my 3 worst-selling products?',
            'Show a pie chart of revenue by category',
            'Plot a bar chart of average rating per category'
        ];
        suggestions.forEach(s => {
            const chip = document.createElement('button');
            chip.type = 'button';
            chip.className = 'suggestion-chip';
            chip.textContent = s;
            chip.addEventListener('click', () => {
                input.value = s;
                input.focus();
            });
            el.appendChild(chip);
        });
    }

    function money(value, decimals) {
        const n = Number(value) || 0;
        return '$' + n.toLocaleString(undefined, { minimumFractionDigits: decimals, maximumFractionDigits: decimals });
    }

    function escapeHtml(str) {
        return String(str == null ? '' : str)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }
})();
