document.addEventListener('DOMContentLoaded', () => {
    loadCategories();
    loadDailySpecials();
    loadRecommended();
    loadProducts();
    loadCart();
    loadTables();
    setupCartUI();
});

function setupCartUI() {
    const sideCartTrigger = document.getElementById('sideCartTrigger');
    const closeCart = document.getElementById('closeCart');
    const cartSidebar = document.getElementById('cartSidebar');
    const checkoutBtn = document.getElementById('checkoutBtn');

    if (sideCartTrigger) {
        sideCartTrigger.addEventListener('click', () => {
            cartSidebar.classList.toggle('open');
        });
    }

    if (closeCart) {
        closeCart.addEventListener('click', () => {
            cartSidebar.classList.remove('open');
        });
    }

    if (checkoutBtn) {
        checkoutBtn.addEventListener('click', checkout);
    }
}

async function loadCart() {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    if (!token) return;

    try {
        const response = await fetch('/api/v1/cart', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const result = await response.json();
        
        if (response.ok) {
            renderCart(result.data);
        }
    } catch (error) {
        console.error('Error loading cart:', error);
    }
}

function renderCart(cart) {
    const cartItems = document.getElementById('cartItems');
    const cartCount = document.getElementById('cartCount');
    const cartTotalPrice = document.getElementById('cartTotalPrice');
    
    cartItems.innerHTML = '';
    cartCount.textContent = cart.items.length;
    cartTotalPrice.textContent = `$${cart.totalPrice.toFixed(2)}`;

    if (cart.items.length === 0) {
        cartItems.innerHTML = '<div class="empty-cart-msg">Your cart is empty.</div>';
        return;
    }

    cart.items.forEach(item => {
        const itemDiv = document.createElement('div');
        itemDiv.className = 'cart-item';
        itemDiv.innerHTML = `
            <div class="cart-item-info">
                <h4>${item.productName}</h4>
                <p>${item.quantity} x $${item.unitPrice.toFixed(2)}</p>
            </div>
            <div class="cart-item-remove" onclick="removeFromCart(${item.id})">
                <i class="fas fa-trash"></i>
            </div>
        `;
        cartItems.appendChild(itemDiv);
    });
}

async function addToCart(productId) {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    if (!token) {
        alert('Please login to add items to your cart.');
        window.location.href = 'login.html';
        return;
    }

    try {
        const response = await fetch('/api/v1/cart/items', {
            method: 'POST',
            headers: { 
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ productId, quantity: 1 })
        });
        
        if (response.ok) {
            const result = await response.json();
            renderCart(result.data);
            document.getElementById('cartSidebar').classList.add('open');
        } else {
            const error = await response.json();
            alert(error.message || 'Failed to add item to cart');
        }
    } catch (error) {
        console.error('Error adding to cart:', error);
    }
}

async function removeFromCart(itemId) {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    try {
        const response = await fetch(`/api/v1/cart/items/${itemId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            const result = await response.json();
            renderCart(result.data);
        }
    } catch (error) {
        console.error('Error removing from cart:', error);
    }
}

async function checkout() {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const tableId = document.getElementById('tableId').value;

    if (!tableId) {
        alert('Please select a table before checking out.');
        return;
    }

    try {
        const response = await fetch(`/api/v1/cart/checkout?tableId=${tableId}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.ok) {
            sessionStorage.removeItem('selectedTableId');
            sessionStorage.removeItem('selectedTableNumber');
            alert('Order placed successfully! Redirecting...');
            window.location.href = 'my-orders.html';
        } else {
            const error = await response.json();
            alert(error.message || 'Checkout failed');
        }
    } catch (error) {
        console.error('Checkout error:', error);
    }
}

