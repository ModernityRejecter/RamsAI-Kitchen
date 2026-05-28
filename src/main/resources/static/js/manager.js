let managerProducts = [];
let managerIngredients = [];
let currentUserRole = '';
let currentFilter = 'ALL';
let productToReject = null;

document.addEventListener('DOMContentLoaded', async () => {
    if (!enforceConsoleAccess()) return;
    await bootstrapManagerPage();
    registerHandlers();
});

function enforceConsoleAccess() {
    const role = localStorage.getItem('role') || sessionStorage.getItem('role');
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    if (!token) {
        window.location.href = 'login.html';
        return false;
    }
    if (role !== 'MANAGER' && role !== 'CHEF') {
        alert('Only managers and chefs can access this page.');
        window.location.href = 'index.html';
        return false;
    }
    currentUserRole = role;
    return true;
}

async function bootstrapManagerPage() {
    const promises = [
        loadCategories(),
        loadProducts(),
        loadLowStock(),
        loadIngredients()
    ];
    if (currentUserRole === 'MANAGER') {
        promises.push(loadInventoryLogs());
    }
    await Promise.all(promises);
    setupConsoleUI();
}

function setupConsoleUI() {
    const titleNode = document.getElementById('consoleTitle');
    const createHeader = document.getElementById('createProductHeader');
    const createDesc = document.getElementById('createProductDesc');
    const createBtn = document.getElementById('createProductBtn');
    const productMgmtDesc = document.getElementById('productManagementDesc');

    if (currentUserRole === 'CHEF') {
        if (titleNode) titleNode.textContent = 'Chef Console';
        if (createHeader) createHeader.textContent = 'Propose Product';
        if (createDesc) createDesc.textContent = 'Propose new items for the menu (requires manager approval).';
        if (createBtn) createBtn.textContent = 'Propose Product';
        if (productMgmtDesc) productMgmtDesc.textContent = 'Track your proposed products and review rejection feedback.';
        
        // Hide Manager-only cards
        const managerCards = [
            'createIngredientCard',
            'manageIngredientCard',
            'stockAdjustmentCard',
            'inventoryLogsCard'
        ];
        managerCards.forEach(id => {
            const node = document.getElementById(id);
            if (node) node.style.display = 'none';
        });
    } else {
        if (titleNode) titleNode.textContent = 'Manager Console';
        if (createHeader) createHeader.textContent = 'Create Product';
        if (createDesc) createDesc.textContent = 'Define new menu items and pricing.';
        if (createBtn) createBtn.textContent = 'Create Product';
        if (productMgmtDesc) productMgmtDesc.textContent = 'Approve proposed items, toggle visibility on the menu, and review feedback.';
    }
}

function registerHandlers() {
    document.getElementById('createProductForm').addEventListener('submit', onCreateProduct);
    const editForm = document.getElementById('editProductForm');
    if (editForm) editForm.addEventListener('submit', onEditProduct);
    
    document.getElementById('addBomForm').addEventListener('submit', onAddBomRow);
    document.getElementById('bomProductId').addEventListener('change', () => {
        const productId = Number(document.getElementById('bomProductId').value);
        if (productId) loadBomRows(productId);
    });

    if (currentUserRole === 'MANAGER') {
        document.getElementById('createIngredientForm').addEventListener('submit', onCreateIngredient);
        document.getElementById('adjustStockForm').addEventListener('submit', onAdjustStock);
        const editIngForm = document.getElementById('editIngredientForm');
        if (editIngForm) editIngForm.addEventListener('submit', onEditIngredient);
    }

    // Tab Filter Handlers
    document.getElementById('tab-all').addEventListener('click', () => setProductFilter('ALL'));
    document.getElementById('tab-pending').addEventListener('click', () => setProductFilter('PENDING'));
    document.getElementById('tab-approved').addEventListener('click', () => setProductFilter('APPROVED'));
    document.getElementById('tab-rejected').addEventListener('click', () => setProductFilter('REJECTED'));
}

