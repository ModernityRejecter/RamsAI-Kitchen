const ORDER_STATUS_FLOW = ['RECEIVED', 'COOKING', 'READY', 'SERVED'];
const ITEM_STATUS_FLOW = ['PENDING', 'COOKING', 'READY', 'SERVED'];
const REFRESH_INTERVAL_MS = 15000;

let currentUserId = null;
let currentTableId = null;
let currentTableNumber = null;
let expandedHistoryIds = new Set();
let refreshTimer = null;

document.addEventListener('DOMContentLoaded', async () => {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    if (!token) {
        window.location.href = 'login.html';
        return;
    }

    const refreshBtn = document.getElementById('refreshOrdersBtn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', () => loadAll(true));
    }

    await loadAll(true);
    refreshTimer = setInterval(() => loadAll(false), REFRESH_INTERVAL_MS);
});

window.addEventListener('beforeunload', () => {
    if (refreshTimer) clearInterval(refreshTimer);
});

async function loadAll(showLoading) {
    try {
        await loadCurrentUserId();
        const [orders, tables] = await Promise.all([fetchMyOrders(), fetchTables()]);
        const occupied = findOccupiedTable(tables, currentUserId);
        currentTableId = occupied ? occupied.id : null;
        currentTableNumber = occupied ? occupied.tableNumber : null;

        renderCurrentTableLabel();
        renderCurrentOrders(filterCurrentOrders(orders, tables, occupied));
        renderHistoryOrders(filterHistoryOrders(orders, tables, occupied));
    } catch (err) {
        console.error('Failed to load my orders:', err);
        showStatus('Could not load your orders. Try again in a moment.', 'error');
    }
}

async function loadCurrentUserId() {
    if (currentUserId != null) return;
    try {
        const res = await authenticatedFetch('/api/v1/user/me');
        if (res.ok) {
            const json = await res.json();
            if (json.data && json.data.id != null) {
                currentUserId = json.data.id;
            }
        }
    } catch (e) { /* ignore */ }
}

async function fetchMyOrders() {
    const res = await authenticatedFetch('/api/v1/orders/my');
    if (!res.ok) throw new Error('orders fetch failed');
    const json = await res.json();
    return Array.isArray(json.data) ? json.data : [];
}

async function fetchTables() {
    const res = await authenticatedFetch('/api/v1/tables/map');
    if (!res.ok) return [];
    const json = await res.json();
    return Array.isArray(json.data) ? json.data : [];
}

function findOccupiedTable(tables, userId) {
    if (userId == null) return null;
    return tables.find(t =>
        t.status === 'OCCUPIED' &&
        t.occupiedByUserId != null &&
        Number(t.occupiedByUserId) === Number(userId)
    ) || null;
}

// Tables that share a number form one logical table (multi-square groups).
function tableIdsForNumber(tables, tableNumber) {
    if (tableNumber == null) return new Set();
    return new Set(
        tables
            .filter(t => t.tableNumber === tableNumber)
            .map(t => t.id)
    );
}

function filterCurrentOrders(orders, tables, occupied) {
    if (!occupied) return [];
    const groupIds = tableIdsForNumber(tables, occupied.tableNumber);
    const sessionStart = occupied.occupiedAt ? new Date(occupied.occupiedAt) : null;
    return orders.filter(o => {
        if (o.tableId == null || !groupIds.has(o.tableId)) return false;
        if (sessionStart && o.updatedAt) return new Date(o.updatedAt) >= sessionStart;
        return true;
    });
}

function filterHistoryOrders(orders, tables, occupied) {
    if (!occupied) return orders.slice();
    const groupIds = tableIdsForNumber(tables, occupied.tableNumber);
    const sessionStart = occupied.occupiedAt ? new Date(occupied.occupiedAt) : null;
    return orders.filter(o => {
        if (o.tableId == null || !groupIds.has(o.tableId)) return true;
        if (sessionStart && o.updatedAt) return new Date(o.updatedAt) < sessionStart;
        return false;
    });
}

function renderCurrentTableLabel() {
    const label = document.getElementById('currentTableLabel');
    if (!label) return;
    label.textContent = currentTableNumber != null ? `· Table ${currentTableNumber}` : '';
}

