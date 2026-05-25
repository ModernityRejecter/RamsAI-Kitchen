// auth.js
function checkAuth() {
    const token = localStorage.getItem('token');
    const username = localStorage.getItem('username');
    const role = localStorage.getItem('role');

    if (!token && !window.location.pathname.endsWith('login.html') && !window.location.pathname.endsWith('register.html') && !window.location.pathname.endsWith('index.html')) {
        window.location.href = 'login.html';
        return;
    }

    if (token) {
        updateUIForAuthenticatedUser(username, role);
    }

    setupLiveValidation();
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
            document.getElementById('usernameError').textContent = 
                usernameInput.value.trim() === '' ? 'Username is required' : '';
        });

        passwordInput.addEventListener('input', () => {
            document.getElementById('passwordError').textContent = 
                passwordInput.value.trim() === '' ? 'Password is required' : '';
        });
    }
}

function updateUIForAuthenticatedUser(username, role) {
    const navUl = document.querySelector('nav ul');
    if (navUl) {
        navUl.innerHTML = `
            <li><a href="index.html">Home</a></li>
            ${role === 'MANAGER' ? '<li><a href="manager.html">Manager</a></li>' : ''}
            ${role === 'CHEF' || role === 'MANAGER' ? '<li><a href="kitchen.html">Kitchen</a></li>' : ''}
            ${role === 'WAITER' || role === 'MANAGER' ? '<li><a href="tables.html">Tables</a></li>' : ''}
            <li><a href="#" id="logoutBtn">Logout (${username})</a></li>
        `;

        document.getElementById('logoutBtn').addEventListener('click', (e) => {
            e.preventDefault();
            localStorage.clear();
            window.location.href = 'index.html';
        });
    }
}

document.addEventListener('DOMContentLoaded', checkAuth);