function setProductFilter(filter) {
    currentFilter = filter;
    // Toggle active class on tabs
    const tabs = ['all', 'pending', 'approved', 'rejected'];
    tabs.forEach(t => {
        const btn = document.getElementById(`tab-${t}`);
        if (btn) {
            if (t === filter.toLowerCase()) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        }
    });
    renderProductList();
}

async function onCreateProduct(e) {
    e.preventDefault();
    const form = e.target;
    const payload = {
        name: form.name.value.trim(),
        description: form.description.value.trim() || null,
        basePrice: Number(form.basePrice.value),
        categoryId: Number(form.categoryId.value),
        isSpecialOffer: form.isSpecialOffer.checked,
        isDailyRecipe: form.isDailyRecipe.checked,
        discountPrice: form.discountPrice.value ? Number(form.discountPrice.value) : null
    };

    const result = await apiRequest('/api/v1/products', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    setStatus('productStatus', result.ok, result.message || 'Product creation finished.');
    if (result.ok) {
        form.reset();
        await loadProducts();
    }
}

async function onCreateIngredient(e) {
    e.preventDefault();
    const form = e.target;
    const payload = {
        name: form.name.value.trim(),
        unit: form.unit.value.trim(),
        currentStock: Number(form.currentStock.value),
        minimumStockThreshold: Number(form.minimumStockThreshold.value)
    };

    const result = await apiRequest('/api/v1/inventory/ingredients', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    setStatus('ingredientStatus', result.ok, result.message || 'Ingredient operation finished.');
    if (result.ok) {
        form.reset();
        await loadIngredients();
        await loadLowStock();
    }
}

async function onAddBomRow(e) {
    e.preventDefault();
    const productId = Number(document.getElementById('bomProductId').value);
    const payload = {
        ingredientId: Number(document.getElementById('bomIngredientId').value),
        quantityRequired: Number(document.getElementById('bomQuantityRequired').value)
    };
    const result = await apiRequest(`/api/v1/inventory/products/${productId}/ingredients`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    setStatus('bomStatus', result.ok, result.message || 'Recipe row operation finished.');
    if (result.ok) {
        document.getElementById('bomQuantityRequired').value = '';
        await loadBomRows(productId);
    }
}

async function onAdjustStock(e) {
    e.preventDefault();
    const ingredientId = Number(document.getElementById('stockIngredientId').value);
    const payload = {
        quantity: Number(document.getElementById('stockQuantity').value),
        reason: document.getElementById('stockReason').value
    };
    const result = await apiRequest(`/api/v1/inventory/ingredients/${ingredientId}/stock`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    setStatus('stockStatus', result.ok, result.message || 'Stock update finished.');
    if (result.ok) {
        document.getElementById('stockQuantity').value = '';
        await Promise.all([loadIngredients(), loadLowStock(), loadInventoryLogs()]);
    }
}

async function loadCategories() {
    const result = await apiRequest('/api/v1/products/categories');
    const select = document.getElementById('productCategoryId');
    const editSelect = document.getElementById('editProductCategoryId');
    if (select) select.innerHTML = '';
    if (editSelect) editSelect.innerHTML = '';
    if (!result.ok) return;

    result.data.forEach(category => {
        if (select) {
            const option = document.createElement('option');
            option.value = category.id;
            option.textContent = category.name;
            select.appendChild(option);
        }
        if (editSelect) {
            const option = document.createElement('option');
            option.value = category.id;
            option.textContent = category.name;
            editSelect.appendChild(option);
        }
    });
}

async function loadProducts() {
    const result = await apiRequest('/api/v1/products/manage');
    if (!result.ok) return;
    managerProducts = result.data || [];

    const bomProduct = document.getElementById('bomProductId');
    if (bomProduct) {
        bomProduct.innerHTML = managerProducts.map(product =>
            `<option value="${product.id}">${escapeHtml(product.name)}</option>`
        ).join('');

        if (managerProducts.length > 0) {
            await loadBomRows(managerProducts[0].id);
        } else {
            document.getElementById('bomRowsTable').innerHTML = '<tr><td colspan="4">No products found.</td></tr>';
        }
    }
    renderProductList();
}

function renderProductList() {
    const tableBody = document.getElementById('productManagementTableBody');
    if (!tableBody) return;

    // Filter products
    let filtered = managerProducts;
    if (currentUserRole === 'CHEF') {
        if (currentFilter === 'ALL') {
            filtered = managerProducts.filter(p => p.approvalStatus === 'PENDING' || p.approvalStatus === 'REJECTED' || p.approvalStatus === 'APPROVED');
        } else {
            filtered = managerProducts.filter(p => p.approvalStatus === currentFilter);
        }
    } else {
        if (currentFilter !== 'ALL') {
            filtered = managerProducts.filter(p => p.approvalStatus === currentFilter);
        }
    }

    if (filtered.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:var(--text-gray); padding: 20px 0;">No products found in this category.</td></tr>`;
        return;
    }

    tableBody.innerHTML = filtered.map(product => {
        const statusClass = product.approvalStatus.toLowerCase();
        const visibilityText = product.isActive 
            ? '<span style="color:#059669; font-weight:600;"><i class="fas fa-check-circle"></i> Yes</span>' 
            : '<span style="color:#dc2626; font-weight:600;"><i class="fas fa-times-circle"></i> No</span>';
        
        let actions = '';
        if (currentUserRole === 'MANAGER') {
            if (product.approvalStatus === 'PENDING') {
                actions += `
                    <button type="button" class="action-btn-sm approve" onclick="approveProduct(${product.id})">
                        <i class="fas fa-check"></i> Approve
                    </button>
                    <button type="button" class="action-btn-sm reject" onclick="openRejectModal(${product.id})">
                        <i class="fas fa-times"></i> Reject
                    </button>
                `;
            } else if (product.approvalStatus === 'REJECTED') {
                actions += `
                    <button type="button" class="action-btn-sm approve" onclick="approveProduct(${product.id})">
                        <i class="fas fa-check"></i> Approve
                    </button>
                `;
            } else {
                actions += `
                    <button type="button" class="action-btn-sm toggle-active" onclick="toggleActive(${product.id}, ${!product.isActive})">
                        <i class="fas fa-power-off"></i> ${product.isActive ? 'Deactivate' : 'Activate'}
                    </button>
                `;
            }
        }

        // Add Edit and Delete buttons for both Manager and Chef
        actions += `
            <button type="button" class="action-btn-sm edit-btn" onclick="openEditModal(${product.id})">
                <i class="fas fa-edit"></i> Edit
            </button>
            <button type="button" class="action-btn-sm reject" onclick="deleteProduct(${product.id})">
                <i class="fas fa-trash"></i> Delete
            </button>
        `;

        // If there is rejection feedback, show the "Feedback" button
        if (product.approvalStatus === 'REJECTED' && product.rejectionFeedback) {
            actions += `
                <button type="button" class="action-btn-sm feedback-btn" onclick="toggleFeedback(${product.id})">
                    <i class="fas fa-comment-dots"></i> Feedback
                </button>
            `;
        }

        if (!actions) {
            actions = '<span style="color:var(--text-gray);">No actions available</span>';
        }

        const feedbackRow = (product.approvalStatus === 'REJECTED' && product.rejectionFeedback) ? `
            <tr id="feedback-row-${product.id}" style="display:none; background-color:#fee2e2;">
                <td colspan="6" style="padding:10px 15px; border-bottom:1px solid var(--border-color);">
                    <div style="color:#b91c1c; font-size:0.85rem; display:flex; align-items:center; gap:8px;">
                        <i class="fas fa-exclamation-circle"></i>
                        <span><strong>Rejection Feedback:</strong> ${escapeHtml(product.rejectionFeedback)}</span>
                    </div>
                </td>
            </tr>
        ` : '';

        return `
            <tr>
                <td><strong>${escapeHtml(product.name)}</strong></td>
                <td>${escapeHtml(product.categoryName)}</td>
                <td>$${product.basePrice.toFixed(2)}</td>
                <td><span class="status-badge ${statusClass}">${product.approvalStatus}</span></td>
                <td>${visibilityText}</td>
                <td>
                    <div style="display:flex; gap:6px; flex-wrap:wrap;">
                        ${actions}
                    </div>
                </td>
            </tr>
            ${feedbackRow}
        `;
    }).join('');
}

function toggleFeedback(productId) {
    const row = document.getElementById(`feedback-row-${productId}`);
    if (row) {
        row.style.display = row.style.display === 'none' ? 'table-row' : 'none';
    }
}

async function approveProduct(id) {
    if (!confirm('Are you sure you want to approve this product?')) return;
    const result = await apiRequest(`/api/v1/products/${id}/approve`, { method: 'PATCH' });
    if (result.ok) {
        await loadProducts();
    } else {
        alert('Failed to approve product: ' + result.message);
    }
}

function openRejectModal(id) {
    productToReject = id;
    const modal = document.getElementById('rejectionModal');
    if (modal) modal.style.display = 'flex';
}

function closeRejectModal() {
    productToReject = null;
    const modal = document.getElementById('rejectionModal');
    if (modal) modal.style.display = 'none';
    const feedbackInput = document.getElementById('rejectionFeedback');
    if (feedbackInput) feedbackInput.value = '';
}

async function submitRejection() {
    const feedbackInput = document.getElementById('rejectionFeedback');
    const feedback = feedbackInput ? feedbackInput.value.trim() : '';
    if (!feedback) {
        alert('Please provide feedback for rejection.');
        return;
    }
    const result = await apiRequest(`/api/v1/products/${productToReject}/reject`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ feedback })
    });
    if (result.ok) {
        closeRejectModal();
        await loadProducts();
    } else {
        alert('Failed to reject product: ' + result.message);
    }
}

async function toggleActive(id, isActive) {
    const result = await apiRequest(`/api/v1/products/${id}/active`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ isActive })
    });
    if (result.ok) {
        await loadProducts();
    } else {
        alert('Failed to update product status: ' + result.message);
    }
}

