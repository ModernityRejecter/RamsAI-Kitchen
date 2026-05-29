const ORDER_STATUS_FLOW = ['RECEIVED', 'COOKING', 'READY', 'SERVED'];
const ITEM_STATUS_FLOW = ['PENDING', 'COOKING', 'READY', 'SERVED'];
// Polling is now a safety net behind realtime push, so it can run less often.
const REFRESH_INTERVAL_MS = 30000;

let currentUserId = null;
let currentTableId = null;
let currentTableNumber = null;
let expandedHistoryIds = new Set();
let refreshTimer = null;
let myReviewsMap = new Map();

// Realtime live-tracking state
let liveClient = null;
let liveConnected = false;
const orderSubs = new Map();          // orderId -> STOMP subscription
const lastKnownStatus = new Map();    // orderId -> last seen status (to detect transitions)
let liveRefreshTimer = null;

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
    connectLiveTracking();
    refreshTimer = setInterval(() => loadAll(false), REFRESH_INTERVAL_MS);
});

window.addEventListener('beforeunload', () => {
    if (refreshTimer) clearInterval(refreshTimer);
    if (liveClient) { try { liveClient.deactivate(); } catch (e) { /* ignore */ } }
});

async function loadAll(showLoading) {
    try {
        await loadCurrentUserId();
        const [orders, tables, reviews] = await Promise.all([fetchMyOrders(), fetchTables(), fetchMyReviews()]);
        
        myReviewsMap.clear();
        if (reviews) {
            reviews.forEach(r => myReviewsMap.set(r.productId, r));
        }

        const occupied = findOccupiedTable(tables, currentUserId);
        currentTableId = occupied ? occupied.id : null;
        currentTableNumber = occupied ? occupied.tableNumber : null;

        const currentOrders = filterCurrentOrders(orders, tables, occupied);
        renderCurrentTableLabel();
        renderCurrentOrders(currentOrders);
        renderHistoryOrders(filterHistoryOrders(orders, tables, occupied));
        trackOrders(currentOrders);
        updateLiveIndicator();
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

async function fetchMyReviews() {
    try {
        const res = await authenticatedFetch('/api/v1/reviews/me');
        if (!res.ok) return [];
        const json = await res.json();
        return Array.isArray(json.data) ? json.data : [];
    } catch (e) {
        return [];
    }
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
                    ${items.map(i => renderItemRow(i, orderStatus)).join('') || '<li class="empty-item">No items.</li>'}
                </ul>
            </div>
        </article>
    `;
}

function renderItemRow(item, orderStatus) {
    const status = item.itemStatus || 'PENDING';
    const unit = formatMoney(item.unitPrice);
    const line = formatMoney((Number(item.unitPrice) || 0) * (Number(item.quantity) || 0));
    const notes = item.specialNotes ? `<div class="item-notes"><i class="fas fa-note-sticky"></i> ${escapeHtml(item.specialNotes)}</div>` : '';
    
    let reviewHtml = '';
    if (orderStatus === 'SERVED' && item.productId) {
        const review = myReviewsMap.get(item.productId);
        if (review) {
            reviewHtml = `<div class="item-review-status">
                <span class="rating-stars">★ ${review.rating}</span>
                <button class="text-btn" onclick="openReviewModal(${item.productId}, '${escapeHtml(item.productName)}', ${review.id}, ${review.rating}, '${escapeHtml(review.comment || '')}')">Edit Review</button>
            </div>`;
        } else {
            reviewHtml = `<div class="item-review-status">
                <button class="cta-button btn-small" onclick="openReviewModal(${item.productId}, '${escapeHtml(item.productName)}')">Leave a Review</button>
            </div>`;
        }
    }

    return `
        <li class="order-item-row">
            <div class="order-item-row-main">
                <div class="order-item-row-name">
                    <span class="qty">${item.quantity}&times;</span>
                    <span>${escapeHtml(item.productName || 'Item')}</span>
                </div>
                <div class="order-item-row-price">${unit} ea &middot; ${line}</div>
                ${notes}
                ${reviewHtml}
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
                    ${items.map(i => renderItemRow(i, status)).join('') || '<li class="empty-item">No items recorded.</li>'}
                </ul>
                <div class="history-row-footer">
                    <span>Last updated ${formatDateTime(order.updatedAt)}</span>
                    <span class="history-total">Total: ${formatMoney(order.totalPrice)}</span>
                </div>
            </div>
        </article>
    `;
}

// ---- Realtime live tracking (STOMP over SockJS, falls back to polling) ----

function connectLiveTracking() {
    liveClient = createRealtimeClient({
        onConnect: (client) => {
            liveConnected = true;
            updateLiveIndicator();
            // Broker subscriptions don't survive a reconnect — re-subscribe to all tracked orders.
            orderSubs.clear();
            [...lastKnownStatus.keys()].forEach(id => subscribeOrder(id));
        },
        onDisconnect: () => { liveConnected = false; orderSubs.clear(); updateLiveIndicator(); },
        onError: () => { liveConnected = false; updateLiveIndicator(); }
    });
    updateLiveIndicator();
}

// Keep STOMP subscriptions aligned with the orders currently shown for this table.
function trackOrders(currentOrders) {
    const next = new Set(currentOrders.map(o => o.id));
    currentOrders.forEach(o => {
        if (!lastKnownStatus.has(o.id)) lastKnownStatus.set(o.id, o.status);
    });
    next.forEach(id => subscribeOrder(id));
    orderSubs.forEach((sub, id) => {
        if (!next.has(id)) {
            try { sub.unsubscribe(); } catch (e) { /* ignore */ }
            orderSubs.delete(id);
            lastKnownStatus.delete(id);
        }
    });
}

function subscribeOrder(id) {
    if (!liveClient || !liveConnected || orderSubs.has(id)) return;
    const sub = liveClient.subscribe(`/topic/orders/${id}`, (msg) => {
        try { onOrderEvent(JSON.parse(msg.body)); } catch (e) { /* ignore */ }
    });
    orderSubs.set(id, sub);
}

function onOrderEvent(order) {
    if (!order || order.id == null) return;
    const prev = lastKnownStatus.get(order.id);
    lastKnownStatus.set(order.id, order.status);
    if (order.status === 'READY' && prev !== 'READY') {
        showToast(`Order #${order.id} is ready to serve!`, 'toast-ready');
    }
    scheduleLiveRefresh();
}

// Coalesce bursts of events into a single refresh.
function scheduleLiveRefresh() {
    if (liveRefreshTimer) return;
    liveRefreshTimer = setTimeout(() => {
        liveRefreshTimer = null;
        loadAll(false);
    }, 350);
}

function updateLiveIndicator() {
    const el = document.getElementById('liveIndicator');
    if (!el) return;
    if (currentTableId == null) { el.style.display = 'none'; return; }
    el.style.display = 'inline-flex';
    el.className = `live-indicator ${liveConnected ? 'live' : ''}`;
    el.innerHTML = `<span class="dot"></span> ${liveConnected ? 'Live' : 'Offline'}`;
}

function showToast(message, extraClass) {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = `toast ${extraClass || ''}`;
    toast.innerHTML = `<i class="fas fa-bell-concierge"></i> ${escapeHtml(message)}`;
    container.appendChild(toast);
    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 5000);
}

