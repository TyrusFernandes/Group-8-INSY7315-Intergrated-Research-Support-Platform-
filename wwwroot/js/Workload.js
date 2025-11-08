(function () {
    const ctx = document.getElementById('workloadChart').getContext('2d');
    const searchBox = document.getElementById('searchBox');
    const deptFilter = document.getElementById('deptFilter');
    const exportBtn = document.getElementById('exportCsvBtn');

    const source = (typeof consultants !== 'undefined' && Array.isArray(consultants)) ? consultants : [];

    function filterData() {
        const q = (searchBox.value || '').toLowerCase();
        const dept = deptFilter.value;

        return source
            .filter(c => dept === 'all' ? true : c.department === dept)
            .filter(c => c.name.toLowerCase().includes(q))
            .sort((a, b) => b.activeProjects - a.activeProjects);
    }

    function toChartSeries(list) {
        return {
            labels: list.map(c => c.name),
            data: list.map(c => c.activeProjects)
        };
    }

    function makeCsv(list) {
        const header = ['Name', 'Department', 'Region', 'ActiveProjects'];
        const rows = list.map(c => [c.name, c.department, c.region, c.activeProjects]);
        return [header, ...rows].map(r => r.map(v => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\n');
    }

    // init chart
    const initial = toChartSeries(filterData());
    let chart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: initial.labels,
            datasets: [{
                label: 'Active Projects',
                data: initial.data,
                borderWidth: 1,
                backgroundColor: '#4F83CC',  // Blue color
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: { y: { beginAtZero: true, ticks: { precision: 0 } } },
            plugins: { legend: { display: false } }
        }
    });

    function refresh() {
        const list = filterData();
        const series = toChartSeries(list);
        chart.data.labels = series.labels;
        chart.data.datasets[0].data = series.data;
        chart.update();
    }

    searchBox?.addEventListener('input', refresh);
    deptFilter?.addEventListener('change', refresh);

    exportBtn?.addEventListener('click', () => {
        const list = filterData();
        const csv = makeCsv(list);
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = 'consultant_workload.csv';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
    });
})();