async function deleteProduct(id) {
    if (!confirm('Are you sure you want to delete this product? This cannot be undone.')) return;
    const result = await apiRequest(`/api/v1/products/${id}`, { method: 'DELETE' });
    if (result.ok) {
        await loadProducts();
    } else {
        alert('Failed to delete product: ' + result.message);
    }
}

async function loadIngredients() {
    const result = await apiRequest('/api/v1/inventory/ingredients');
    if (!result.ok) return;
    managerIngredients = result.data || [];

    const ingredientOptions = managerIngredients.map(ingredient =>
        `<option value="${ingredient.id}">${escapeHtml(ingredient.name)} (${escapeHtml(ingredient.unit)})</option>`
    ).join('');

    const bomIngredient = document.getElementById('bomIngredientId');
    if (bomIngredient) bomIngredient.innerHTML = ingredientOptions;

    const stockIngredient = document.getElementById('stockIngredientId');
    if (stockIngredient) stockIngredient.innerHTML = ingredientOptions;

    renderIngredientList();
}

function renderIngredientList() {
    const tableBody = document.getElementById('manageIngredientTableBody');
    if (!tableBody) return;

    if (managerIngredients.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:var(--text-gray); padding: 20px 0;">No ingredients found.</td></tr>`;
        return;
    }

    tableBody.innerHTML = managerIngredients.map(ing => `
        <tr>
            <td><strong>${escapeHtml(ing.name)}</strong></td>
            <td>${escapeHtml(ing.unit)}</td>
            <td>${ing.currentStock}</td>
            <td>${ing.minimumStockThreshold}</td>
            <td>
                <div style="display:flex; gap:6px; flex-wrap:wrap;">
                    <button type="button" class="action-btn-sm edit-btn" onclick="openEditIngredientModal(${ing.id})">
                        <i class="fas fa-edit"></i> Edit
                    </button>
                    <button type="button" class="action-btn-sm reject" onclick="deleteIngredient(${ing.id})">
                        <i class="fas fa-trash"></i> Delete
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

function openEditIngredientModal(id) {
    const ing = managerIngredients.find(i => i.id === id);
    if (!ing) return;

    document.getElementById('editIngredientId').value = ing.id;
    document.getElementById('editIngredientName').value = ing.name;
    document.getElementById('editIngredientUnit').value = ing.unit;
    document.getElementById('editIngredientCurrentStock').value = ing.currentStock;
    document.getElementById('editIngredientMinThreshold').value = ing.minimumStockThreshold;

    const modal = document.getElementById('editIngredientModal');
    if (modal) modal.style.display = 'flex';
}

function closeEditIngredientModal() {
    const modal = document.getElementById('editIngredientModal');
    if (modal) modal.style.display = 'none';
    const form = document.getElementById('editIngredientForm');
    if (form) form.reset();
}

async function onEditIngredient(e) {
    e.preventDefault();
    const form = e.target;
    const id = Number(form.id.value);
    const payload = {
        name: form.name.value.trim(),
        unit: form.unit.value.trim(),
        currentStock: Number(form.currentStock.value),
        minimumStockThreshold: Number(form.minimumStockThreshold.value)
    };

    const result = await apiRequest(`/api/v1/inventory/ingredients/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });

    if (result.ok) {
        closeEditIngredientModal();
        await loadIngredients();
        await loadLowStock();
    } else {
        alert('Failed to update ingredient: ' + result.message);
    }
}

