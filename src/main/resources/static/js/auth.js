// auth.js
const getStorage = () => localStorage.getItem('token') ? localStorage : sessionStorage;

function checkAuth() {
    const storage = getStorage();
    const token = storage.getItem('token');
    const username = storage.getItem('username');
    const role = storage.getItem('role');

    if (!token && !window.location.pathname.endsWith('login.html') && !window.location.pathname.endsWith('register.html') && !window.location.pathname.endsWith('index.html')) {
        window.location.href = 'login.html';
        return;
    }

    if (token) {
        updateUIForAuthenticatedUser(username, role);
    }

    setupLiveValidation();
}

async function authenticatedFetch(url, options = {}) {
    const storage = getStorage();
    let token = storage.getItem('token');

    if (!options.headers) options.headers = {};
    options.headers['Authorization'] = `Bearer ${token}`;

    let response = await fetch(url, options);

    if (response.status === 401) {
        const refreshToken = storage.getItem('refreshToken');
        if (refreshToken) {
            const refreshResponse = await fetch('/api/v1/auth/refresh', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken })
            });

            if (refreshResponse.ok) {
                const result = await refreshResponse.json();
                storage.setItem('token', result.data.token);
                options.headers['Authorization'] = `Bearer ${result.data.token}`;
                return fetch(url, options);
            }
        }
        localStorage.clear();
        sessionStorage.clear();
        window.location.href = 'login.html';
    }

    return response;
}

function setupLiveValidation() {
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        const usernameInput = registerForm.username;
        const emailInput = registerForm.email;
        const passwordInput = registerForm.password;

        usernameInput.addEventListener('input', () => {
            const errorDiv = document.getElementById('usernameError');
            if (usernameInput.value.length < 3) {
                errorDiv.textContent = 'Minimum 3 characters required';
            } else {
                errorDiv.textContent = '';
            }
        });

        emailInput.addEventListener('input', () => {
            const errorDiv = document.getElementById('emailError');
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(emailInput.value)) {
                errorDiv.textContent = 'Invalid email format';
            } else {
                errorDiv.textContent = '';
            }
        });

        passwordInput.addEventListener('input', () => {
            const errorDiv = document.getElementById('passwordError');
            const passwordRegex = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$/;
            if (passwordInput.value.length < 8) {
                errorDiv.textContent = 'Minimum 8 characters required';
            } else if (!/(?=.*[0-9])/.test(passwordInput.value)) {
                errorDiv.textContent = 'Must contain at least one digit';
            } else if (!/(?=.*[a-z])/.test(passwordInput.value)) {
                errorDiv.textContent = 'Must contain at least one lowercase letter';
            } else if (!/(?=.*[A-Z])/.test(passwordInput.value)) {
                errorDiv.textContent = 'Must contain at least one uppercase letter';
            } else if (!/(?=.*[@#$%^&+=!])/.test(passwordInput.value)) {
                errorDiv.textContent = 'Must contain at least one special character';
            } else {
                errorDiv.textContent = '';
            }
        });
    }

    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        const usernameInput = loginForm.username;
        const passwordInput = loginForm.password;

        usernameInput.addEventListener('input', () => {
            const errorDiv = document.getElementById('usernameError');
            if (errorDiv) {
                errorDiv.textContent = usernameInput.value.trim() === '' ? 'Username is required' : '';
            }
        });

        passwordInput.addEventListener('input', () => {
            const errorDiv = document.getElementById('passwordError');
            if (errorDiv) {
                errorDiv.textContent = passwordInput.value.trim() === '' ? 'Password is required' : '';
            }
        });
    }
}

function updateUIForAuthenticatedUser(username, role) {
    const navUl = document.querySelector('nav ul');
    if (navUl) {
        navUl.innerHTML = `
            <li><a href="index.html">Home</a></li>
            <li><a href="menu.html">Menu</a></li>
            <li><a href="profile.html">Profile</a></li>
            ${role === 'MANAGER' ? '<li><a href="audit.html">Audit Logs</a></li>' : ''}
            ${role === 'MANAGER' ? '<li><a href="manager.html">Manager</a></li>' : ''}
            ${role === 'CHEF' || role === 'MANAGER' ? '<li><a href="kitchen.html">Kitchen</a></li>' : ''}
            ${role === 'CUSTOMER' || role === 'WAITER' || role === 'MANAGER' ? '<li><a href="tables.html">Tables</a></li>' : ''}
            <li><a href="#" id="logoutBtn">Logout (${username})</a></li>
            <li><a href="order.html" id="cartLink"><i class="fas fa-shopping-cart"></i> <span id="cartCount">0</span></a></li>
        `;

        document.getElementById('logoutBtn').addEventListener('click', (e) => {
            e.preventDefault();
            localStorage.clear();
            sessionStorage.clear();
            window.location.href = 'index.html';
        });

        updateCartCount();
    }
}

async function updateCartCount() {
    const cartCount = document.getElementById('cartCount');
    if (!cartCount) return;

    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    if (!token) return;

    try {
        const response = await fetch('/api/v1/cart', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (response.ok) {
            const result = await response.json();
            cartCount.textContent = result.data.items.length;
        }
    } catch (e) {
        console.error('Error updating cart count:', e);
    }
}

document.addEventListener('DOMContentLoaded', checkAuth);