async function loadTables() {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const tableSelect = document.getElementById('tableId');
    const label = document.getElementById('tableSelectLabel');
    const preSelectedId = sessionStorage.getItem('selectedTableId');
    const preSelectedNumber = sessionStorage.getItem('selectedTableNumber');

    let currentUserId = null;
    try {
        const headers = token ? { 'Authorization': `Bearer ${token}` } : {};
        const authResp = await fetch('/api/v1/user/me', { headers });
        if (authResp.ok) {
            const authData = await authResp.json();
            currentUserId = authData.data.id;
        }
    } catch (e) {}

    try {
        const headers = token ? { 'Authorization': `Bearer ${token}` } : {};
        const response = await fetch('/api/v1/tables/map', { headers });
        const result = await response.json();

        if (!response.ok) throw new Error('Failed to load tables');

        // Group squares by tableNumber, keep the entry with the smallest id per group
        const groups = {};
        result.data.forEach(t => {
            const n = t.tableNumber;
            if (!groups[n] || t.id < groups[n].id) groups[n] = t;
        });

        tableSelect.innerHTML = '<option value="">-- Select a table --</option>';
        Object.values(groups)
            .sort((a, b) => a.tableNumber - b.tableNumber)
            .forEach(t => {
                const opt = document.createElement('option');
                opt.value = t.id;
                
                const isSelfOccupied = t.status === 'OCCUPIED' && currentUserId != null && t.occupiedByUserId != null && Number(t.occupiedByUserId) === Number(currentUserId);
                const statusLabel = isSelfOccupied ? 'Occupied by you' : (t.status === 'FREE' ? 'Available' : 'Occupied');
                
                opt.textContent = `Table ${t.tableNumber} — ${statusLabel}`;
                
                if (t.status === 'OCCUPIED' && !isSelfOccupied) {
                    opt.disabled = true;
                }
                
                if ((preSelectedId && t.id.toString() === preSelectedId) || isSelfOccupied) {
                    opt.selected = true;
                    opt.disabled = false;
                }
                
                tableSelect.appendChild(opt);
            });

        if (preSelectedNumber) {
            label.innerHTML = `Table: <strong style="color:#27ae60">Table ${preSelectedNumber} pre-selected</strong>`;
        }
    } catch {
        tableSelect.innerHTML = '<option value="">Error loading tables</option>';
    }
}

async function loadDailySpecials() {
    await fetchAndRender('/api/v1/products/daily', 'daily-grid');
}

async function loadRecommended() {
    await fetchAndRender('/api/v1/products/recommended', 'recommended-grid');
}

async function fetchAndRender(url, targetId) {
    const grid = document.getElementById(targetId);
    try {
        const response = await fetch(url);
        const result = await response.json();
        
        if (response.ok) {
            grid.innerHTML = '';
            if (result.data.length === 0) {
                document.getElementById(targetId + '-section').style.display = 'none';
                return;
            }
            
            result.data.forEach(product => {
                grid.appendChild(createProductCard(product));
            });
        }
    } catch (error) {
        console.error(`Error loading ${targetId}:`, error);
        grid.innerHTML = '<div class="error">Failed to load this section.</div>';
    }
}

async function loadCategories() {
    try {
        const response = await fetch('/api/v1/products/categories');
        const result = await response.json();
        
        if (response.ok) {
            const categoryFilters = document.getElementById('category-filters');
            result.data.forEach(category => {
                const btn = document.createElement('button');
                btn.className = 'filter-btn';
                btn.textContent = category.name;
                btn.dataset.category = category.id;
                btn.onclick = () => {
                    updateActiveFilter(btn);
                    loadProducts(category.id);
                };
                categoryFilters.appendChild(btn);
            });
        }
    } catch (error) {
        console.error('Error loading categories:', error);
    }
}

async function loadProducts(categoryId = null) {
    const menuGrid = document.getElementById('menu-grid');
    menuGrid.innerHTML = '<div class="loading">Loading...</div>';

    try {
        let url = '/api/v1/products';
        if (categoryId) {
            url += `?categoryId=${categoryId}`;
        }
        
        const response = await fetch(url);
        const result = await response.json();
        
        if (response.ok) {
            menuGrid.innerHTML = '';
            if (result.data.length === 0) {
                menuGrid.innerHTML = '<div class="no-items">No items found in this category.</div>';
                return;
            }
            
            result.data.forEach(product => {
                const card = createProductCard(product);
                menuGrid.appendChild(card);
            });
        }
    } catch (error) {
        console.error('Error loading products:', error);
        menuGrid.innerHTML = '<div class="error">Failed to load menu. Please try again later.</div>';
    }
}

