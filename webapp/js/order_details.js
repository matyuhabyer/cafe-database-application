/* 
 * Order Details Page JavaScript
 * Displays detailed information about a specific order
 */

// Get the context path
if (typeof window.BASE_URL === 'undefined') {
    window.BASE_URL = window.location.origin + window.location.pathname.replace(/\/[^/]*$/, '');
    window.API_BASE = window.BASE_URL + '/api';
}
// Use window properties directly to avoid any redeclaration issues
console.log('Order Details.js - Base URL:', window.BASE_URL, 'API Base:', window.API_BASE);

let orderDetailsContent = null;
let currentOrderId = null;
let currentRole = null;

// Get order ID from URL
function getOrderIdFromURL() {
    const urlParams = new URLSearchParams(window.location.search);
    const orderId = urlParams.get('order_id');
    if (!orderId) {
        console.error('No order_id parameter in URL');
        return null;
    }
    return parseInt(orderId);
}

// Load order details
async function loadOrderDetails() {
    try {
        // Ensure element is available
        if (!orderDetailsContent) {
            orderDetailsContent = document.getElementById('orderDetailsContent');
        }
        if (!orderDetailsContent) {
            console.error('orderDetailsContent element not found');
            return;
        }
        
        const orderId = getOrderIdFromURL();
        if (!orderId) {
            orderDetailsContent.innerHTML = `
                <div class="error-message">
                    <p>Invalid order ID. Please go back and try again.</p>
                </div>
            `;
            return;
        }
        
        currentOrderId = orderId;
        
        const apiUrl = `${window.API_BASE}/orders/get_order_details?order_id=${orderId}`;
        console.log('Fetching order details from URL:', apiUrl);
        
        const response = await fetch(apiUrl, {
            credentials: 'include'
        });
        
        console.log('Order details response status:', response.status, response.statusText);
        
        if (!response.ok) {
            const errorText = await response.text();
            console.error('Order details error response:', errorText);
            
            if (response.status === 401) {
                orderDetailsContent.innerHTML = `
                    <div class="error-message">
                        <p>You are not logged in. Please <a href="index.html" style="color: #6d4c41; font-weight: 600;">login</a> to view order details.</p>
                    </div>
                `;
                return;
            }
            
            if (response.status === 403) {
                orderDetailsContent.innerHTML = `
                    <div class="error-message">
                        <p>You do not have permission to view this order.</p>
                    </div>
                `;
                return;
            }
            
            if (response.status === 404) {
                orderDetailsContent.innerHTML = `
                    <div class="error-message">
                        <p>Order not found.</p>
                    </div>
                `;
                return;
            }
            
            orderDetailsContent.innerHTML = `
                <div class="error-message">
                    <p>Error loading order details: ${response.status} ${response.statusText}<br>${errorText.substring(0, 200)}</p>
                </div>
            `;
            return;
        }
        
        const result = await response.json();
        console.log('Order details API Response:', result);
        
        if (result.success && result.data) {
            renderOrderDetails(result.data);
        } else {
            orderDetailsContent.innerHTML = `
                <div class="error-message">
                    <p>Error loading order details: ${result.message || 'Unknown error'}</p>
                </div>
            `;
        }
    } catch (error) {
        console.error('Error loading order details:', error);
        if (orderDetailsContent) {
            orderDetailsContent.innerHTML = `
                <div class="error-message">
                    <p>Error connecting to server. Please check your connection and try again.</p>
                </div>
            `;
        }
    }
}

