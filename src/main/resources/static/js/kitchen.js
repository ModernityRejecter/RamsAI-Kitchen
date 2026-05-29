// Kitchen Display System — live board of active orders (New / Cooking / Ready).
const KDS_COLUMNS = ['RECEIVED', 'COOKING', 'READY'];
const WARN_MINUTES = 10;
const DANGER_MINUTES = 20;
const FALLBACK_POLL_MS = 20000;

const orders = new Map();      // orderId -> OrderResponse
const knownIds = new Set();    // ids we've already seen (so we only alert on truly new orders)
let flashIds = new Set();      // ids to flash on next render
let connected = false;
let soundEnabled = true;
let audioCtx = null;

document.addEventListener('DOMContentLoaded', () => {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const role = localStorage.getItem('role') || sessionStorage.getItem('role');
    if (!token) { window.location.href = 'login.html'; return; }
    if (role !== 'CHEF' && role !== 'MANAGER') { window.location.href = 'index.html'; return; }

    document.getElementById('refreshBtn').addEventListener('click', loadBoard);
    document.getElementById('soundToggle').addEventListener('click', toggleSound);

    loadBoard();
    connectRealtime();

    setInterval(updateElapsedLabels, 30000);
    setInterval(() => { if (!connected) loadBoard(); }, FALLBACK_POLL_MS);
});

async function loadBoard() {
    try {
        const res = await authenticatedFetch('/api/v1/kitchen/board');
        if (!res.ok) throw new Error('board fetch failed');
        const json = await res.json();
        const list = Array.isArray(json.data) ? json.data : [];
        orders.clear();
        list.forEach(o => { orders.set(o.id, o); knownIds.add(o.id); });
        renderAll();
    } catch (err) {
        console.error('Failed to load kitchen board:', err);
    }
}

function connectRealtime() {
    createRealtimeClient({
        onConnect: (client) => {
            setConn(true);
            client.subscribe('/topic/kitchen', (msg) => {
                try { handleOrderEvent(JSON.parse(msg.body)); } catch (e) { console.error(e); }
            });
        },
        onDisconnect: () => setConn(false),
        onError: (e) => { console.warn('Realtime error', e); setConn(false); }
    });
}

function handleOrderEvent(order) {
    if (!order || order.id == null) return;
    const isNew = !knownIds.has(order.id);

    if (!KDS_COLUMNS.includes(order.status)) {
        // Order left the active set (served / cancelled) — drop it from the board.
        orders.delete(order.id);
        knownIds.add(order.id);
        renderAll();
        return;
    }

    orders.set(order.id, order);
    knownIds.add(order.id);
    if (isNew) flashIds.add(order.id);
    renderAll();

    if (isNew && order.status === 'RECEIVED') {
        showToast(`New order #${order.id}${order.tableNumber != null ? ' · Table ' + order.tableNumber : ''}`);
        playBeep();
    }
}

function renderAll() {
    const byColumn = { RECEIVED: [], COOKING: [], READY: [] };
    [...orders.values()].forEach(o => { if (byColumn[o.status]) byColumn[o.status].push(o); });

    KDS_COLUMNS.forEach(status => {
        const list = byColumn[status].sort((a, b) => new Date(a.placedAt || a.createdAt) - new Date(b.placedAt || b.createdAt));
        document.getElementById(`count-${status}`).textContent = list.length;
        const col = document.getElementById(`col-${status}`);
        col.innerHTML = list.length
            ? list.map(renderCard).join('')
            : `<div class="kds-empty"><i class="fas fa-mug-hot"></i>No orders here.</div>`;
    });

    flashIds.forEach(id => {
        const el = document.querySelector(`.kds-card[data-order-id="${id}"]`);
        if (el) el.classList.add('flash');
    });
    flashIds = new Set();
}

function renderCard(order) {
    const items = Array.isArray(order.items) ? order.items : [];
    const table = order.tableNumber != null ? `<span class="kds-table">Table ${order.tableNumber}</span>` : '';
    return `
        <article class="kds-card" data-order-id="${order.id}">
            <div class="kds-card-head">
                <div><span class="kds-order-id">#${order.id}</span>${table}</div>
                <span class="kds-elapsed" data-created="${order.placedAt || order.createdAt}">${formatElapsed(order.placedAt || order.createdAt)}</span>
            </div>
            <ul class="kds-items">
                ${items.map(renderItem).join('') || '<li class="kds-item">No items.</li>'}
            </ul>
            ${renderOrderActions(order)}
        </article>
    `;
}

function renderItem(item) {
    const status = item.itemStatus || 'PENDING';
    const done = status === 'SERVED' || status === 'CANCELLED';
    const note = item.specialNotes
        ? `<div class="kds-note"><i class="fas fa-triangle-exclamation"></i><span>${escapeHtml(item.specialNotes)}</span></div>`
        : '';
    return `
        <li class="kds-item ${done ? 'item-done' : ''}">
            <div class="kds-item-main">
                <div class="kds-item-name"><span class="qty">${item.quantity}&times;</span>${escapeHtml(item.productName || 'Item')}</div>
                ${note}
            </div>
            <div class="kds-item-side">
                <span class="status-pill status-pill-item status-${status}">${prettyStatus(status)}</span>
                ${renderItemBtn(item, status)}
            </div>
        </li>
    `;
}

