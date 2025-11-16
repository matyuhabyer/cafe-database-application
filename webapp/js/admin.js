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
console.log('Admin.js - Base URL:', window.BASE_URL, 'API Base:', window.API_BASE);

// Load branches
async function loadBranches() {
    try {
        const response = await fetch(`${window.API_BASE}/admin/get_branches`, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const result = await response.json();
        
        const branchesList = document.getElementById('branchesList');
        if (!branchesList) {
            console.error('branchesList element not found');
            return;
        }
        
        if (result.success && result.data && result.data.length > 0) {
            branchesList.innerHTML = result.data.map(branch => `
                <div class="branch-item">
                    <h4>${branch.name}</h4>
                    <p><strong>Address:</strong> ${branch.address || 'N/A'}</p>
                    <p><strong>Contact:</strong> ${branch.contact_num || 'N/A'}</p>
                    <p><strong>Manager:</strong> ${branch.manager_name || 'N/A'}</p>
                </div>
            `).join('');
        } else {
            branchesList.innerHTML = '<p class="error-message">No branches found.</p>';
        }
    } catch (error) {
        console.error('Error loading branches:', error);
        document.getElementById('branchesList').innerHTML = '<p class="error-message">Error loading branches. Please try again later.</p>';
    }
}

// Load reports
async function loadReports() {
    try {
        const response = await fetch(`${window.API_BASE}/admin/get_reports`, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const result = await response.json();
        
        const reportsContent = document.getElementById('reportsContent');
        if (!reportsContent) {
            console.error('reportsContent element not found');
            return;
        }
        
        if (result.success && result.data && result.data.reports) {
            const reports = result.data.reports;
            let html = '<div class="reports-section">';
            
            if (reports.sales) {
                html += `
                    <div class="report-card">
                        <h4>📈 Sales Summary</h4>
                        <p><strong>Total Sales:</strong> <span class="stat-value">₱${parseFloat(reports.sales.total_sales_php || 0).toFixed(2)}</span></p>
                        <p><strong>Total Transactions:</strong> ${reports.sales.total_transactions || 0}</p>
                    </div>
                `;
            }
            
            if (reports.top_items && reports.top_items.length > 0) {
                html += `
                    <div class="report-card">
                        <h4>🏆 Top Selling Items</h4>
                        <ul>
                            ${reports.top_items.map(item => 
                                `<li><strong>${item.name}</strong> - Quantity: ${item.total_quantity}, Revenue: ₱${parseFloat(item.total_revenue || 0).toFixed(2)}</li>`
                            ).join('')}
                        </ul>
                    </div>
                `;
            }
            
            if (reports.orders_by_status) {
                html += `
                    <div class="report-card">
                        <h4>📋 Orders by Status</h4>
                        <ul>
                            ${Object.entries(reports.orders_by_status).map(([status, count]) => 
                                `<li><strong>${status.charAt(0).toUpperCase() + status.slice(1)}:</strong> ${count}</li>`
                            ).join('')}
                        </ul>
                    </div>
                `;
            }
            
            html += '</div>';
            reportsContent.innerHTML = html;
        } else {
            reportsContent.innerHTML = '<p class="error-message">No reports available.</p>';
        }
    } catch (error) {
        console.error('Error loading reports:', error);
        document.getElementById('reportsContent').innerHTML = '<p class="error-message">Error loading reports. Please try again later.</p>';
    }
}

// Load orders for staff/manager/admin
async function loadOrders() {
    try {
        const response = await fetch(`${window.API_BASE}/orders/get_orders`, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const result = await response.json();
        
        const ordersList = document.getElementById('ordersList');
        if (!ordersList) {
            console.error('ordersList element not found');
            return;
        }
        
        if (result.success && result.data && result.data.length > 0) {
            const orders = result.data;
            let html = '<div class="orders-container">';
            
            orders.forEach(order => {
                const orderDate = new Date(order.order_date).toLocaleString();
                const statusClass = order.status === 'completed' ? 'status-completed' : 
                                   order.status === 'confirmed' ? 'status-confirmed' : 
                                   order.status === 'pending' ? 'status-pending' : 'status-cancelled';
                
                html += `
                    <div class="order-item">
                        <div class="order-header">
                            <h4>Order #${order.order_id}</h4>
                            <span class="order-status ${statusClass}">${order.status.charAt(0).toUpperCase() + order.status.slice(1)}</span>
                        </div>
                        <div class="order-details">
                            <p><strong>Customer:</strong> ${order.customer_name || 'N/A'}</p>
                            <p><strong>Branch:</strong> ${order.branch_name || 'N/A'}</p>
                            <p><strong>Date:</strong> ${orderDate}</p>
                            <p><strong>Total:</strong> ₱${parseFloat(order.total_amount || 0).toFixed(2)}</p>
                            ${order.earned_points ? `<p><strong>Points Earned:</strong> ${order.earned_points}</p>` : ''}
                        </div>
                        <div class="order-actions">
                            ${order.status === 'pending' ? `
                                <button onclick="updateOrderStatus(${order.order_id}, 'confirmed')" class="btn-confirm">Confirm Order</button>
                            ` : ''}
                            ${order.status === 'confirmed' ? `
                                <button onclick="updateOrderStatus(${order.order_id}, 'completed')" class="btn-complete">Mark as Completed</button>
                            ` : ''}
                            <button onclick="viewOrderDetails(${order.order_id})" class="btn-view">View Details</button>
                        </div>
                    </div>
                `;
            });
            
            html += '</div>';
            ordersList.innerHTML = html;
        } else {
            if (result.success && result.data && result.data.length === 0) {
                ordersList.innerHTML = '<p class="error-message">No orders found.</p>';
            } else {
                ordersList.innerHTML = '<p class="error-message">' + (result.message || 'No orders found.') + '</p>';
            }
        }
    } catch (error) {
        console.error('Error loading orders:', error);
        const ordersList = document.getElementById('ordersList');
        if (ordersList) {
            ordersList.innerHTML = '<p class="error-message">Error loading orders: ' + error.message + '. Please try again later.</p>';
        }
    }
}

// Update order status (for staff)
async function updateOrderStatus(orderId, status) {
    if (!confirm(`Are you sure you want to ${status === 'confirmed' ? 'confirm' : 'complete'} this order?`)) {
        return;
    }
    
    try {
        const response = await fetch(`${window.API_BASE}/orders/update_order_status`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify({
                order_id: orderId,
                status: status,
                remarks: status === 'confirmed' ? 'Order confirmed by staff' : 'Order completed by staff'
            })
        });
        
        const result = await response.json();
        
        if (result.success) {
            alert(`Order ${status === 'confirmed' ? 'confirmed' : 'completed'} successfully!`);
            loadOrders(); // Reload orders
        } else {
            alert('Error: ' + result.message);
        }
    } catch (error) {
        console.error('Error updating order status:', error);
        alert('An error occurred while updating order status.');
    }
}

// View order details
function viewOrderDetails(orderId) {
    window.location.href = `order_details.html?order_id=${orderId}`;
}

// Load employees (Admin only)
async function loadEmployees() {
    try {
        const response = await fetch(`${window.API_BASE}/admin/get_employees`, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const result = await response.json();
        
        const employeesList = document.getElementById('employeesList');
        if (!employeesList) {
            console.error('employeesList element not found');
            return;
        }
        
        if (result.success && result.data && result.data.length > 0) {
            employeesList.innerHTML = result.data.map(emp => `
                <div class="employee-item">
                    <h4>${emp.name}</h4>
                    <p><strong>Role:</strong> ${emp.role ? emp.role.charAt(0).toUpperCase() + emp.role.slice(1) : 'N/A'}</p>
                    <p><strong>Branch:</strong> ${emp.branch_name || 'N/A'}</p>
                    <p><strong>Contact:</strong> ${emp.contact_num || 'N/A'}</p>
                </div>
            `).join('');
        } else {
            employeesList.innerHTML = '<p class="error-message">No employees found.</p>';
        }
    } catch (error) {
        console.error('Error loading employees:', error);
        document.getElementById('employeesList').innerHTML = '<p class="error-message">Error loading employees. Please try again later.</p>';
    }
}

// Initialize based on role
async function initializeAdminPanel() {
    try {
        const role = localStorage.getItem("role") || "guest";
        const userType = localStorage.getItem("user_type") || "guest";
        
        console.log('Initializing admin panel for role:', role, 'userType:', userType);
        
        // Wait for elements to be available
        const pageTitle = document.getElementById('pageTitle');
        if (!pageTitle) {
            console.error('pageTitle element not found, retrying...');
            setTimeout(initializeAdminPanel, 200);
            return;
        }
        
        // Update page title based on role
        if (role === "staff") {
            pageTitle.textContent = "📦 Order Management";
        } else if (role === "manager") {
            pageTitle.textContent = "📊 Manager Dashboard";
        } else {
            pageTitle.textContent = "⚙️ Admin Panel";
        }
        
        // Show/hide sections based on role
        if (role === "admin") {
            // Admin: Show all sections
            const adminManagerSections = document.getElementById('adminManagerSections');
            const adminOnlySections = document.getElementById('adminOnlySections');
            
            if (adminManagerSections) adminManagerSections.style.display = 'grid';
            if (adminOnlySections) adminOnlySections.style.display = 'block';
            
            const branchesSection = document.getElementById('branchesSection');
            const reportsSection = document.getElementById('reportsSection');
            const employeesSection = document.getElementById('employeesSection');
            const ordersSection = document.getElementById('ordersSection');
            
            if (branchesSection) branchesSection.style.display = 'block';
            if (reportsSection) reportsSection.style.display = 'block';
            if (employeesSection) employeesSection.style.display = 'block';
            if (ordersSection) ordersSection.style.display = 'block';
            
            console.log('Loading data for admin...');
            await Promise.all([
                loadBranches(),
                loadReports(),
                loadEmployees(),
                loadOrders()
            ]);
        } else if (role === "manager") {
            // Manager: Show reports, menu management (via reports), and orders
            const adminManagerSections = document.getElementById('adminManagerSections');
            const adminOnlySections = document.getElementById('adminOnlySections');
            
            if (adminManagerSections) adminManagerSections.style.display = 'grid';
            if (adminOnlySections) adminOnlySections.style.display = 'none';
            
            const branchesSection = document.getElementById('branchesSection');
            const reportsSection = document.getElementById('reportsSection');
            const employeesSection = document.getElementById('employeesSection');
            const ordersSection = document.getElementById('ordersSection');
            
            if (branchesSection) branchesSection.style.display = 'block';
            if (reportsSection) reportsSection.style.display = 'block';
            if (employeesSection) employeesSection.style.display = 'none';
            if (ordersSection) ordersSection.style.display = 'block';
            
            console.log('Loading data for manager...');
            await Promise.all([
                loadBranches(),
                loadReports(),
                loadOrders()
            ]);
        } else if (role === "staff") {
            // Staff: Show only order management
            const adminManagerSections = document.getElementById('adminManagerSections');
            const adminOnlySections = document.getElementById('adminOnlySections');
            const ordersSection = document.getElementById('ordersSection');
            
            if (adminManagerSections) adminManagerSections.style.display = 'none';
            if (adminOnlySections) adminOnlySections.style.display = 'none';
            if (ordersSection) ordersSection.style.display = 'block';
            
            console.log('Loading data for staff...');
            await loadOrders();
        } else {
            console.warn('Unknown role:', role);
        }
    } catch (error) {
        console.error('Error initializing admin panel:', error);
    }
}

// Make functions globally available
window.updateOrderStatus = updateOrderStatus;
window.viewOrderDetails = viewOrderDetails;

// Initialize when DOM is ready
if (typeof window.initializeWhenReady === 'function') {
    window.initializeWhenReady(
        initializeAdminPanel,
        ['adminContent', 'pageTitle'], // Required elements
        {
            waitForAccessControl: true, // Wait for access control
            timeout: 5000
        }
    );
} else {
    // Fallback if dom-utils.js isn't loaded yet
    async function startAdminInitialization() {
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
            const content = document.getElementById('adminContent');
            const title = document.getElementById('pageTitle');
            if (content && title) {
                initializeAdminPanel();
            } else if (retries < maxRetries) {
                retries++;
                setTimeout(tryInit, 100);
            } else {
                console.error('Admin elements not found after multiple retries');
            }
        };
        
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => {
                setTimeout(tryInit, 200);
            });
        } else {
            setTimeout(tryInit, 200);
        }
    }
    
    startAdminInitialization();
}