async function deleteIngredient(id) {
    if (!confirm('Are you sure you want to delete this ingredient?')) return;
    const result = await apiRequest(`/api/v1/inventory/ingredients/${id}`, { method: 'DELETE' });
    if (result.ok) {
        await loadIngredients();
        await loadLowStock();
    } else {
        alert('Failed to delete ingredient: ' + result.message);
    }
}

async function loadBomRows(productId) {
    const result = await apiRequest(`/api/v1/inventory/products/${productId}/ingredients`);
    const tableBody = document.getElementById('bomRowsTable');
    if (!tableBody) return;
    if (!result.ok) {
        tableBody.innerHTML = '<tr><td colspan="4">Failed to load recipe rows.</td></tr>';
        return;
    }
    const rows = result.data || [];
    if (rows.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="4">No ingredients assigned yet.</td></tr>';
        return;
    }
    tableBody.innerHTML = rows.map(row => `
        <tr>
            <td>${escapeHtml(row.ingredientName)}</td>
            <td>${escapeHtml(row.ingredientUnit)}</td>
            <td>${row.quantityRequired}</td>
            <td>
                <button class="small-btn delete" onclick="removeBomRow(${row.id}, ${productId})">Remove</button>
            </td>
        </tr>
    `).join('');
}

async function removeBomRow(rowId, productId) {
    if (!confirm('Remove this recipe row?')) return;
    const result = await apiRequest(`/api/v1/inventory/product-ingredients/${rowId}`, { method: 'DELETE' });
    setStatus('bomStatus', result.ok, result.message || 'Recipe row delete finished.');
    if (result.ok) {
        await loadBomRows(productId);
        await loadProducts();
    }
}

