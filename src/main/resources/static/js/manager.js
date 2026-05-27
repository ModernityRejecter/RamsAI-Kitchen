let managerProducts = [];
let managerIngredients = [];

document.addEventListener('DOMContentLoaded', async () => {
    if (!enforceManagerRole()) return;
    await bootstrapManagerPage();
    registerHandlers();
});

function enforceManagerRole() {
    const role = localStorage.getItem('role') || sessionStorage.getItem('role');
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    if (!token) {
        window.location.href = 'login.html';
        return false;
    }
    if (role !== 'MANAGER') {
        alert('Only managers can access this page.');
        window.location.href = 'index.html';
        return false;
    }
    return true;
}

async function bootstrapManagerPage() {
    await Promise.all([
        loadCategories(),
        loadProducts(),
        loadIngredients(),
        loadLowStock(),
        loadInventoryLogs()
    ]);
}

function registerHandlers() {
    document.getElementById('createProductForm').addEventListener('submit', onCreateProduct);
    document.getElementById('createIngredientForm').addEventListener('submit', onCreateIngredient);
    document.getElementById('addBomForm').addEventListener('submit', onAddBomRow);
    document.getElementById('adjustStockForm').addEventListener('submit', onAdjustStock);
    document.getElementById('bomProductId').addEventListener('change', () => {
        const productId = Number(document.getElementById('bomProductId').value);
        if (productId) loadBomRows(productId);
    });
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
    select.innerHTML = '';
    if (!result.ok) return;

    result.data.forEach(category => {
        const option = document.createElement('option');
        option.value = category.id;
        option.textContent = category.name;
        select.appendChild(option);
    });
}

async function loadProducts() {
    const result = await apiRequest('/api/v1/products/manage');
    if (!result.ok) return;
    managerProducts = result.data || [];

    const bomProduct = document.getElementById('bomProductId');
    bomProduct.innerHTML = managerProducts.map(product =>
        `<option value="${product.id}">${escapeHtml(product.name)}</option>`
    ).join('');

    if (managerProducts.length > 0) {
        await loadBomRows(managerProducts[0].id);
    } else {
        document.getElementById('bomRowsTable').innerHTML = '<tr><td colspan="4">No products found.</td></tr>';
    }
}

async function loadIngredients() {
    const result = await apiRequest('/api/v1/inventory/ingredients');
    if (!result.ok) return;
    managerIngredients = result.data || [];

    const ingredientOptions = managerIngredients.map(ingredient =>
        `<option value="${ingredient.id}">${escapeHtml(ingredient.name)} (${escapeHtml(ingredient.unit)})</option>`
    ).join('');

    document.getElementById('bomIngredientId').innerHTML = ingredientOptions;
    document.getElementById('stockIngredientId').innerHTML = ingredientOptions;
}

async function loadBomRows(productId) {
    const result = await apiRequest(`/api/v1/inventory/products/${productId}/ingredients`);
    const tableBody = document.getElementById('bomRowsTable');
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
    }
}

async function loadLowStock() {
    const result = await apiRequest('/api/v1/inventory/ingredients/low-stock');
    const tableBody = document.getElementById('lowStockTable');
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
    
    // Auto-hide after 5 seconds
    setTimeout(() => {
        node.classList.remove('show');
    }, 5000);
}

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}