// Review Modal Logic
function openReviewModal(productId, productName, reviewId = null, existingRating = 5, existingComment = '') {
    let modal = document.getElementById('reviewModal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'reviewModal';
        modal.className = 'modal-overlay';
        document.body.appendChild(modal);
    }
    
    modal.innerHTML = `
        <div class="modal-content review-modal-content">
            <span class="close-modal" onclick="closeReviewModal()">&times;</span>
            <h2>Review ${productName}</h2>
            <form id="reviewForm" onsubmit="submitReview(event, ${productId}, ${reviewId})">
                <div class="form-group">
                    <label>Rating (1-5)</label>
                    <div class="star-rating-input">
                        ${[1,2,3,4,5].map(i => 
                            `<i class="fas fa-star star-opt ${i <= existingRating ? 'active' : ''}" data-val="${i}" onclick="setRating(${i})"></i>`
                        ).join('')}
                    </div>
                    <input type="hidden" id="reviewRating" value="${existingRating}">
                </div>
                <div class="form-group">
                    <label>Comment</label>
                    <textarea id="reviewComment" rows="4" maxlength="500">${existingComment}</textarea>
                </div>
                <div class="modal-actions">
                    <button type="button" class="btn-cancel" onclick="closeReviewModal()">Cancel</button>
                    ${reviewId ? `<button type="button" class="btn-danger" onclick="deleteReview(${reviewId})">Delete</button>` : ''}
                    <button type="submit" class="btn-primary">Save Review</button>
                </div>
            </form>
        </div>
    `;
    modal.style.display = 'flex';
}

function setRating(val) {
    document.getElementById('reviewRating').value = val;
    document.querySelectorAll('.star-rating-input .star-opt').forEach(el => {
        if (parseInt(el.dataset.val) <= val) {
            el.classList.add('active');
        } else {
            el.classList.remove('active');
        }
    });
}

function closeReviewModal() {
    const modal = document.getElementById('reviewModal');
    if (modal) modal.style.display = 'none';
}

async function submitReview(e, productId, reviewId) {
    e.preventDefault();
    const rating = document.getElementById('reviewRating').value;
    const comment = document.getElementById('reviewComment').value;
    
    const payload = {
        productId: productId,
        rating: parseInt(rating),
        comment: comment
    };
    
    try {
        let url = '/api/v1/reviews';
        let method = 'POST';
        if (reviewId) {
            url = `/api/v1/reviews/${reviewId}`;
            method = 'PUT';
        }
        
        const res = await authenticatedFetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });
        
        if (res.ok) {
            closeReviewModal();
            showStatus('Review saved successfully.', 'success');
            loadAll(false);
        } else {
            const err = await res.json();
            alert(err.message || 'Failed to save review');
        }
    } catch (e) {
        alert('Error saving review');
    }
}

async function deleteReview(reviewId) {
    if (!confirm('Are you sure you want to delete this review?')) return;
    try {
        const res = await authenticatedFetch(`/api/v1/reviews/${reviewId}`, {
            method: 'DELETE'
        });
        if (res.ok) {
            closeReviewModal();
            showStatus('Review deleted.', 'success');
            loadAll(false);
        } else {
            const err = await res.json();
            alert(err.message || 'Failed to delete review');
        }
    } catch (e) {
        alert('Error deleting review');
    }
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
