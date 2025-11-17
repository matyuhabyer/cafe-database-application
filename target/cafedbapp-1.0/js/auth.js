/* 
 * Role-Based Access Control Utility
 * Provides functions to check user roles and enforce page access
 */

// Get the context path
if (typeof window.BASE_URL === 'undefined') {
    window.BASE_URL = window.location.origin + window.location.pathname.replace(/\/[^/]*$/, '');
    window.API_BASE = window.BASE_URL + '/api';
}
// Use window property directly to avoid any redeclaration issues

/**
 * Get current user role from localStorage
 * @returns {string} Role: 'guest', 'customer', 'staff', 'manager', or 'admin'
 */
function getCurrentRole() {
    return localStorage.getItem("role") || "guest";
}

/**
 * Get current user type from localStorage
 * @returns {string} User type: 'guest', 'customer', or 'employee'
 */
function getCurrentUserType() {
    return localStorage.getItem("user_type") || "guest";
}

/**
 * Check if user is logged in (not guest)
 * @returns {boolean}
 */
function isLoggedIn() {
    const role = getCurrentRole();
    return role !== "guest";
}

/**
 * Check if user is a customer
 * @returns {boolean}
 */
function isCustomer() {
    return getCurrentRole() === "customer" && getCurrentUserType() === "customer";
}

/**
 * Check if user is an employee (staff, manager, or admin)
 * @returns {boolean}
 */
function isEmployee() {
    const userType = getCurrentUserType();
    const role = getCurrentRole();
    return userType === "employee" && (role === "staff" || role === "manager" || role === "admin");
}

/**
 * Check if user is admin
 * @returns {boolean}
 */
function isAdmin() {
    return getCurrentRole() === "admin" && getCurrentUserType() === "employee";
}

/**
 * Check if user is manager
 * @returns {boolean}
 */
function isManager() {
    return getCurrentRole() === "manager" && getCurrentUserType() === "employee";
}

/**
 * Check if user is staff
 * @returns {boolean}
 */
function isStaff() {
    return getCurrentRole() === "staff" && getCurrentUserType() === "employee";
}

/**
 * Check if user is admin or manager
 * @returns {boolean}
 */
function isAdminOrManager() {
    return isAdmin() || isManager();
}

/**
 * Check if user is admin, manager, or staff
 * @returns {boolean}
 */
function isAdminOrManagerOrStaff() {
    return isAdmin() || isManager() || isStaff();
}

/**
 * Verify session with server
 * @returns {Promise<boolean>} True if session is valid
 */
async function verifySession() {
    try {
        const response = await fetch(`${window.API_BASE}/auth/verify_session`, {
            method: 'GET',
            credentials: 'include'
        });
        
        if (response.ok) {
            const result = await response.json();
            if (result.success && result.data) {
                // Update localStorage with server data
                if (result.data.role) localStorage.setItem("role", result.data.role);
                if (result.data.user_type) localStorage.setItem("user_type", result.data.user_type);
                if (result.data.user_id) localStorage.setItem("user_id", result.data.user_id);
                return true;
            } else if (result.success && !result.data) {
                // Session exists but no user data - invalid
                localStorage.clear();
                return false;
            }
        }
        // If response is not ok, session might be invalid
        if (response.status === 401 || response.status === 403) {
            localStorage.clear();
        }
        return false;
    } catch (error) {
        console.error('Session verification error:', error);
        // On error, don't clear localStorage - might be network issue
        return true; // Assume valid if we can't verify
    }
}

/**
 * Enforce role-based access control on a page
 * @param {Object} options - Access control options
 * @param {string[]} options.allowedRoles - Array of allowed roles (e.g., ['customer', 'admin'])
 * @param {boolean} options.requireLogin - If true, requires any logged-in user
 * @param {boolean} options.allowGuest - If true, allows guest access
 * @param {string} options.redirectTo - URL to redirect if access denied (default: 'index.html')
 * @param {string} options.redirectIfLoggedIn - URL to redirect if user is already logged in (for login/register pages)
 */
