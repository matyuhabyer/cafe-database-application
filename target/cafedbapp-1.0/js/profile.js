/* 
 * Profile Page JavaScript
 * Loads and displays user profile information
 */

// Get the context path
if (typeof window.BASE_URL === 'undefined') {
    window.BASE_URL = window.location.origin + window.location.pathname.replace(/\/[^/]*$/, '');
    window.API_BASE = window.BASE_URL + '/api';
}
// Use window properties directly to avoid any redeclaration issues
console.log('Profile.js - Base URL:', window.BASE_URL, 'API Base:', window.API_BASE);

let profileContent = null;

// Load profile data
async function loadProfile() {
    try {
        // Ensure element is available
        if (!profileContent) {
            profileContent = document.getElementById('profileContent');
        }
        if (!profileContent) {
            console.error('profileContent element not found');
            return;
        }
        
        const apiUrl = `${window.API_BASE}/auth/get_profile`;
        console.log('Fetching profile from URL:', apiUrl);
        
        const response = await fetch(apiUrl, {
            credentials: 'include'
        });
        
        console.log('Profile response status:', response.status, response.statusText);
        
        if (!response.ok) {
            const errorText = await response.text();
            console.error('Profile error response:', errorText);
            
            if (response.status === 401) {
                profileContent.innerHTML = `
                    <div class="error-message">
                        <p>You are not logged in. Please <a href="index.html" style="color: #6d4c41; font-weight: 600;">login</a> to view your profile.</p>
                    </div>
                `;
                return;
            }
            
            profileContent.innerHTML = `
                <div class="error-message">
                    <p>Error loading profile: ${response.status} ${response.statusText}<br>${errorText.substring(0, 200)}</p>
                </div>
            `;
            return;
        }
        
        const result = await response.json();
        console.log('Profile API Response:', result);
        
        if (result.success && result.data) {
            renderProfile(result.data);
        } else {
            profileContent.innerHTML = `
                <div class="error-message">
                    <p>Error loading profile: ${result.message || 'Unknown error'}</p>
                </div>
            `;
        }
    } catch (error) {
        console.error('Error loading profile:', error);
        if (profileContent) {
            profileContent.innerHTML = `
                <div class="error-message">
                    <p>Error connecting to server. Please check your connection and try again.</p>
                </div>
            `;
        }
    }
}

// Render profile information
function renderProfile(profile) {
    let html = '<div class="profile-card">';
    
    if (profile.user_type === 'customer') {
        // Customer profile
        html += `
            <div class="profile-section">
                <h3>👤 Personal Information</h3>
                <div class="info-grid">
                    <div class="info-item">
                        <label>Username</label>
                        <span>${profile.username || 'N/A'}</span>
                    </div>
                    <div class="info-item">
                        <label>Full Name</label>
                        <span>${profile.name || 'N/A'}</span>
                    </div>
                    <div class="info-item">
                        <label>Email</label>
                        <span>${profile.email || 'N/A'}</span>
                    </div>
                    <div class="info-item">
                        <label>Phone Number</label>
                        <span>${profile.phone_num || 'N/A'}</span>
                    </div>
                </div>
            </div>
        `;
        
        // Loyalty card section
        if (profile.loyalty_card) {
            const loyalty = profile.loyalty_card;
            const canRedeem = loyalty.can_redeem;
            const lastRedeemed = loyalty.last_redeemed ? new Date(loyalty.last_redeemed).toLocaleDateString() : 'Never';
            const createdDate = loyalty.created_at ? new Date(loyalty.created_at).toLocaleDateString() : 'N/A';
            
            html += `
                <div class="profile-section">
                    <h3>🎁 Loyalty Card</h3>
                    <div class="loyalty-card-section">
                        <h4>💳 ${loyalty.card_number || 'N/A'}</h4>
                        <div class="points-display">${loyalty.points || 0} Points</div>
                        <div class="loyalty-stats">
                            <div class="loyalty-stat">
                                <label>Card Status</label>
                                <div class="value">${loyalty.is_active ? '✓ Active' : '✗ Inactive'}</div>
                            </div>
                            <div class="loyalty-stat">
                                <label>Last Redeemed</label>
                                <div class="value" style="font-size: 1.2rem;">${lastRedeemed}</div>
                            </div>
                            <div class="loyalty-stat">
                                <label>Member Since</label>
                                <div class="value" style="font-size: 1.2rem;">${createdDate}</div>
                            </div>
                        </div>
                        ${canRedeem ? `
                            <div class="redeem-info">
                                🎉 You have enough points to redeem a free item! (100 points required)
                            </div>
                        ` : `
                            <div class="redeem-info">
                                You need ${100 - (loyalty.points || 0)} more points to redeem a free item
                            </div>
                        `}
                    </div>
                </div>
            `;
        } else {
            html += `
                <div class="profile-section">
                    <h3>🎁 Loyalty Card</h3>
                    <p style="color: #666; padding: 20px; background: white; border-radius: 10px; border: 1px solid #d7ccc8;">
                        No loyalty card found. A loyalty card will be created when you place your first order.
                    </p>
                </div>
            `;
        }
    } else if (profile.user_type === 'employee') {
        // Employee profile
        const roleBadge = profile.role ? `<span class="role-badge">${profile.role}</span>` : '';
        
        html += `
            <div class="profile-section">
                <h3>👤 Employee Information${roleBadge}</h3>
                <div class="info-grid">
                    <div class="info-item">
                        <label>Username</label>
                        <span>${profile.username || 'N/A'}</span>
                    </div>
                    <div class="info-item">
                        <label>Full Name</label>
                        <span>${profile.name || 'N/A'}</span>
                    </div>
                    <div class="info-item">
                        <label>Role</label>
                        <span>${profile.role ? profile.role.charAt(0).toUpperCase() + profile.role.slice(1) : 'N/A'}</span>
                    </div>
                    <div class="info-item">
                        <label>Contact Number</label>
                        <span>${profile.contact_num || 'N/A'}</span>
                    </div>
                </div>
            </div>
        `;
        
        // Branch information
        if (profile.branch) {
            html += `
                <div class="profile-section">
                    <h3>📍 Branch Assignment</h3>
                    <div class="branch-info">
                        <h5>${profile.branch.name || 'N/A'}</h5>
                        <p><strong>Address:</strong> ${profile.branch.address || 'N/A'}</p>
                        <p><strong>Contact:</strong> ${profile.branch.contact_num || 'N/A'}</p>
                    </div>
                </div>
            `;
        } else {
            html += `
                <div class="profile-section">
                    <h3>📍 Branch Assignment</h3>
                    <p style="color: #666; padding: 20px; background: white; border-radius: 10px; border: 1px solid #d7ccc8;">
                        No branch assigned.
                    </p>
                </div>
            `;
        }
    }
    
    html += `
            <div class="logout-section">
                <button class="btn-logout" onclick="handleProfileLogout()">
                    🚪 Logout
                </button>
            </div>
        </div>
    `;
    
    if (profileContent) {
        profileContent.innerHTML = html;
    }
}