// Render order details
function renderOrderDetails(order) {
    if (!orderDetailsContent) {
        return;
    }
    
    const statusClass = `status-${order.status || 'pending'}`;
    const statusText = (order.status || 'pending').charAt(0).toUpperCase() + (order.status || 'pending').slice(1);
    
    // Format date
    const orderDate = order.order_date ? new Date(order.order_date).toLocaleString() : 'N/A';
    
    let html = `
        <div class="order-details-card">
            <h3>Order Information</h3>
            <div class="info-grid">
                <div class="info-item">
                    <label>Order ID</label>
                    <span>#${order.order_id}</span>
                </div>
                <div class="info-item">
                    <label>Order Date</label>
                    <span>${orderDate}</span>
                </div>
                <div class="info-item">
                    <label>Status</label>
                    <span><span class="status-badge ${statusClass}">${statusText}</span></span>
                </div>
                <div class="info-item">
                    <label>Total Amount</label>
                    <span>₱${parseFloat(order.total_amount || 0).toFixed(2)}</span>
                </div>
                ${order.earned_points ? `
                <div class="info-item">
                    <label>Points Earned</label>
                    <span>${order.earned_points} points</span>
                </div>
                ` : ''}
            </div>
            
            ${order.customer ? `
            <div class="info-grid" style="margin-top: 20px;">
                <div class="info-item">
                    <label>Customer Name</label>
                    <span>${order.customer.name || 'N/A'}</span>
                </div>
                <div class="info-item">
                    <label>Email</label>
                    <span>${order.customer.email || 'N/A'}</span>
                </div>
                <div class="info-item">
                    <label>Phone</label>
                    <span>${order.customer.phone_num || 'N/A'}</span>
                </div>
            </div>
            ` : ''}
            
            ${order.branch ? `
            <div class="info-grid" style="margin-top: 20px;">
                <div class="info-item">
                    <label>Branch</label>
                    <span>${order.branch.name || 'N/A'}</span>
                </div>
                ${order.branch.address ? `
                <div class="info-item">
                    <label>Address</label>
                    <span>${order.branch.address}</span>
                </div>
                ` : ''}
            </div>
            ` : ''}
            
            <div class="items-section">
                <h3 style="margin-top: 0;">Order Items</h3>
                ${renderOrderItems(order.items || [])}
            </div>
            
            ${order.transaction ? `
            <div class="transaction-section">
                <h3>Payment Information</h3>
                <div class="transaction-card">
                    <h4>Transaction #${order.transaction.transaction_id}</h4>
                    <div class="info-grid">
                        <div class="info-item">
                            <label>Payment Method</label>
                            <span>${formatPaymentMethod(order.transaction.payment_method)}</span>
                        </div>
                        <div class="info-item">
                            <label>Amount Paid</label>
                            <span>${order.transaction.currency_symbol || '₱'}${parseFloat(order.transaction.amount_paid || 0).toFixed(2)}</span>
                        </div>
                        <div class="info-item">
                            <label>Currency</label>
                            <span>${order.transaction.currency_code || 'PHP'}</span>
                        </div>
                        <div class="info-item">
                            <label>Exchange Rate</label>
                            <span>${parseFloat(order.transaction.exchange_rate || 1).toFixed(4)}</span>
                        </div>
                        <div class="info-item">
                            <label>Transaction Date</label>
                            <span>${order.transaction.transaction_date ? new Date(order.transaction.transaction_date).toLocaleString() : 'N/A'}</span>
                        </div>
                        <div class="info-item">
                            <label>Status</label>
                            <span><span class="status-badge ${order.transaction.status === 'completed' ? 'status-completed' : 'status-pending'}">${order.transaction.status || 'pending'}</span></span>
                        </div>
                    </div>
                </div>
            </div>
            ` : `
            <div class="transaction-section">
                <h3>Payment Information</h3>
                <div class="info-item">
                    <p style="color: #666; padding: 15px; background: #fff3cd; border-radius: 8px;">
                        No payment recorded yet.
                    </p>
                </div>
            </div>
            `}
            
            ${order.history && order.history.length > 0 ? `
            <div class="history-section">
                <h3>Order History</h3>
                ${renderOrderHistory(order.history)}
            </div>
            ` : ''}
            
            ${renderActionButtons(order)}
        </div>
    `;
    
    orderDetailsContent.innerHTML = html;
    
    // Attach event listeners to action buttons
    attachActionListeners(order);
}

// Render order items
function renderOrderItems(items) {
    if (!items || items.length === 0) {
        return '<p style="color: #666; padding: 15px;">No items in this order.</p>';
    }
    
    // Debug logging
    console.log('renderOrderItems: Processing', items.length, 'items');
    items.forEach((item, index) => {
        console.log(`Item ${index}:`, {
            menu_id: item.menu_id,
            name: item.name,
            menu_name: item.menu_name,
            hasName: 'name' in item,
            nameValue: item.name
        });
    });
    
    return items.map(item => {
        const itemPrice = parseFloat(item.price || 0).toFixed(2);
        const itemTotal = parseFloat(item.total_price || item.price || 0).toFixed(2);
        
        let extrasHTML = '';
        if (item.extras && item.extras.length > 0) {
            extrasHTML = `
                <div class="order-item-extras">
                    <strong>Extras:</strong>
                    <ul>
                        ${item.extras.map(extra => 
                            `<li>${extra.name} (${extra.quantity}x) - ₱${parseFloat(extra.price || 0).toFixed(2)}</li>`
                        ).join('')}
                    </ul>
                </div>
            `;
        }
        
        return `
            <div class="order-item">
                <div class="order-item-header">
                    <div class="order-item-name">${item.name || 'Unnamed Item'}</div>
                    <div class="order-item-price">₱${itemTotal}</div>
                </div>
                <div class="order-item-details">
                    <strong>Quantity:</strong> ${item.quantity || 1}<br>
                    <strong>Unit Price:</strong> ₱${itemPrice}<br>
                    ${item.temperature ? `<strong>Temperature:</strong> ${item.temperature}<br>` : ''}
                    ${item.size ? `<strong>Size:</strong> ${item.size}<br>` : ''}
                </div>
                ${extrasHTML}
            </div>
        `;
    }).join('');
}