async function loadLowStock() {
    const result = await apiRequest('/api/v1/inventory/ingredients/low-stock');
    const tableBody = document.getElementById('lowStockTable');
    if (!tableBody) return;
    if (!result.ok) {
        tableBody.innerHTML = '<tr><td colspan="4">Failed to load low-stock ingredients.</td></tr>';
        return;
    }
    const rows = result.data || [];
    if (rows.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="4">No low-stock ingredients right now.</td></tr>';
        return;
    }
    tableBody.innerHTML = rows.map(row => `
        <tr>
            <td class="danger">${escapeHtml(row.name)}</td>
            <td>${row.currentStock}</td>
            <td>${row.minimumStockThreshold}</td>
            <td>${escapeHtml(row.unit)}</td>
        </tr>
    `).join('');
}

async function loadInventoryLogs() {
    const result = await apiRequest('/api/v1/inventory/logs');
    const tableBody = document.getElementById('inventoryLogsTable');
    if (!tableBody) return;
    if (!result.ok) {
        tableBody.innerHTML = '<tr><td colspan="4">Failed to load logs.</td></tr>';
        return;
    }
    const rows = result.data || [];
    if (rows.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="4">No inventory logs yet.</td></tr>';
        return;
    }
    tableBody.innerHTML = rows.map(log => `
        <tr>
            <td>${escapeHtml(log.ingredientName)}</td>
            <td>${log.changeAmount}</td>
            <td>${escapeHtml(log.reason)}</td>
            <td>${new Date(log.timestamp).toLocaleString()}</td>
        </tr>
    `).join('');
}