// Logout handler for profile page
function handleProfileLogout() {
    if (confirm('Are you sure you want to logout?')) {
        if (typeof window.handleLogout === 'function') {
            window.handleLogout();
        } else {
            // Fallback logout if handleLogout is not available
            async function performLogout() {
                try {
                    const response = await fetch(`${window.API_BASE}/auth/logout`, {
                        method: 'POST',
                        credentials: 'include'
                    });
                    
                    // Clear local storage
                    localStorage.clear();
                    
                    // Redirect to login
                    window.location.href = "index.html";
                } catch (error) {
                    console.error('Logout error:', error);
                    // Still clear local storage and redirect
                    localStorage.clear();
                    window.location.href = "index.html";
                }
            }
            performLogout();
        }
    }
}

// Make logout function globally available
window.handleProfileLogout = handleProfileLogout;

// Initialize
function initializeProfile() {
    try {
        profileContent = document.getElementById('profileContent');
        
        if (!profileContent) {
            console.error('profileContent element not found, retrying...');
            setTimeout(initializeProfile, 100);
            return;
        }
        
        console.log('Profile content element found:', profileContent);
        console.log('Initializing profile page...');
        loadProfile();
    } catch (error) {
        console.error('Error initializing profile:', error);
        if (profileContent) {
            profileContent.innerHTML = '<div class="error-message"><p>Error initializing page. Please refresh.</p></div>';
        }
    }
}

// Initialize when DOM is ready
if (typeof window.initializeWhenReady === 'function') {
    window.initializeWhenReady(
        initializeProfile,
        ['profileContent'], // Required element
        {
            waitForAccessControl: true, // Wait for access control
            timeout: 5000
        }
    );
} else {
    // Fallback if dom-utils.js isn't loaded yet
    async function startInitialization() {
        if (typeof window.applyPageAccessControl === 'function') {
            try {
                const accessControlPromise = window.applyPageAccessControl();
                const timeoutPromise = new Promise((resolve) => setTimeout(resolve, 2000));
                await Promise.race([accessControlPromise, timeoutPromise]);
            } catch (error) {
                console.error('Access control error:', error);
            }
        }
        
        let retries = 0;
        const maxRetries = 50;
        const tryInit = () => {
            const element = document.getElementById('profileContent');
            if (element) {
                initializeProfile();
            } else if (retries < maxRetries) {
                retries++;
                setTimeout(tryInit, 100);
            } else {
                console.error('profileContent element not found after multiple retries');
            }
        };
        
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => {
                setTimeout(tryInit, 100);
            });
        } else {
            setTimeout(tryInit, 100);
        }
    }
    
    startInitialization();
}

