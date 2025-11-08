(function () {
    const ctx = document.getElementById('satisfactionChart');
    if (!ctx || !window.__satisfaction) return;

    const d = window.__satisfaction;
    const values = [d.satisfied || 0, d.neutral || 0, d.unsatisfied || 0];

    // Colors to match your screenshot
    const colors = ['#5B6EE1', '#F5A623', '#E74C3C'];

    // Build the pie chart
    new Chart(ctx, {
        type: 'pie',
        data: {
            labels: ['Satisfied', 'Neutral', 'Unsatisfied'],
            datasets: [{
                data: values,
                backgroundColor: colors,
                borderColor: '#ffffff',
                borderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }, // we render a custom legend above
                tooltip: {
                    callbacks: {
                        label: (ctx) => {
                            const total = values.reduce((a, b) => a + b, 0) || 1;
                            const val = ctx.parsed || 0;
                            const pct = Math.round((val / total) * 100);
                            return ` ${ctx.label}: ${val} (${pct}%)`;
                        }
                    }
                }
            }
        }
    });
})();
