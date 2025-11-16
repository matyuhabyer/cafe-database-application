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

// Check authentication status
async function checkAuth() {
    try {
        // Check if session exists (you may need to implement a session check endpoint)
        const role = localStorage.getItem("role") || "guest";
        const userRoleSpan = document.getElementById("userRole");
        const adminLink = document.getElementById("adminLink");
        const profileLink = document.getElementById("profileLink");
        const logoutBtn = document.getElementById("logoutBtn");
        
        console.log('checkAuth called, role:', role);
        console.log('profileLink found:', !!profileLink);
        console.log('userRoleSpan found:', !!userRoleSpan);
        console.log('logoutBtn found:', !!logoutBtn);
        
        if (userRoleSpan) {
            if (role === "guest") {
                userRoleSpan.style.display = "none";
            } else {
                userRoleSpan.style.display = "inline-block";
                userRoleSpan.textContent = role.charAt(0).toUpperCase() + role.slice(1);
            }
        } else {
            console.warn('userRoleSpan not found!');
        }
        
        if (adminLink) {
            const shouldShow = (role === "admin" || role === "manager" || role === "staff");
            adminLink.style.display = shouldShow ? "inline-block" : "none";
            console.log('Admin link display:', shouldShow ? 'shown' : 'hidden');
        } else {
            console.warn('adminLink not found!');
        }
        
        if (profileLink) {
            const shouldShow = (role !== "guest");
            if (shouldShow) {
                profileLink.style.display = "inline-block";
                profileLink.style.visibility = "visible";
            } else {
                profileLink.style.display = "none";
            }
            console.log('Profile link display:', shouldShow ? 'shown' : 'hidden', 'role:', role);
        } else {
            console.error('profileLink not found! This is a problem.');
            // Try to find it again with multiple selectors
            let retryProfileLink = document.getElementById("profileLink");
            if (!retryProfileLink) {
                retryProfileLink = document.querySelector('a[href="profile.html"]');
            }
            if (retryProfileLink) {
                console.log('Found profileLink on retry');
                const shouldShow = (role !== "guest");
                if (shouldShow) {
                    retryProfileLink.style.display = "inline-block";
                    retryProfileLink.style.visibility = "visible";
                } else {
                    retryProfileLink.style.display = "none";
                }
            } else {
                console.error('Could not find profileLink even after retry');
            }
        }
        
        if (logoutBtn) {
            const shouldShow = (role !== "guest");
            if (shouldShow) {
                logoutBtn.style.display = "inline-block";
                logoutBtn.style.visibility = "visible";
            } else {
                logoutBtn.style.display = "none";
            }
            console.log('Logout button display:', shouldShow ? 'shown' : 'hidden', 'role:', role);
        } else {
            console.error('logoutBtn not found! This is a problem.');
            // Try to find it again with multiple selectors
            let retryLogoutBtn = document.getElementById("logoutBtn");
            if (!retryLogoutBtn) {
                retryLogoutBtn = document.querySelector('button.logout-btn');
            }
            if (retryLogoutBtn) {
                console.log('Found logoutBtn on retry');
                const shouldShow = (role !== "guest");
                if (shouldShow) {
                    retryLogoutBtn.style.display = "inline-block";
                    retryLogoutBtn.style.visibility = "visible";
                } else {
                    retryLogoutBtn.style.display = "none";
                }
            } else {
                console.error('Could not find logoutBtn even after retry');
            }
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





