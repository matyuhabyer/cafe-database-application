/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/JavaScript.js to edit this template
 */

// Get the context path - same method as test-servlets.html
// Only declare if not already declared (to avoid conflicts with inline scripts)
if (typeof window.BASE_URL === 'undefined') {
    window.BASE_URL = window.location.origin + window.location.pathname.replace(/\/[^/]*$/, '');
    window.API_BASE = window.BASE_URL + '/api';
}
const BASE_URL = window.BASE_URL;
const API_BASE = window.API_BASE;
console.log('Header.js - Base URL:', BASE_URL, 'API Base:', API_BASE);

// Check authentication status
async function checkAuth() {
    try {
        // Check if session exists (you may need to implement a session check endpoint)
        const role = localStorage.getItem("role") || "guest";
        const userRoleSpan = document.getElementById("userRole");
        const adminLink = document.getElementById("adminLink");
        const logoutBtn = document.getElementById("logoutBtn");
        
        if (userRoleSpan) {
            userRoleSpan.textContent = role.charAt(0).toUpperCase() + role.slice(1);
        }
        
        if (adminLink) {
            adminLink.style.display = (role === "admin" || role === "manager") ? "inline-block" : "none";
        }
        
        if (logoutBtn) {
            logoutBtn.style.display = (role !== "guest") ? "inline-block" : "none";
        }
    } catch (error) {
        console.error('Auth check error:', error);
    }
}

// Logout handler
async function handleLogout() {
    try {
        const response = await fetch(`${API_BASE}/auth/logout`, {
            method: 'POST',
            credentials: 'include'
        });
        
        // Clear local storage
        localStorage.removeItem("role");
        localStorage.removeItem("cart");
        localStorage.removeItem("user");
        
        // Redirect to login
        window.location.href = "index.jsp";
    } catch (error) {
        console.error('Logout error:', error);
        // Still clear local storage and redirect
        localStorage.clear();
        window.location.href = "index.jsp";
    }
}

// Initialize
const logoutBtn = document.getElementById("logoutBtn");
if (logoutBtn) {
    logoutBtn.addEventListener("click", handleLogout);
}

checkAuth();