function renderCurrentOrders(orders) {
    const container = document.getElementById('currentOrdersList');
    const noTableMsg = document.getElementById('noCurrentTableMsg');
    if (!container || !noTableMsg) return;

    if (currentTableId == null) {
        container.innerHTML = '';
        noTableMsg.style.display = 'block';
        return;
    }
    noTableMsg.style.display = 'none';

    if (orders.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-mug-hot"></i>
                <p>No orders yet at this table. Head to the menu to order something delicious.</p>
                <a href="menu.html" class="cta-button" style="display: inline-block; margin-top: 10px;">Browse Menu</a>
            </div>
        `;
        return;
    }

    container.innerHTML = orders.map(renderCurrentOrderCard).join('');
}

function renderCurrentOrderCard(order) {
    const orderStatus = order.status || 'RECEIVED';
    const items = Array.isArray(order.items) ? order.items : [];
    const total = formatMoney(order.totalPrice);
    const placed = formatDateTime(order.createdAt);

    return `
        <article class="order-card order-card-current">
            <div class="order-card-header">
                <div>
                    <h4>Order #${order.id}</h4>
                    <div class="order-meta">Placed ${placed} &middot; ${items.length} item(s) &middot; ${total}</div>
                </div>
                ${renderStatusBadge(orderStatus, 'order')}
            </div>

            ${renderOrderProgress(orderStatus)}

            <div class="order-items">
                <h5>Items</h5>
                <ul class="order-items-list-detailed">
                    ${items.map(renderItemRow).join('') || '<li class="empty-item">No items.</li>'}
                </ul>
            </div>
        </article>
    `;
}

function renderItemRow(item) {
    const status = item.itemStatus || 'PENDING';
    const unit = formatMoney(item.unitPrice);
    const line = formatMoney((Number(item.unitPrice) || 0) * (Number(item.quantity) || 0));
    const notes = item.specialNotes ? `<div class="item-notes"><i class="fas fa-note-sticky"></i> ${escapeHtml(item.specialNotes)}</div>` : '';
    return `
        <li class="order-item-row">
            <div class="order-item-row-main">
                <div class="order-item-row-name">
                    <span class="qty">${item.quantity}&times;</span>
                    <span>${escapeHtml(item.productName || 'Item')}</span>
                </div>
                <div class="order-item-row-price">${unit} ea &middot; ${line}</div>
                ${notes}
            </div>
            ${renderStatusBadge(status, 'item')}
        </li>
    `;
}

function renderOrderProgress(status) {
    if (status === 'CANCELLED') {
        return `<div class="order-progress order-progress-cancelled">Order was cancelled.</div>`;
    }
    const activeIndex = ORDER_STATUS_FLOW.indexOf(status);
    return `
        <ol class="order-progress">
            ${ORDER_STATUS_FLOW.map((step, i) => {
                let cls = 'pending';
                if (activeIndex > i) cls = 'done';
                else if (activeIndex === i) cls = 'active';
                return `<li class="step ${cls}"><span class="dot"></span><span class="label">${prettyStatus(step)}</span></li>`;
            }).join('')}
        </ol>
    `;
}

function renderHistoryOrders(orders) {
    const container = document.getElementById('historyList');
    if (!container) return;

    if (orders.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-receipt"></i>
                <p>No past orders yet.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = orders.map(renderHistoryRow).join('');

    container.querySelectorAll('.history-row').forEach(row => {
        const id = row.dataset.orderId;
        row.querySelector('.history-row-summary').addEventListener('click', () => toggleHistoryRow(id));
    });
}

function renderHistoryRow(order) {
    const id = String(order.id);
    const expanded = expandedHistoryIds.has(id);
    const items = Array.isArray(order.items) ? order.items : [];
    const status = order.status || 'RECEIVED';
    const tableLabel = order.tableNumber != null ? `Table ${order.tableNumber}` : 'No table';
    const placed = formatDateTime(order.createdAt);

    return `
        <article class="history-row ${expanded ? 'expanded' : ''}" data-order-id="${id}">
            <div class="history-row-summary">
                <div class="history-row-main">
                    <div class="history-row-title">Order #${order.id} <span class="history-row-table">&middot; ${tableLabel}</span></div>
                    <div class="history-row-meta">${placed} &middot; ${items.length} item(s) &middot; ${formatMoney(order.totalPrice)}</div>
                </div>
                <div class="history-row-right">
                    ${renderStatusBadge(status, 'order')}
                    <i class="fas fa-chevron-down expand-chevron"></i>
                </div>
            </div>
            <div class="history-row-details">
                ${renderOrderProgress(status)}
                <ul class="order-items-list-detailed">
                    ${items.map(renderItemRow).join('') || '<li class="empty-item">No items recorded.</li>'}
                </ul>
                <div class="history-row-footer">
                    <span>Last updated ${formatDateTime(order.updatedAt)}</span>
                    <span class="history-total">Total: ${formatMoney(order.totalPrice)}</span>
                </div>
            </div>
        </article>
    `;
}

function toggleHistoryRow(id) {
    const key = String(id);
    if (expandedHistoryIds.has(key)) expandedHistoryIds.delete(key);
    else expandedHistoryIds.add(key);

    const row = document.querySelector(`.history-row[data-order-id="${key}"]`);
    if (row) row.classList.toggle('expanded');
}

function renderStatusBadge(status, kind) {
    const normalized = (status || '').toUpperCase();
    return `<span class="status-pill status-pill-${kind} status-${normalized}">${prettyStatus(normalized)}</span>`;
}

function prettyStatus(status) {
    if (!status) return '';
    return status.charAt(0) + status.slice(1).toLowerCase();
}

function formatMoney(value) {
    const n = Number(value);
    if (!isFinite(n)) return '$0.00';
    return `$${n.toFixed(2)}`;
}

function formatDateTime(value) {
    if (!value) return '—';
    const d = new Date(value);
    if (isNaN(d.getTime())) return String(value);
    return d.toLocaleString();
}

function escapeHtml(s) {
    return String(s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function showStatus(message, type) {
    const el = document.getElementById('myOrdersStatus');
    if (!el) return;
    el.textContent = message;
    el.className = `my-orders-status ${type || ''}`;
    setTimeout(() => {
        el.textContent = '';
        el.className = 'my-orders-status';
    }, 4000);
}
