document.addEventListener('DOMContentLoaded', () => {
    loadCategories();
    loadDailySpecials();
    loadRecommended();
    loadProducts();
});

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
    
    const dailyBadge = product.isDailyRecipe 
        ? `<div class="daily-badge">Daily Special</div>` 
        : '';

    const priceHtml = product.isSpecialOffer 
        ? `<span class="original-price">$${product.basePrice.toFixed(2)}</span> <span class="discount-price">$${product.discountPrice.toFixed(2)}</span>`
        : `<span class="price">$${product.basePrice.toFixed(2)}</span>`;

    card.innerHTML = `
        ${dailyBadge}
        <div class="menu-card-content">
            <div class="menu-card-header">
                <h3>${product.name}</h3>
                <div class="rating">
                    ${getRatingStars(product.averageRating)}
                </div>
            </div>
            <p class="description">${product.description || 'No description available.'}</p>
            <div class="menu-card-footer">
                <div class="price-tag">${priceHtml}</div>
                <button class="add-btn"><i class="fas fa-plus"></i> Add</button>
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

// Global filter for "All"
document.querySelector('[data-category="all"]').onclick = (e) => {
    updateActiveFilter(e.target);
    loadProducts();
};
