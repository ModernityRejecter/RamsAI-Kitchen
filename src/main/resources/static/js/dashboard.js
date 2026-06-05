// dashboard.js - Task 20 Reporting Implementation (Real API Sync)

function runDashboardSystem() {
    let popularityChart = null;
    let ratingsChart = null;
    let originalData = [];

    const searchInput = document.getElementById("dashboardSearch");
    const chartsSection = document.getElementById("dashboardChartsSection");
    const tableSection = document.getElementById("dashboardTableSection");
    const tableBody = document.getElementById("dashboardTableBody");
    const performanceCheck = document.getElementById("dashboardPerformanceCheck");

    if (!searchInput) return;

    // UX Tab switching
    document.getElementById("btn-chart-view").addEventListener("click", () => {
        document.getElementById("btn-chart-view").classList.add("active");
        document.getElementById("btn-table-view").classList.remove("active");
        chartsSection.style.display = "grid";
        tableSection.style.display = "none";
    });

    document.getElementById("btn-table-view").addEventListener("click", () => {
        document.getElementById("btn-table-view").classList.add("active");
        document.getElementById("btn-chart-view").classList.remove("active");
        chartsSection.style.display = "none";
        tableSection.style.display = "block";
    });

    // Filtro di ricerca in tempo reale
    searchInput.addEventListener("input", (e) => {
        const searchTerm = e.target.value.toLowerCase();
        const filtered = originalData.filter(item => 
            item.name.toLowerCase().includes(searchTerm) || 
            item.category.toLowerCase().includes(searchTerm)
        );
        renderTable(filtered);
        updateCharts(filtered);
    });

    async function fetchDashboardData() {
        const startTime = performance.now();
        performanceCheck.innerHTML = `<span style="color: #6366f1;"><i class="fas fa-spinner fa-spin"></i> Synchronizing with live analytics server...</span>`;
        
        try {
            const popularityRes = await authenticatedFetch('/api/v1/manager/dashboard/popularity');
            const ratingsRes = await authenticatedFetch('/api/v1/manager/dashboard/ratings');

            // Se uno dei due risponde male, stampiamo l'errore esatto per capire se è un problema di permessi (403) o di indirizzo errato (404)
            if (!popularityRes.ok || !ratingsRes.ok) {
                performanceCheck.innerHTML = `<span class="danger"><i class="fas fa-exclamation-circle"></i> Server Error! Popularity Status: ${popularityRes.status} | Ratings Status: ${ratingsRes.status}</span>`;
                return;
            }

            const popularityJson = await popularityRes.json();
            const ratingsJson = await ratingsRes.json();

            const popularityData = popularityJson.data || [];
            const ratingsData = ratingsJson.data || [];

            if (popularityData.length === 0) {
                performanceCheck.innerHTML = `<span><i class="fas fa-check-circle" style="color:#10b981;"></i> Connected to server. API is live but database contains 0 orders yet.</span>`;
                return;
            }

            originalData = popularityData.map(p => {
                const r = ratingsData.find(rating => rating.id === p.id || rating.name === p.name) || { avgRating: 0 };
                return {
                    id: p.id,
                    name: p.name || "Unnamed Product",
                    category: p.category || 'General',
                    salesCount: p.salesCount || 0,
                    avgRating: r.avgRating || 0
                };
            });

            originalData.sort((a, b) => b.salesCount - a.salesCount);

            renderTable(originalData);
            initCharts(originalData);

            const endTime = performance.now();
            const duration = ((endTime - startTime) / 1000).toFixed(4);
            performanceCheck.innerText = `Real-time dashboard generated in ${duration} seconds.`;

        } catch (error) {
            console.error("Dashboard Engine Crash:", error);
            performanceCheck.innerHTML = `<span class="danger">Connection failed. Check your local network or Docker status.</span>`;
        }
    }

    function renderTable(data) {
        tableBody.innerHTML = "";
        data.forEach(item => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td><strong>${item.name}</strong></td>
                <td><span class="status-badge approved" style="background:#f3f4f6; color:#111827;">${item.category}</span></td>
                <td>${item.salesCount} ord.</td>
                <td><i class="fas fa-star" style="color:#eab308;"></i> ${item.avgRating.toFixed(1)}</td>
            `;
            tableBody.appendChild(row);
        });
    }

    function initCharts(data) {
        const ctxPop = document.getElementById('popularityChart').getContext('2d');
        popularityChart = new Chart(ctxPop, {
            type: 'bar',
            data: {
                labels: data.map(i => i.name),
                datasets: [{ label: 'Orders (Popularity)', data: data.map(i => i.salesCount), backgroundColor: 'rgba(99, 102, 241, 0.75)', borderColor: '#4338ca', borderWidth: 1 }]
            },
            options: { responsive: true, maintainAspectRatio: false }
        });

        const ctxRate = document.getElementById('ratingsChart').getContext('2d');
        ratingsChart = new Chart(ctxRate, {
            type: 'line',
            data: {
                labels: data.map(i => i.name),
                datasets: [{ label: 'Avg Rating (Stars)', data: data.map(i => i.avgRating), backgroundColor: 'rgba(234, 179, 8, 0.15)', borderColor: '#eab308', borderWidth: 2, fill: true }]
            },
            options: { responsive: true, maintainAspectRatio: false, scales: { y: { min: 0, max: 5 } } }
        });
    }

    function updateCharts(filteredData) {
        if (!popularityChart || !ratingsChart) return;
        const labels = filteredData.map(i => i.name);
        
        popularityChart.data.labels = labels;
        popularityChart.data.datasets[0].data = filteredData.map(i => i.salesCount);
        popularityChart.update();

        ratingsChart.data.labels = labels;
        ratingsChart.data.datasets[0].data = filteredData.map(i => i.avgRating);
        ratingsChart.update();
    }

    fetchDashboardData();
}

// Avvio ad aggancio continuo sul token JWT valido
const authCheckInterval = setInterval(() => {
    if (localStorage.getItem('token') || sessionStorage.getItem('token')) {
        clearInterval(authCheckInterval);
        runDashboardSystem();
    }
}, 100);