function createProductCard(product) {
    const card = document.createElement('div');
    card.className = 'menu-card';
    card.style.position = 'relative';

    if (!product.isActive) {
        card.style.opacity = '0.7';
        card.style.filter = 'grayscale(0.5)';
    }
    
    const dailyBadge = product.isDailyRecipe 
        ? `<div class="daily-badge">Daily Special</div>` 
        : '';

    const priceHtml = product.isSpecialOffer 
        ? `<span class="original-price">$${product.basePrice.toFixed(2)}</span> <span class="discount-price">$${product.discountPrice.toFixed(2)}</span>`
        : `<span class="price">$${product.basePrice.toFixed(2)}</span>`;

    const statusOverlay = !product.isActive 
        ? `<div style="position:absolute; top:50%; left:50%; transform:translate(-50%,-50%); background:rgba(0,0,0,0.7); color:white; padding:5px 15px; border-radius:20px; z-index:2; font-weight:bold; text-transform:uppercase;">Unavailable</div>` 
        : '';

    const addBtn = product.isActive 
        ? `<button class="add-btn" onclick="addToCart(${product.id})"><i class="fas fa-plus"></i> Add</button>`
        : `<button class="add-btn" disabled style="background:#ccc; cursor:not-allowed;"><i class="fas fa-ban"></i> Add</button>`;

    card.innerHTML = `
        ${dailyBadge}
        ${statusOverlay}
        <div class="menu-card-content">
            <div class="menu-card-header">
                <h3>${product.name}</h3>
                <div class="rating" style="cursor: pointer;" onclick="showProductReviewsModal(${product.id}, '${escapeJS(product.name)}', ${product.averageRating})">
                    ${getRatingStars(product.averageRating)}
                </div>
            </div>
            <p class="description">${product.description || 'No description available.'}</p>
            <div class="menu-card-footer">
                <div class="price-tag">${priceHtml}</div>
                ${addBtn}
            </div>
        </div>
    `;
    
    return card;
}

function getRatingStars(rating) {
    let stars = '';
    for (let i = 1; i <= 5; i++) {
        if (i <= Math.floor(rating)) {
            stars += '<i class="fas fa-star"></i>';
        } else if (i - 0.5 <= rating) {
            stars += '<i class="fas fa-star-half-alt"></i>';
        } else {
            stars += '<i class="far fa-star"></i>';
        }
    }
    return stars;
}

function updateActiveFilter(clickedBtn) {
    document.querySelectorAll('.filter-btn').forEach(btn => btn.classList.remove('active'));
    clickedBtn.classList.add('active');
}

async function showProductReviewsModal(productId, productName, averageRating) {
    let modal = document.getElementById('productReviewsModal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'productReviewsModal';
        modal.className = 'modal-overlay';
        document.body.appendChild(modal);
    }

    modal.innerHTML = `
        <div class="modal-content reviews-display-modal">
            <span class="close-modal" onclick="closeProductReviewsModal()">&times;</span>
            <h2>Reviews for ${productName}</h2>
            <div class="reviews-summary">
                <div class="rating-big">
                    ${getRatingStars(averageRating)}
                </div>
                <span>${averageRating.toFixed(1)} average rating</span>
            </div>
            <div id="reviewsList" class="reviews-list">
                <p>Loading reviews...</p>
            </div>
        </div>
    `;
    modal.style.display = 'flex';

    try {
        const response = await fetch(`/api/v1/products/${productId}/reviews`);
        const result = await response.json();
        const reviewsList = document.getElementById('reviewsList');

        if (response.ok && result.data.length > 0) {
            reviewsList.innerHTML = result.data.map(review => `
                <div class="review-item">
                    <div class="review-header">
                        <strong>${review.customerName}</strong>
                        <div class="rating-small">${getRatingStars(review.rating)}</div>
                    </div>
                    <p class="review-comment">${review.comment || '<em>No comment provided.</em>'}</p>
                    <small class="review-date">${new Date(review.createdAt).toLocaleDateString()}</small>
                </div>
            `).join('');
        } else {
            reviewsList.innerHTML = '<p>No reviews yet. Be the first to review this product!</p>';
        }
    } catch (error) {
        document.getElementById('reviewsList').innerHTML = '<p>Could not load reviews.</p>';
        console.error('Error fetching reviews:', error);
    }
}

function closeProductReviewsModal() {
    let modal = document.getElementById('productReviewsModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

function escapeJS(s) {
    if (!s) return '';
    return String(s)
        .replace(/\\/g, '\\\\')
        .replace(/'/g, "\\'")
        .replace(/"/g, '\\"')
        .replace(/\n/g, '\\n')
        .replace(/\r/g, '\\r');
}

// Global filter for "All"
document.querySelector('[data-category="all"]').onclick = (e) => {
    updateActiveFilter(e.target);
    loadProducts();
};
