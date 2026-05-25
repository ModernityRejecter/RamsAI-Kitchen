document.addEventListener('DOMContentLoaded', () => {
    loadDetailedCart();
    setupOrderUI();
});

async function loadDetailedCart() {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    if (!token) {
        window.location.href = 'login.html';
        return;
    }

    try {
        const response = await fetch('/api/v1/cart', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const result = await response.json();
        
        if (response.ok) {
            renderDetailedCart(result.data);
        }
    } catch (error) {
        console.error('Error loading detailed cart:', error);
    }
}

function renderDetailedCart(cart) {
    const detailedItems = document.getElementById('detailedCartItems');
    const orderContent = document.getElementById('orderContent');
    const emptyMsg = document.getElementById('emptyOrderMsg');
    const subtotal = document.getElementById('summarySubtotal');
    const total = document.getElementById('summaryTotal');
    
    if (!cart.items || cart.items.length === 0) {
        orderContent.style.display = 'none';
        emptyMsg.style.display = 'block';
        return;
    }

    orderContent.style.display = 'grid';
    emptyMsg.style.display = 'none';
    detailedItems.innerHTML = '';
    
    subtotal.textContent = `$${cart.totalPrice.toFixed(2)}`;
    total.textContent = `$${cart.totalPrice.toFixed(2)}`;

    cart.items.forEach(item => {
        const itemDiv = document.createElement('div');
        itemDiv.className = 'order-item-detailed';
        itemDiv.innerHTML = `
            <div class="item-main">
                <div class="item-info">
                    <h4>${item.productName}</h4>
                    <div class="item-meta">
                        <span>$${item.unitPrice.toFixed(2)} each</span>
                    </div>
                </div>
            </div>
            <div class="item-quantity-controls" style="display: flex; align-items: center; gap: 15px;">
                <span>Qty: ${item.quantity}</span>
                <button class="remove-btn" onclick="removeFromDetailedCart(${item.id})" style="background: none; border: none; color: #e74c3c; cursor: pointer;">
                    <i class="fas fa-trash"></i> Remove
                </button>
            </div>
            <div class="item-price" style="font-weight: 700;">
                $${(item.unitPrice * item.quantity).toFixed(2)}
            </div>
        `;
        detailedItems.appendChild(itemDiv);
    });
}

async function removeFromDetailedCart(itemId) {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    try {
        const response = await fetch(`/api/v1/cart/items/${itemId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            const result = await response.json();
            renderDetailedCart(result.data);
            // Also update the global nav cart count if possible
            if (typeof updateCartCount === 'function') updateCartCount();
        }
    } catch (error) {
        console.error('Error removing from detailed cart:', error);
    }
}

function setupOrderUI() {
    const checkoutBtn = document.getElementById('finalCheckoutBtn');
    if (checkoutBtn) {
        checkoutBtn.addEventListener('click', async () => {
            const token = localStorage.getItem('token') || sessionStorage.getItem('token');
            const tableId = document.getElementById('orderTableId').value;
            const notes = document.getElementById('specialNotes').value;

            // Note: Our current API doesn't take notes in checkout, 
            // but we can extend the cart items with notes if needed.
            
            try {
                const response = await fetch(`/api/v1/cart/checkout?tableId=${tableId}`, {
                    method: 'POST',
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                
                if (response.ok) {
                    alert('Order placed successfully! Redirecting to home...');
                    window.location.href = 'index.html';
                } else {
                    const error = await response.json();
                    alert(error.message || 'Checkout failed');
                }
            } catch (error) {
                console.error('Checkout error:', error);
            }
        });
    }
}