async function enforceAccessControl(options = {}) {
    const {
        allowedRoles = [],
        requireLogin = false,
        allowGuest = false,
        redirectTo = 'index.html',
        redirectIfLoggedIn = null
    } = options;

    // Verify session with server
    const sessionValid = await verifySession();
    const role = getCurrentRole();
    const userType = getCurrentUserType();
    
    // If session is invalid and user thinks they're logged in, clear localStorage
    if (!sessionValid && isLoggedIn()) {
        localStorage.clear();
        // Update role to guest after clearing
        const clearedRole = "guest";
        // Don't redirect here if allowGuest is true - let the role check handle it
        if (!allowGuest) {
            alert('Your session has expired. Please login again.');
            window.location.href = redirectTo;
            return;
        }
    }

    // Get updated role after potential session check
    const currentRole = getCurrentRole();
    const currentUserType = getCurrentUserType();

    // Handle redirect if already logged in (for login/register pages)
    if (redirectIfLoggedIn && currentRole !== "guest") {
        // Redirect based on role
        if (isCustomer()) {
            window.location.href = 'products.html';
        } else if (isEmployee()) {
            window.location.href = 'admin.html';
        } else {
            window.location.href = redirectIfLoggedIn;
        }
        return;
    }

    // Check if guest access is allowed
    if (currentRole === "guest" && allowGuest) {
        return; // Allow access
    }

    // Check if login is required
    if (requireLogin && currentRole === "guest") {
        alert('Please login to access this page.');
        window.location.href = redirectTo;
        return;
    }

    // Check if specific roles are required
    if (allowedRoles.length > 0) {
        const hasAccess = allowedRoles.includes(currentRole) || 
                         (allowedRoles.includes('employee') && isEmployee()) ||
                         (allowedRoles.includes('customer') && isCustomer());
        
        if (!hasAccess) {
            const roleNames = allowedRoles.map(r => r.charAt(0).toUpperCase() + r.slice(1)).join(', ');
            alert(`Access denied. This page requires ${roleNames} access.`);
            
            // Redirect based on current role
            if (isCustomer()) {
                window.location.href = 'products.html';
            } else if (isEmployee()) {
                window.location.href = 'admin.html';
            } else {
                window.location.href = redirectTo;
            }
            return;
        }
    }
}

/**
 * Page-specific access control configurations
 */
const PAGE_ACCESS = {
    'index.html': {
        allowGuest: true,
        redirectIfLoggedIn: 'products.html'
    },
    'register.html': {
        allowGuest: true,
        redirectIfLoggedIn: 'products.html'
    },
    'products.html': {
        allowGuest: true
    },
    'cart.html': {
        allowedRoles: ['customer'],
        requireLogin: true
    },
    'orders.html': {
        allowedRoles: ['customer'],
        requireLogin: true
    },
    'profile.html': {
        requireLogin: true
    },
    'admin.html': {
        allowedRoles: ['admin', 'manager', 'staff'],
        requireLogin: true
    },
    'order_details.html': {
        requireLogin: true
    },
    'test-servlets.html': {
        allowedRoles: ['admin'],
        requireLogin: true
    }
};

/**
 * Apply access control based on current page
 */
async function applyPageAccessControl() {
    const currentPage = window.location.pathname.split('/').pop() || 'index.html';
    const config = PAGE_ACCESS[currentPage];
    
    if (config) {
        await enforceAccessControl(config);
    } else {
        // Default: require login for unknown pages
        await enforceAccessControl({ requireLogin: true });
    }
}

// Make functions globally available
window.getCurrentRole = getCurrentRole;
window.getCurrentUserType = getCurrentUserType;
window.isLoggedIn = isLoggedIn;
window.isCustomer = isCustomer;
window.isEmployee = isEmployee;
window.isAdmin = isAdmin;
window.isManager = isManager;
window.isStaff = isStaff;
window.isAdminOrManager = isAdminOrManager;
window.isAdminOrManagerOrStaff = isAdminOrManagerOrStaff;
window.enforceAccessControl = enforceAccessControl;
window.applyPageAccessControl = applyPageAccessControl;