function renderItemBtn(item, status) {
    if (status === 'PENDING') return `<button class="kds-item-btn" onclick="advanceItem(${item.id}, 'COOKING')">Start</button>`;
    if (status === 'COOKING') return `<button class="kds-item-btn" onclick="advanceItem(${item.id}, 'READY')">Ready</button>`;
    if (status === 'READY') return `<button class="kds-item-btn" onclick="advanceItem(${item.id}, 'SERVED')">Serve</button>`;
    return '';
}

function renderOrderActions(order) {
    if (order.status === 'RECEIVED') {
        return `<div class="kds-actions"><button class="kds-action start" onclick="advanceOrder(${order.id}, 'COOKING')"><i class="fas fa-play"></i> Start Cooking</button></div>`;
    }
    if (order.status === 'COOKING') {
        return `<div class="kds-actions"><button class="kds-action ready" onclick="advanceOrder(${order.id}, 'READY')"><i class="fas fa-check"></i> Mark Ready</button></div>`;
    }
    if (order.status === 'READY') {
        return `<div class="kds-actions"><button class="kds-action serve" onclick="advanceOrder(${order.id}, 'SERVED')"><i class="fas fa-bell-concierge"></i> Mark Served</button></div>`;
    }
    return '';
}

async function advanceItem(itemId, status) {
    await patchAndApply(`/api/v1/kitchen/items/${itemId}/status?status=${status}`);
}

async function advanceOrder(orderId, status) {
    await patchAndApply(`/api/v1/kitchen/orders/${orderId}/status?status=${status}`);
}

// Optimistically apply the server's returned order so the UI updates even if the
// realtime broadcast is delayed; the broadcast (when it arrives) is an idempotent re-apply.
async function patchAndApply(url) {
    try {
        const res = await authenticatedFetch(url, { method: 'PATCH' });
        if (!res.ok) { showToast('Update failed'); return; }
        const json = await res.json();
        if (json.data) handleAppliedUpdate(json.data);
    } catch (e) {
        console.error('Kitchen update failed:', e);
        showToast('Update failed');
    }
}

function handleAppliedUpdate(order) {
    if (!KDS_COLUMNS.includes(order.status)) orders.delete(order.id);
    else orders.set(order.id, order);
    knownIds.add(order.id);
    renderAll();
}

function updateElapsedLabels() {
    document.querySelectorAll('.kds-elapsed[data-created]').forEach(el => {
        const created = el.getAttribute('data-created');
        el.textContent = formatElapsed(created);
        el.classList.remove('warn', 'danger');
        const mins = minutesSince(created);
        if (mins >= DANGER_MINUTES) el.classList.add('danger');
        else if (mins >= WARN_MINUTES) el.classList.add('warn');
    });
}

function setConn(isUp) {
    connected = isUp;
    const el = document.getElementById('connStatus');
    if (!el) return;
    el.className = `kds-conn ${isUp ? 'live' : 'down'}`;
    el.innerHTML = `<span class="dot"></span> ${isUp ? 'Live' : 'Reconnecting…'}`;
}

function toggleSound() {
    soundEnabled = !soundEnabled;
    const btn = document.getElementById('soundToggle');
    btn.classList.toggle('active', soundEnabled);
    btn.innerHTML = soundEnabled
        ? '<i class="fas fa-volume-high"></i> Sound'
        : '<i class="fas fa-volume-xmark"></i> Muted';
    if (soundEnabled) ensureAudio();
}

function ensureAudio() {
    if (!audioCtx) {
        try { audioCtx = new (window.AudioContext || window.webkitAudioContext)(); } catch (e) { return; }
    }
    if (audioCtx.state === 'suspended') audioCtx.resume();
}

function playBeep() {
    if (!soundEnabled) return;
    ensureAudio();
    if (!audioCtx) return;
    try {
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.connect(gain);
        gain.connect(audioCtx.destination);
        osc.type = 'sine';
        osc.frequency.value = 880;
        gain.gain.setValueAtTime(0.18, audioCtx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.0001, audioCtx.currentTime + 0.45);
        osc.start();
        osc.stop(audioCtx.currentTime + 0.45);
    } catch (e) { /* ignore */ }
}

function minutesSince(iso) {
    const t = new Date(iso).getTime();
    if (isNaN(t)) return 0;
    return Math.max(0, Math.floor((Date.now() - t) / 60000));
}

function formatElapsed(iso) {
    const mins = minutesSince(iso);
    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins}m`;
    const h = Math.floor(mins / 60);
    return `${h}h ${mins % 60}m`;
}

function prettyStatus(status) {
    if (!status) return '';
    return status.charAt(0) + status.slice(1).toLowerCase();
}

function escapeHtml(s) {
    return String(s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function showToast(message) {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerHTML = `<i class="fas fa-bell"></i> ${escapeHtml(message)}`;
    container.appendChild(toast);
    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 4500);
}
