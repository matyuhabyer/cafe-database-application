/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/JavaScript.js to edit this template
 */

// API Base URL
const API_BASE = 'api/';

let cart = JSON.parse(localStorage.getItem("cart") || "[]");
let currencies = [];
let selectedCurrency = 'PHP';
let selectedBranch = 1; // Default branch

const cartItemsDiv = document.getElementById("cartItems");
const placeOrderBtn = document.getElementById("placeOrder");

// Fetch currencies
async function fetchCurrencies() {
    try {
        const response = await fetch(`${API_BASE}menu/get_currencies`);
        const result = await response.json();
        
        if (result.success) {
            currencies = result.data;
            selectedCurrency = currencies.find(c => c.code === 'PHP')?.code || currencies[0]?.code || 'PHP';
            renderCart();
        }
    } catch (error) {
        console.error('Error fetching currencies:', error);
    }
}

// Fetch branches
async function fetchBranches() {
    try {
        const response = await fetch(`${API_BASE}admin/get_branches`, {
            credentials: 'include'
        });
        const result = await response.json();
        
        if (result.success && result.data && result.data.length > 0) {
            selectedBranch = result.data[0].branch_id;
        }
    } catch (error) {
        console.error('Error fetching branches:', error);
    }
}

// Render cart
async function renderCart() {
    if (cart.length === 0) {
        cartItemsDiv.innerHTML = "<p>Your cart is empty.</p>";
        if (placeOrderBtn) placeOrderBtn.style.display = 'none';
        return;
    }
    
    const currency = currencies.find(c => c.code === selectedCurrency) || currencies[0];
    const symbol = currency?.symbol || '₱';
    const rate = currency?.rate || 1;
    
    let total = 0;
    let cartHTML = '<div class="cart-items">';
    
    cart.forEach((item, index) => {
        const itemTotal = item.price * item.quantity * rate;
        total += itemTotal;
        
        cartHTML += `
            <div class="cart-item">
                <div class="cart-item-info">
                    <h4>${item.name}</h4>
                    <p>Quantity: ${item.quantity}</p>
                    ${item.drink_option_id ? '<p>Temperature: Selected</p>' : ''}
                    ${item.extras && item.extras.length > 0 ? `<p>Extras: ${item.extras.map(e => e.name).join(', ')}</p>` : ''}
                </div>
                <div class="cart-item-price">
                    <p>${symbol}${itemTotal.toFixed(2)}</p>
                    <button onclick="removeFromCart(${index})" class="btn-remove">Remove</button>
                </div>
            </div>
        `;
    });
    
    cartHTML += `
        </div>
        <div class="cart-summary">
            <h3>Total: ${symbol}${(total).toFixed(2)}</h3>
        </div>
    `;
    
    cartItemsDiv.innerHTML = cartHTML;
    
    if (placeOrderBtn) {
        placeOrderBtn.style.display = 'block';
    }
}

// Remove item from cart
function removeFromCart(index) {
    cart.splice(index, 1);
    localStorage.setItem("cart", JSON.stringify(cart));
    renderCart();
}

// Place order
async function placeOrder() {
    if (cart.length === 0) {
        alert('Your cart is empty!');
        return;
    }
    
    // Check if user is logged in
    const role = localStorage.getItem("role");
    if (!role || role === 'guest') {
        alert('Please login to place an order');
        window.location.href = 'index.jsp';
        return;
    }
    
    try {
        // Prepare order items
        const items = cart.map(item => ({
            menu_id: item.menu_id,
            quantity: item.quantity,
            price: item.price,
            drink_option_id: item.drink_option_id || null,
            extras: item.extras || []
        }));
        
        // Create order
        const response = await fetch(`${API_BASE}orders/create_order`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify({
                items: items,
                branch_id: selectedBranch
            })
        });
        
        const result = await response.json();
        
        if (result.success) {
            // Clear cart
            cart = [];
            localStorage.removeItem("cart");
            
            alert('Order placed successfully! Order ID: ' + result.data.order_id);
            window.location.href = 'orders.jsp';
        } else {
            alert('Error placing order: ' + result.message);
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Error placing order. Please try again.');
    }
}

// Initialize
if (placeOrderBtn) {
    placeOrderBtn.addEventListener("click", placeOrder);
}

fetchCurrencies();
fetchBranches();
renderCart();

// Make functions globally available
window.removeFromCart = removeFromCart;
window.placeOrder = placeOrder;