// Render order history
function renderOrderHistory(history) {
    if (!history || history.length === 0) {
        return '<p style="color: #666; padding: 15px;">No history available.</p>';
    }
    
    return history.map(entry => {
        const timestamp = entry.timestamp ? new Date(entry.timestamp).toLocaleString() : 'N/A';
        const statusText = (entry.status || 'unknown').charAt(0).toUpperCase() + (entry.status || 'unknown').slice(1);
        const employeeName = entry.employee_name || 'System';
        const remarks = entry.remarks || 'No remarks';
        
        return `
            <div class="history-item">
                <div class="history-item-info">
                    <div class="history-item-status">${statusText}</div>
                    <div class="history-item-details">
                        <strong>By:</strong> ${employeeName}<br>
                        <strong>Remarks:</strong> ${remarks}
                    </div>
                    <div class="history-item-time">${timestamp}</div>
                </div>
            </div>
        `;
    }).join('');
}

// Render action buttons based on role and order status
function renderActionButtons(order) {
    currentRole = getCurrentRole();
    const isEmployee = window.isEmployee ? window.isEmployee() : false;
    const status = order.status || 'pending';
    
    // Only show action buttons for employees (staff, manager, admin)
    if (!isEmployee) {
        return '';
    }
    
    let buttons = '<div class="action-buttons">';
    
    // Staff/Manager/Admin can confirm pending orders
    if (status === 'pending') {
        buttons += `<button class="btn-action btn-confirm" data-action="confirm">✓ Confirm Order</button>`;
        buttons += `<button class="btn-action btn-cancel" data-action="cancel">✗ Cancel Order</button>`;
    }
    
    // Staff/Manager/Admin can complete confirmed orders
    if (status === 'confirmed') {
        buttons += `<button class="btn-action btn-complete" data-action="complete">✓ Complete Order</button>`;
        buttons += `<button class="btn-action btn-cancel" data-action="cancel">✗ Cancel Order</button>`;
    }
    
    buttons += '</div>';
    
    return buttons;
}

// Attach event listeners to action buttons
function attachActionListeners(order) {
    const actionButtons = document.querySelectorAll('.btn-action');
    actionButtons.forEach(button => {
        button.addEventListener('click', async (e) => {
            const action = button.getAttribute('data-action');
            if (!action || !currentOrderId) {
                return;
            }
            
            let status = action;
            if (action === 'confirm') {
                status = 'confirmed';
            } else if (action === 'complete') {
                status = 'completed';
            } else if (action === 'cancel') {
                status = 'cancelled';
            }
            
            await updateOrderStatus(currentOrderId, status);
        });
    });
}

// Update order status
async function updateOrderStatus(orderId, status) {
    if (!confirm(`Are you sure you want to ${status === 'confirmed' ? 'confirm' : status === 'completed' ? 'complete' : 'cancel'} this order?`)) {
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
                remarks: `Order ${status} by ${currentRole || 'employee'}`
            })
        });
        
        const result = await response.json();
        
        if (result.success) {
            alert(`Order ${status} successfully!`);
            // Reload order details
            loadOrderDetails();
        } else {
            alert('Error: ' + result.message);
        }
    } catch (error) {
        console.error('Error updating order status:', error);
        alert('An error occurred while updating order status.');
    }
}

// Format payment method
function formatPaymentMethod(method) {
    if (!method) return 'N/A';
    
    const methods = {
        'cash': 'Cash',
        'card': 'Card',
        'bank_transfer': 'Bank Transfer',
        'others': 'Others'
    };
    
    return methods[method.toLowerCase()] || method;
}

// Initialize
function initializeOrderDetails() {
    try {
        orderDetailsContent = document.getElementById('orderDetailsContent');
        
        if (!orderDetailsContent) {
            console.error('orderDetailsContent element not found, retrying...');
            setTimeout(initializeOrderDetails, 100);
            return;
        }
        
        console.log('Order details content element found:', orderDetailsContent);
        console.log('Initializing order details page...');
        loadOrderDetails();
    } catch (error) {
        console.error('Error initializing order details:', error);
        if (orderDetailsContent) {
            orderDetailsContent.innerHTML = '<div class="error-message"><p>Error initializing page. Please refresh.</p></div>';
        }
    }
}

// Initialize when DOM is ready
if (typeof window.initializeWhenReady === 'function') {
    window.initializeWhenReady(
        initializeOrderDetails,
        ['orderDetailsContent'], // Required element
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
            const element = document.getElementById('orderDetailsContent');
            if (element) {
                initializeOrderDetails();
            } else if (retries < maxRetries) {
                retries++;
                setTimeout(tryInit, 100);
            } else {
                console.error('orderDetailsContent element not found after multiple retries');
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