async function apiRequest(url, options = {}) {
    try {
        const response = await authenticatedFetch(url, options);
        const contentType = response.headers.get('content-type') || '';
        const body = contentType.includes('application/json') ? await response.json() : {};
        return {
            ok: response.ok,
            data: body.data,
            message: body.message || body.error || 'Request failed'
        };
    } catch (error) {
        console.error(`API error on ${url}`, error);
        return { ok: false, message: 'Network or server error' };
    }
}

function setStatus(elementId, ok, message) {
    const node = document.getElementById(elementId);
    if (!node) return;
    node.textContent = message;
    node.classList.add('show');
    node.style.backgroundColor = ok ? '#ecfdf5' : '#fef2f2';
    node.style.color = ok ? '#065f46' : '#991b1b';
    node.style.border = `1px solid ${ok ? '#10b981' : '#f87171'}`;
    
    setTimeout(() => {
        node.classList.remove('show');
    }, 5000);
}

function openEditModal(id) {
    const product = managerProducts.find(p => p.id === id);
    if (!product) return;

    document.getElementById('editProductId').value = product.id;
    document.getElementById('editProductName').value = product.name;
    document.getElementById('editProductDescription').value = product.description || '';
    document.getElementById('editProductBasePrice').value = product.basePrice;
    document.getElementById('editProductCategoryId').value = product.categoryId;
    document.getElementById('editProductIsSpecialOffer').checked = product.isSpecialOffer;
    document.getElementById('editProductIsDailyRecipe').checked = product.isDailyRecipe;
    document.getElementById('editProductDiscountPrice').value = product.discountPrice !== null && product.discountPrice !== undefined ? product.discountPrice : '';

    const modal = document.getElementById('editProductModal');
    if (modal) modal.style.display = 'flex';
}

function closeEditModal() {
    const modal = document.getElementById('editProductModal');
    if (modal) modal.style.display = 'none';
    const form = document.getElementById('editProductForm');
    if (form) form.reset();
}

async function onEditProduct(e) {
    e.preventDefault();
    const form = e.target;
    const id = Number(form.id.value);
    const payload = {
        name: form.name.value.trim(),
        description: form.description.value.trim() || null,
        basePrice: Number(form.basePrice.value),
        categoryId: Number(form.categoryId.value),
        isSpecialOffer: form.isSpecialOffer.checked,
        isDailyRecipe: form.isDailyRecipe.checked,
        discountPrice: form.discountPrice.value ? Number(form.discountPrice.value) : null
    };

    const result = await apiRequest(`/api/v1/products/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });

    if (result.ok) {
        closeEditModal();
        await loadProducts();
    } else {
        alert('Failed to edit product: ' + result.message);
    }
}

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}
