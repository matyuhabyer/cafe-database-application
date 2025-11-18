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
// Use window properties directly to avoid any redeclaration issues
// Access via window to avoid variable conflicts
console.log('Header.js - Base URL:', window.BASE_URL, 'API Base:', window.API_BASE);

const ROLE_NAV_MAP = {
    guest: "navGuest",
    customer: "navCustomer",
    staff: "navStaff",
    manager: "navManager",
    admin: "navAdmin"
};

function updateRoleNav(role) {
    Object.values(ROLE_NAV_MAP).forEach(navId => {
        const nav = document.getElementById(navId);
        if (nav) {
            nav.style.display = "none";
        }
    });
    
    const targetNavId = ROLE_NAV_MAP[role] || ROLE_NAV_MAP.guest;
    const targetNav = document.getElementById(targetNavId);
    if (targetNav) {
        targetNav.style.display = "flex";
    }
}

// Check authentication status
async function checkAuth() {
    try {
        const role = localStorage.getItem("role") || "guest";
        const normalizedRole = ROLE_NAV_MAP[role] ? role : "guest";
        const userRoleSpan = document.getElementById("userRole");
        const logoutBtn = document.getElementById("logoutBtn");
        
        console.log('checkAuth called, role:', role, 'normalizedRole:', normalizedRole);
        
        updateRoleNav(normalizedRole);
        
        if (userRoleSpan) {
            userRoleSpan.textContent = normalizedRole === "guest"
                ? "Guest"
                : normalizedRole.charAt(0).toUpperCase() + normalizedRole.slice(1);
            userRoleSpan.style.display = "inline-block";
        } else {
            console.warn('userRoleSpan not found!');
        }
        
        if (logoutBtn) {
            const shouldShow = (normalizedRole !== "guest");
            logoutBtn.style.display = shouldShow ? "inline-block" : "none";
            logoutBtn.style.visibility = shouldShow ? "visible" : "hidden";
            console.log('Logout button display:', shouldShow ? 'shown' : 'hidden', 'role:', normalizedRole);
        } else {
            console.error('logoutBtn not found! This is a problem.');
        }
    } catch (error) {
        console.error('Auth check error:', error);
    }
}

// Make checkAuth available globally so it can be called after page load
window.checkAuth = checkAuth;

// Logout handler
async function handleLogout() {
    try {
        const response = await fetch(`${window.API_BASE}/auth/logout`, {
            method: 'POST',
            credentials: 'include'
        });
        
        // Clear local storage
        localStorage.removeItem("role");
        localStorage.removeItem("cart");
        localStorage.removeItem("user");
        
        // Redirect to login
        window.location.href = "index.html";
    } catch (error) {
        console.error('Logout error:', error);
        // Still clear local storage and redirect
        localStorage.clear();
        window.location.href = "index.html";
    }
}

// Make handleLogout available globally
window.handleLogout = handleLogout;

// Initialize when DOM is ready
function initializeHeader() {
    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) {
        // Remove any existing listeners
        const newLogoutBtn = logoutBtn.cloneNode(true);
        logoutBtn.parentNode.replaceChild(newLogoutBtn, logoutBtn);
        // Add event listener
        newLogoutBtn.addEventListener("click", function(e) {
            e.preventDefault();
            e.stopPropagation();
            console.log('Logout button clicked from initializeHeader');
            handleLogout();
        });
        console.log('Logout button event listener attached from initializeHeader');
    }
    
    // Check auth status
    checkAuth();
    
    // Listen for storage changes (when role is updated in another tab/window)
    window.addEventListener('storage', function(e) {
        if (e.key === 'role') {
            checkAuth();
        }
    });
    
    // Also check when page becomes visible (user navigates back)
    document.addEventListener('visibilitychange', function() {
        if (!document.hidden) {
            checkAuth();
        }
    });
    
    // Check periodically for role changes (in case user logs in on another page)
    setInterval(function() {
        const currentRole = localStorage.getItem("role") || "guest";
        const userRoleSpan = document.getElementById("userRole");
        if (userRoleSpan) {
            const displayedRole = userRoleSpan.textContent.toLowerCase();
            if (currentRole !== displayedRole && currentRole !== "guest") {
                checkAuth();
            }
        }
    }, 1000);
}

// Run initialization when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeHeader);
} else {
    // DOM is already ready
    initializeHeader();
}

// Also check auth after a short delay to ensure header is loaded
// Use multiple timeouts to catch different loading scenarios
setTimeout(checkAuth, 100);
setTimeout(checkAuth, 300);
setTimeout(checkAuth, 500);
setTimeout(checkAuth, 1000);
setTimeout(checkAuth, 2000);

// Also check when window loads completely
window.addEventListener('load', function() {
    console.log('Window loaded, checking auth');
    setTimeout(checkAuth, 100);
    
    // Also ensure logout button has event listener
    setTimeout(() => {
        const logoutBtn = document.getElementById("logoutBtn");
        if (logoutBtn && typeof handleLogout === 'function') {
            // Check if it already has a listener by checking if it's been cloned
            if (!logoutBtn.hasAttribute('data-listener-attached')) {
                const newLogoutBtn = logoutBtn.cloneNode(true);
                logoutBtn.parentNode.replaceChild(newLogoutBtn, logoutBtn);
                newLogoutBtn.setAttribute('data-listener-attached', 'true');
                newLogoutBtn.addEventListener("click", function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    console.log('Logout button clicked from window load');
                    handleLogout();
                });
                console.log('Logout button event listener attached from window load');
            }
        }
    }, 200);
});





