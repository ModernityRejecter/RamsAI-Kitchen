// auth.js
const getStorage = () => localStorage.getItem('token') ? localStorage : sessionStorage;

let soundEnabled = true;
let audioCtx = null;
let kitchenClient = null;
let orderClient = null;

function checkAuth() {
    const storage = getStorage();
    const token = storage.getItem('token');
    const username = storage.getItem('username');
    const role = storage.getItem('role');

    if (!token && !window.location.pathname.endsWith('login.html') && !window.location.pathname.endsWith('register.html') && !window.location.pathname.endsWith('index.html') && !window.location.pathname.endsWith('menu.html')) {
        window.location.href = 'login.html';
        return;
    }

    if (token) {
        updateUIForAuthenticatedUser(username, role);
        const userRole = localStorage.getItem('role') || sessionStorage.getItem('role');
        if (userRole === 'CHEF' || userRole === 'MANAGER') {
            initKitchenNotifications();
        }
        if (userRole === 'CUSTOMER' || userRole === 'WAITER' || userRole === 'MANAGER') {
            initOrderNotifications();
        }
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
            ${role === 'CHEF' || role === 'MANAGER' ? `<li><a href="manager.html">${role === 'MANAGER' ? 'Manager' : 'Console'}</a></li>` : ''}
            ${role === 'CHEF' || role === 'MANAGER' ? '<li><a href="kitchen.html" id="navKitchen">Kitchen</a></li>' : ''}
            ${role === 'CUSTOMER' || role === 'WAITER' || role === 'MANAGER' ? '<li><a href="tables.html">Tables</a></li>' : ''}
            ${role === 'CUSTOMER' || role === 'WAITER' || role === 'MANAGER' ? '<li><a href="my-orders.html" id="navMyOrders">My Orders</a></li>' : ''}
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
        applyNavNotificationDots();
    }
}

// Notification dots on the nav. A notification "comes from" a page (kitchen / my-orders);
// we flag its nav link with a dot unless the user is already viewing that page. The flag is
// kept in sessionStorage so it survives navigation, and is cleared once that page is opened.
const NAV_NOTIF_PAGES = { kitchen: 'navKitchen', 'my-orders': 'navMyOrders' };

function markNavNotification(page) {
    if (window.location.pathname.endsWith(`${page}.html`)) return;
    sessionStorage.setItem(`navNotif_${page}`, '1');
    setNavDot(page, true);
}

function applyNavNotificationDots() {
    Object.keys(NAV_NOTIF_PAGES).forEach(page => {
        if (window.location.pathname.endsWith(`${page}.html`)) {
            sessionStorage.removeItem(`navNotif_${page}`);
            setNavDot(page, false);
        } else if (sessionStorage.getItem(`navNotif_${page}`) === '1') {
            setNavDot(page, true);
        }
    });
}

function setNavDot(page, on) {
    const link = document.getElementById(NAV_NOTIF_PAGES[page]);
    if (link) link.classList.toggle('has-notif', on);
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

function ensureAudio() {
    if (!audioCtx) {
        try { audioCtx = new (window.AudioContext || window.webkitAudioContext)(); } catch (e) { return; }
    }
    if (audioCtx.state === 'suspended') audioCtx.resume();
}

function playKitchenAlert() {
    if (!soundEnabled) return;
    ensureAudio();
    if (!audioCtx) return;
    try {
        [[880, 0], [1100, 0.22]].forEach(([freq, delay]) => {
            const osc = audioCtx.createOscillator();
            const gain = audioCtx.createGain();
            osc.connect(gain);
            gain.connect(audioCtx.destination);
            osc.type = 'sine';
            osc.frequency.value = freq;
            const t = audioCtx.currentTime + delay;
            gain.gain.setValueAtTime(0.22, t);
            gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.35);
            osc.start(t);
            osc.stop(t + 0.35);
        });
    } catch (e) { /* ignore */ }
}

function playOrderAlert() {
    if (!soundEnabled) return;
    ensureAudio();
    if (!audioCtx) return;
    try {
        [[1047, 0], [880, 0.22]].forEach(([freq, delay]) => {
            const osc = audioCtx.createOscillator();
            const gain = audioCtx.createGain();
            osc.connect(gain);
            gain.connect(audioCtx.destination);
            osc.type = 'sine';
            osc.frequency.value = freq;
            const t = audioCtx.currentTime + delay;
            gain.gain.setValueAtTime(0.18, t);
            gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.4);
            osc.start(t);
            osc.stop(t + 0.4);
        });
    } catch (e) { /* ignore */ }
}

function toggleGlobalSound() {
    soundEnabled = !soundEnabled;
    localStorage.setItem('soundEnabled', soundEnabled);
}

function initKitchenNotifications() {
    if (kitchenClient) return;
    kitchenClient = createRealtimeClient({
        onConnect: (client) => {
            client.subscribe('/topic/kitchen', (msg) => {
                try {
                    const order = JSON.parse(msg.body);
                    if (order && order.id != null && order.status === 'RECEIVED') {
                        playKitchenAlert();
                        markNavNotification('kitchen');
                        window.dispatchEvent(new CustomEvent('kitchenOrderArrived', { detail: order }));
                    }
                } catch (e) { /* ignore */ }
            });
        },
        onDisconnect: () => { kitchenClient = null; },
        onError: () => { kitchenClient = null; }
    });
}

async function initOrderNotifications() {
    if (orderClient) return;
    // The server pushes each of a customer's order updates to /topic/customers/{id}/orders.
    const userId = await fetchCurrentUserId();
    if (userId == null) return;
    const knownOrderStatuses = new Map();
    orderClient = createRealtimeClient({
        onConnect: (client) => {
            client.subscribe(`/topic/customers/${userId}/orders`, (msg) => {
                try {
                    const order = JSON.parse(msg.body);
                    if (order && order.id != null) {
                        const prev = knownOrderStatuses.get(order.id);
                        knownOrderStatuses.set(order.id, order.status);
                        // The my-orders page does its own per-order live tracking (sound + toast),
                        // so only fire the global alert/dot when the customer is on another page.
                        if (order.status === 'SERVED' && prev !== 'SERVED'
                                && !window.location.pathname.endsWith('my-orders.html')) {
                            playOrderAlert();
                            markNavNotification('my-orders');
                            window.dispatchEvent(new CustomEvent('orderServed', { detail: order }));
                        }
                    }
                } catch (e) { /* ignore */ }
            });
        },
        onDisconnect: () => { orderClient = null; },
        onError: () => { orderClient = null; }
    });
}

async function fetchCurrentUserId() {
    try {
        const res = await authenticatedFetch('/api/v1/user/me');
        if (!res.ok) return null;
        const json = await res.json();
        return json.data && json.data.id != null ? json.data.id : null;
    } catch (e) {
        return null;
    }
}

document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    soundEnabled = localStorage.getItem('soundEnabled') !== 'false';
    document.addEventListener('click', ensureAudio);
});
