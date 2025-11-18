/* 
 * Receipt Display Script
 * Handles fetching and displaying receipt data
 */

// Get the context path
if (typeof window.BASE_URL === 'undefined') {
    window.BASE_URL = window.location.origin + window.location.pathname.replace(/\/[^/]*$/, '');
    window.API_BASE = window.BASE_URL + '/api';
}

// Get URL parameters
const urlParams = new URLSearchParams(window.location.search);
const transactionId = urlParams.get('transaction_id');
const orderId = urlParams.get('order_id');

// Format date
function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Format currency
function formatCurrency(amount, symbol) {
    return `${symbol}${parseFloat(amount).toFixed(2)}`;
}

// Format payment method
function formatPaymentMethod(method) {
    if (!method) return 'N/A';
    return method.split('_').map(word => 
        word.charAt(0).toUpperCase() + word.slice(1)
    ).join(' ');
}

// Render receipt
function renderReceipt(receipt) {
    const receiptContent = document.getElementById('receiptContent');
    
    if (!receipt) {
        receiptContent.innerHTML = '<div class="error">Receipt not found</div>';
        return;
    }
    
    const transaction = receipt.transaction || {};
    const order = receipt.order || {};
    const customer = receipt.customer || {};
    const branch = receipt.branch || {};
    const items = receipt.items || [];
    const loyalty = receipt.loyalty || {};
    
    const currencySymbol = transaction.currency_symbol || '₱';
    const totalAmount = order.total_amount || 0;
    const amountPaid = transaction.amount_paid || 0;
    const exchangeRate = transaction.exchange_rate || 1;
    
    // Calculate amount in PHP (base currency)
    const amountPhp = amountPaid / exchangeRate;
    
    let html = `
        <div class="receipt-header">
            <img src="assets/images/cafe_logo.jpg" alt="The Waiting Room Café Logo" class="receipt-logo">
            <h1>THE WAITING ROOM CAFÉ</h1>
            <p>Thank you for your purchase!</p>
        </div>
        
        <div class="receipt-section">
            <h3>Transaction Details</h3>
            <div class="receipt-info">
                <div class="receipt-info-item">
                    <label>Transaction ID</label>
                    <span>#${transaction.transaction_id || 'N/A'}</span>
                </div>
                <div class="receipt-info-item">
                    <label>Order ID</label>
                    <span>#${order.order_id || 'N/A'}</span>
                </div>
                <div class="receipt-info-item">
                    <label>Date & Time</label>
                    <span>${formatDate(transaction.transaction_date)}</span>
                </div>
                <div class="receipt-info-item">
                    <label>Payment Method</label>
                    <span>${formatPaymentMethod(transaction.payment_method)}</span>
                </div>
            </div>
        </div>
        
        <div class="receipt-section">
            <h3>Customer Information</h3>
            <div class="receipt-info">
                <div class="receipt-info-item">
                    <label>Name</label>
                    <span>${customer.name || 'N/A'}</span>
                </div>
                <div class="receipt-info-item">
                    <label>Email</label>
                    <span>${customer.email || 'N/A'}</span>
                </div>
                <div class="receipt-info-item">
                    <label>Phone</label>
                    <span>${customer.phone_num || 'N/A'}</span>
                </div>
            </div>
        </div>
        
        <div class="receipt-section">
            <h3>Branch Information</h3>
            <div class="receipt-info">
                <div class="receipt-info-item">
                    <label>Branch</label>
                    <span>${branch.name || 'N/A'}</span>
                </div>
                <div class="receipt-info-item">
                    <label>Address</label>
                    <span>${branch.address || 'N/A'}</span>
                </div>
                ${branch.contact_num ? `
                <div class="receipt-info-item">
                    <label>Contact</label>
                    <span>${branch.contact_num}</span>
                </div>
                ` : ''}
            </div>
        </div>
        
        <div class="receipt-section">
            <h3>Order Items</h3>
            <div class="receipt-items">
    `;
    
    items.forEach(item => {
        const itemTotal = item.total_price || (item.unit_price * item.quantity);
        html += `
            <div class="receipt-item">
                <div class="receipt-item-info">
                    <h4>${item.name || 'Unknown Item'}</h4>
                    <p>Quantity: ${item.quantity}</p>
                    ${item.temperature ? `<p>Temperature: ${item.temperature}</p>` : ''}
                    ${item.extras && item.extras.length > 0 ? `
                        <p>Extras: ${item.extras.map(e => `${e.name}${e.quantity > 1 ? ` (x${e.quantity})` : ''}`).join(', ')}</p>
                    ` : ''}
                </div>
                <div class="receipt-item-price">
                    ${formatCurrency(itemTotal, currencySymbol)}
                </div>
            </div>
        `;
    });
    
    html += `
            </div>
        </div>
        
        <div class="receipt-total">
            <div class="receipt-total-row">
                <span>Subtotal (PHP):</span>
                <span>₱${parseFloat(totalAmount).toFixed(2)}</span>
            </div>
            ${exchangeRate !== 1 ? `
            <div class="receipt-total-row">
                <span>Exchange Rate:</span>
                <span>${parseFloat(exchangeRate).toFixed(4)}</span>
            </div>
            ` : ''}
            <div class="receipt-total-row grand-total">
                <span>Total Paid:</span>
                <span>${formatCurrency(amountPaid, currencySymbol)}</span>
            </div>
            ${exchangeRate !== 1 ? `
            <div class="receipt-total-row" style="font-size: 0.9rem; color: #666; margin-top: 5px;">
                <span>(Equivalent to ₱${parseFloat(amountPhp).toFixed(2)})</span>
            </div>
            ` : ''}
        </div>
    `;
    
    if (loyalty.card_number && order.earned_points > 0) {
        html += `
            <div class="receipt-section">
                <h3>Loyalty Points</h3>
                <div class="receipt-info">
                    <div class="receipt-info-item">
                        <label>Card Number</label>
                        <span>${loyalty.card_number}</span>
                    </div>
                    <div class="receipt-info-item">
                        <label>Points Earned</label>
                        <span>${order.earned_points}</span>
                    </div>
                    <div class="receipt-info-item">
                        <label>Total Points</label>
                        <span>${loyalty.points || 0}</span>
                    </div>
                </div>
            </div>
        `;
    }
    
    html += `
        <div class="receipt-footer">
            <p>Thank you for visiting The Waiting Room Café!</p>
            <p>We hope to see you again soon.</p>
        </div>
        
        <div class="receipt-actions">
            <button class="btn btn-print" onclick="window.print()">Print Receipt</button>
            <button class="btn btn-back" onclick="window.location.href='orders.html'">Back to Orders</button>
        </div>
    `;
    
    receiptContent.innerHTML = html;
}

// Fetch receipt
async function fetchReceipt() {
    const receiptContent = document.getElementById('receiptContent');
    
    if (!transactionId && !orderId) {
        receiptContent.innerHTML = '<div class="error">Missing transaction_id or order_id parameter</div>';
        return;
    }
    
    try {
        let url = `${window.API_BASE}/payments/get_receipt?`;
        if (transactionId) {
            url += `transaction_id=${transactionId}`;
        } else {
            url += `order_id=${orderId}`;
        }
        
        const response = await fetch(url, {
            method: 'GET',
            credentials: 'include'
        });
        
        const result = await response.json();
        
        if (!result.success) {
            receiptContent.innerHTML = `<div class="error">Error: ${result.message || 'Failed to load receipt'}</div>`;
            return;
        }
        
        renderReceipt(result.data);
        
    } catch (error) {
        console.error('Error fetching receipt:', error);
        receiptContent.innerHTML = '<div class="error">Error loading receipt. Please try again.</div>';
    }
}

// Initialize when page loads
document.addEventListener('DOMContentLoaded', function() {
    fetchReceipt();
});

