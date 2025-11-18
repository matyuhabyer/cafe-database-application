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
console.log('Cart.js - Base URL:', window.BASE_URL, 'API Base:', window.API_BASE);

let cart = JSON.parse(localStorage.getItem("cart") || "[]");
let currencies = [];
let branches = [];
let selectedCurrency = localStorage.getItem('selectedCurrency') || 'PHP';
let selectedBranch = parseInt(localStorage.getItem('selectedBranch')) || null; // Get from localStorage

const cartItemsDiv = document.getElementById("cartItems");
const placeOrderBtn = document.getElementById("placeOrder");
const currencySelect = document.getElementById("currency");
let checkoutCurrencySelect = null;
let paymentMethodSelect = null;
let branchSelect = null;

function resolveImageUrl(imageUrl) {
    if (!imageUrl) return null;
    let trimmed = String(imageUrl).trim();
    if (!trimmed) return null;
    if (/^(https?:|data:|blob:)/i.test(trimmed)) {
        return trimmed;
    }
    trimmed = trimmed.replace(/^\.\/+/, '');
    if (trimmed.startsWith('/')) {
        return trimmed;
    }
    if (trimmed.startsWith('assets/')) {
        return trimmed;
    }
    return `assets/images/${trimmed}`;
}

function escapeAttribute(value) {
    if (value === undefined || value === null) return '';
    const strValue = String(value);
    const escapeMap = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
    return strValue.replace(/[&<>"']/g, (char) => escapeMap[char] || char);
}

function getCartImageMarkup(item) {
    const resolvedSrc = resolveImageUrl(item.image_url);
    if (resolvedSrc) {
        const alt = escapeAttribute(item.name || 'Cart item');
        return `<img src="${resolvedSrc}" alt="${alt}" loading="lazy" decoding="async">`;
    }
    return '<span class="cart-item-placeholder">🍽️</span>';
}

// Get checkout elements when DOM is ready
function getCheckoutElements() {
    if (!branchSelect) {
        branchSelect = document.getElementById("branchSelect");
    }
    if (!checkoutCurrencySelect) {
        checkoutCurrencySelect = document.getElementById("checkoutCurrency");
    }
    if (!paymentMethodSelect) {
        paymentMethodSelect = document.getElementById("paymentMethod");
    }
}

// Fetch currencies
async function fetchCurrencies() {
    try {
        const response = await fetch(`${window.API_BASE}/menu/get_currencies`);
        const result = await response.json();
        
        if (result.success && result.data) {
            currencies = result.data;
            // Use stored currency or default to PHP
            const storedCurrency = localStorage.getItem('selectedCurrency');
            selectedCurrency = storedCurrency || currencies.find(c => c.code === 'PHP')?.code || currencies[0]?.code || 'PHP';
            if (!storedCurrency) {
                localStorage.setItem('selectedCurrency', selectedCurrency);
            }
            // Update currency selector
            if (currencySelect) {
                currencySelect.value = selectedCurrency;
            }
            // Update checkout currency selector (get element if not already found)
            getCheckoutElements();
            if (checkoutCurrencySelect) {
                checkoutCurrencySelect.innerHTML = currencies.map(c => 
                    `<option value="${c.currency_id}" data-code="${c.code}" data-symbol="${c.symbol}" data-rate="${c.rate}">${c.code} - ${c.symbol}</option>`
                ).join('');
                // Set default to current selected currency
                const defaultCurrency = currencies.find(c => c.code === selectedCurrency);
                if (defaultCurrency) {
                    checkoutCurrencySelect.value = defaultCurrency.currency_id;
                } else if (currencies.length > 0) {
                    checkoutCurrencySelect.value = currencies[0].currency_id;
                }
                
                // Remove old event listener if exists before adding new one (clear all)
                if (checkoutCurrencySelect.hasAttribute('data-listener-added')) {
                    // Clone the element to remove all event listeners
                    const newSelect = checkoutCurrencySelect.cloneNode(true);
                    checkoutCurrencySelect.parentNode.replaceChild(newSelect, checkoutCurrencySelect);
                    checkoutCurrencySelect = newSelect;
                    getCheckoutElements(); // Update reference
                }
                
                // Add event listener for currency change to update prices
                checkoutCurrencySelect.addEventListener('change', function() {
                    console.log('Checkout currency changed!');
                    const selectedOption = checkoutCurrencySelect.options[checkoutCurrencySelect.selectedIndex];
                    if (selectedOption && selectedOption.dataset.code) {
                        const newCurrencyCode = selectedOption.dataset.code;
                        const newRate = parseFloat(selectedOption.dataset.rate);
                        const newSymbol = selectedOption.dataset.symbol;
                        console.log('New currency:', newCurrencyCode, 'Rate:', newRate, 'Symbol:', newSymbol);
                        selectedCurrency = newCurrencyCode;
                        localStorage.setItem('selectedCurrency', newCurrencyCode);
                        // Sync currency selector in header if it exists
                        if (currencySelect) {
                            currencySelect.value = newCurrencyCode;
                        }
                        // Re-render cart immediately with new currency
                        renderCart();
                    }
                });
                checkoutCurrencySelect.setAttribute('data-listener-added', 'true');
            } else {
                console.warn('checkoutCurrencySelect not found, will retry when cart summary is shown');
            }
            // Render cart with updated currency
            renderCart();
        }
    } catch (error) {
        console.error('Error fetching currencies:', error);
    }
}

// Fetch branches - public endpoint for customers
async function fetchBranches() {
    try {
        const response = await fetch(`${window.API_BASE}/menu/get_branches`);
        const result = await response.json();
        
        if (result.success && result.data && result.data.length > 0) {
            branches = result.data;
            
            // Populate branch dropdown
            getCheckoutElements();
            if (branchSelect) {
                branchSelect.innerHTML = branches.map(branch => 
                    `<option value="${branch.branch_id}">${branch.name} - ${branch.address || ''}</option>`
                ).join('');
                
                // Set selected branch from localStorage or default to first branch
                if (selectedBranch && branches.find(b => b.branch_id === selectedBranch)) {
                    branchSelect.value = selectedBranch;
                } else if (branches.length > 0) {
                    selectedBranch = branches[0].branch_id;
                    branchSelect.value = selectedBranch;
                    localStorage.setItem('selectedBranch', selectedBranch);
                }
                
                // Add event listener to update selectedBranch when user changes selection
                branchSelect.addEventListener('change', function() {
                    selectedBranch = parseInt(branchSelect.value);
                    localStorage.setItem('selectedBranch', selectedBranch);
                });
            } else {
                // If branchSelect not found yet, set default
                if (branches.length > 0 && !selectedBranch) {
                    selectedBranch = branches[0].branch_id;
                    localStorage.setItem('selectedBranch', selectedBranch);
                }
            }
        } else {
            console.error('No branches found');
            if (branchSelect) {
                branchSelect.innerHTML = '<option value="">No branches available</option>';
            }
        }
    } catch (error) {
        console.error('Error fetching branches:', error);
        if (branchSelect) {
            branchSelect.innerHTML = '<option value="">Error loading branches</option>';
        }
    }
}

// Render cart
async function renderCart() {
    // Make sure we have currencies loaded
    if (currencies.length === 0) {
        console.log('Waiting for currencies to load...');
        return;
    }
    
    if (cart.length === 0) {
        cartItemsDiv.innerHTML = `
            <div class="empty-cart">
                <h3>Your cart is empty</h3>
                <p>Start adding items to your cart</p>
                <a href="products.html">Browse Menu</a>
            </div>
        `;
        const cartSummary = document.getElementById('cartSummary');
        if (cartSummary) cartSummary.style.display = 'none';
        return;
    }
    
    // Get checkout elements (including branch select)
    getCheckoutElements();
    
    // Ensure branches are loaded
    if (branches.length === 0) {
        await fetchBranches();
    }
    
    // Ensure checkout currency selector is populated BEFORE we try to read from it
    if (checkoutCurrencySelect && currencies.length > 0) {
        const needsPopulation = checkoutCurrencySelect.options.length <= 1 || 
                               checkoutCurrencySelect.options[0].value === '' ||
                               checkoutCurrencySelect.options[0].text.includes('Loading');
        
        if (needsPopulation) {
            checkoutCurrencySelect.innerHTML = currencies.map(c => 
                `<option value="${c.currency_id}" data-code="${c.code}" data-symbol="${c.symbol}" data-rate="${c.rate}">${c.code} - ${c.symbol}</option>`
            ).join('');
            // Set default to current selected currency
            const defaultCurrency = currencies.find(c => c.code === selectedCurrency);
            if (defaultCurrency) {
                checkoutCurrencySelect.value = defaultCurrency.currency_id;
            } else if (currencies.length > 0) {
                checkoutCurrencySelect.value = currencies[0].currency_id;
            }
        }
        
        // Note: Event listener is added in fetchCurrencies() to ensure it's always attached
        // If it wasn't added there, add it here as a fallback
        if (!checkoutCurrencySelect.hasAttribute('data-listener-added')) {
            console.log('Adding checkout currency event listener in renderCart()');
            checkoutCurrencySelect.addEventListener('change', function() {
                console.log('Checkout currency changed (from renderCart listener)!');
                const selectedOption = checkoutCurrencySelect.options[checkoutCurrencySelect.selectedIndex];
                if (selectedOption && selectedOption.dataset.code) {
                    const newCurrencyCode = selectedOption.dataset.code;
                    const newRate = parseFloat(selectedOption.dataset.rate);
                    const newSymbol = selectedOption.dataset.symbol;
                    console.log('New currency:', newCurrencyCode, 'Rate:', newRate, 'Symbol:', newSymbol);
                    selectedCurrency = newCurrencyCode;
                    localStorage.setItem('selectedCurrency', newCurrencyCode);
                    // Sync currency selector in header if it exists
                    if (currencySelect) {
                        currencySelect.value = newCurrencyCode;
                    }
                }
                // Re-render cart with new currency
                renderCart();
            });
            checkoutCurrencySelect.setAttribute('data-listener-added', 'true');
        }
    }
    
    // Use checkout currency selector for display if available and selected
    // Otherwise fall back to stored currency
    let displayCurrency = null;
    let displayCurrencyCode = null;
    
    // Always check checkout currency selector first if it's available and has a value
    if (checkoutCurrencySelect && checkoutCurrencySelect.value && checkoutCurrencySelect.selectedIndex >= 0) {
        // Use checkout currency selector for display
        const selectedOption = checkoutCurrencySelect.options[checkoutCurrencySelect.selectedIndex];
        console.log('Reading checkout currency selector:', {
            value: checkoutCurrencySelect.value,
            selectedIndex: checkoutCurrencySelect.selectedIndex,
            option: selectedOption,
            hasDataCode: selectedOption && selectedOption.dataset.code
        });
        
        if (selectedOption && selectedOption.dataset.code) {
            displayCurrencyCode = selectedOption.dataset.code;
            const rate = parseFloat(selectedOption.dataset.rate);
            const symbol = selectedOption.dataset.symbol;
            
            console.log('Using checkout currency for display:', displayCurrencyCode, 'Rate:', rate, 'Symbol:', symbol);
            
            if (!isNaN(rate) && symbol) {
                displayCurrency = {
                    currency_id: parseInt(checkoutCurrencySelect.value),
                    code: displayCurrencyCode,
                    symbol: symbol,
                    rate: rate
                };
                
                // Update stored currency when checkout currency changes
                if (displayCurrencyCode !== selectedCurrency) {
                    selectedCurrency = displayCurrencyCode;
                    localStorage.setItem('selectedCurrency', selectedCurrency);
                    // Sync currency selector in header if it exists
                    if (currencySelect) {
                        currencySelect.value = selectedCurrency;
                    }
                }
            } else {
                console.warn('Invalid currency data in checkout selector:', {
                    rate: rate,
                    symbol: symbol,
                    option: selectedOption
                });
            }
        } else {
            console.warn('Selected option missing or missing data-code:', selectedOption);
        }
    } else {
        console.log('Checkout currency selector not available:', {
            exists: !!checkoutCurrencySelect,
            hasValue: checkoutCurrencySelect ? !!checkoutCurrencySelect.value : false,
            selectedIndex: checkoutCurrencySelect ? checkoutCurrencySelect.selectedIndex : -1
        });
    }
    
    // Fallback to stored currency if checkout currency not selected
    if (!displayCurrency) {
        const currentStoredCurrency = localStorage.getItem('selectedCurrency') || selectedCurrency || 'PHP';
        selectedCurrency = currentStoredCurrency;
        
        const currency = currencies.find(c => c.code === selectedCurrency);
        if (!currency) {
            console.error('Currency not found:', selectedCurrency);
            // Fallback to first available currency
            selectedCurrency = currencies[0]?.code || 'PHP';
            localStorage.setItem('selectedCurrency', selectedCurrency);
            if (currencySelect) {
                currencySelect.value = selectedCurrency;
            }
        }
        
        displayCurrency = currency || currencies[0] || { symbol: '₱', rate: 1 };
    }
    
    const symbol = displayCurrency.symbol || '₱';
    const rate = displayCurrency.rate || 1;
    
    console.log('Rendering cart with currency:', displayCurrency.code, 'Rate:', rate, 'Symbol:', symbol);
    console.log('Cart items:', cart.map(item => ({ name: item.name, price: item.price, quantity: item.quantity })));
    
    let total = 0;
    let cartHTML = '<div class="cart-items">';
    
    cart.forEach((item, index) => {
        // Cart items are stored in PHP base currency
        // Convert from PHP base to display currency by multiplying by rate
        const itemTotal = item.price * item.quantity * rate;
        console.log(`Item ${item.name}: PHP ${item.price} x ${item.quantity} x ${rate} = ${displayCurrency.code} ${itemTotal.toFixed(2)}`);
        total += itemTotal;
        
        const imageMarkup = getCartImageMarkup(item);
        cartHTML += `
            <div class="cart-item">
                <div class="cart-item-image">
                    ${imageMarkup}
                </div>
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
    
    cartHTML += '</div>';
    cartItemsDiv.innerHTML = cartHTML;
    
    // Update summary
    const cartSummary = document.getElementById('cartSummary');
    const cartTotal = document.getElementById('cartTotal');
    if (cartSummary) {
        cartSummary.style.display = 'block';
        
        // Now that cart summary is visible, ensure checkout currency selector is populated
        // This is important for first-time cart loads when the selector might not have been initialized yet
        getCheckoutElements(); // Refresh references since DOM is now visible
        if (checkoutCurrencySelect && currencies.length > 0) {
            // Check if selector needs population (has loading option or is empty)
            const needsPopulation = !checkoutCurrencySelect.options || 
                                   checkoutCurrencySelect.options.length <= 1 || 
                                   checkoutCurrencySelect.options[0].value === '' ||
                                   checkoutCurrencySelect.options[0].text.includes('Loading');
            
            if (needsPopulation) {
                console.log('Populating checkout currency selector in cart summary');
                checkoutCurrencySelect.innerHTML = currencies.map(c => 
                    `<option value="${c.currency_id}" data-code="${c.code}" data-symbol="${c.symbol}" data-rate="${c.rate}">${c.code} - ${c.symbol}</option>`
                ).join('');
                // Set default to current selected currency
                const defaultCurrency = currencies.find(c => c.code === selectedCurrency);
                if (defaultCurrency) {
                    checkoutCurrencySelect.value = defaultCurrency.currency_id;
                } else if (currencies.length > 0) {
                    checkoutCurrencySelect.value = currencies[0].currency_id;
                }
            }
            
            // Ensure event listener is attached (critical for first-time loads)
            if (!checkoutCurrencySelect.hasAttribute('data-listener-added')) {
                console.log('Adding checkout currency event listener in cart summary');
                checkoutCurrencySelect.addEventListener('change', function() {
                    console.log('Checkout currency changed (from cart summary listener)!');
                    const selectedOption = checkoutCurrencySelect.options[checkoutCurrencySelect.selectedIndex];
                    if (selectedOption && selectedOption.dataset.code) {
                        const newCurrencyCode = selectedOption.dataset.code;
                        const newRate = parseFloat(selectedOption.dataset.rate);
                        const newSymbol = selectedOption.dataset.symbol;
                        console.log('New currency:', newCurrencyCode, 'Rate:', newRate, 'Symbol:', newSymbol);
                        selectedCurrency = newCurrencyCode;
                        localStorage.setItem('selectedCurrency', newCurrencyCode);
                        // Sync currency selector in header if it exists
                        if (currencySelect) {
                            currencySelect.value = newCurrencyCode;
                        }
                    }
                    // Re-render cart with new currency
                    renderCart();
                });
                checkoutCurrencySelect.setAttribute('data-listener-added', 'true');
            }
        }
    }
    if (cartTotal) {
        cartTotal.textContent = `Total: ${symbol}${total.toFixed(2)}`;
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
        window.location.href = 'index.html';
        return;
    }
    
    // Get checkout elements before validation
    getCheckoutElements();
    
    // Validate checkout options
    if (!branchSelect || !branchSelect.value) {
        alert('Please select a branch');
        return;
    }
    
    if (!checkoutCurrencySelect || !checkoutCurrencySelect.value) {
        alert('Please select a payment currency');
        return;
    }
    
    if (!paymentMethodSelect || !paymentMethodSelect.value) {
        alert('Please select a payment method');
        return;
    }
    
    // Get selected branch
    selectedBranch = parseInt(branchSelect.value);
    
    // Get selected currency and payment method
    const selectedCurrencyOption = checkoutCurrencySelect.options[checkoutCurrencySelect.selectedIndex];
    const currencyId = parseInt(checkoutCurrencySelect.value);
    const currencyCode = selectedCurrencyOption.dataset.code;
    const currencySymbol = selectedCurrencyOption.dataset.symbol;
    const currencyRate = parseFloat(selectedCurrencyOption.dataset.rate);
    const paymentMethod = paymentMethodSelect.value;
    
    // Disable button during processing
    if (placeOrderBtn) {
        placeOrderBtn.disabled = true;
        placeOrderBtn.textContent = 'Processing...';
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
        const orderResponse = await fetch(`${window.API_BASE}/orders/create_order`, {
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
        
        const orderResult = await orderResponse.json();
        
        if (!orderResult.success) {
            alert('Error creating order: ' + orderResult.message);
            if (placeOrderBtn) {
                placeOrderBtn.disabled = false;
                placeOrderBtn.textContent = 'Place Order';
            }
            return;
        }
        
        const orderId = orderResult.data.order_id;
        const totalAmount = orderResult.data.total_amount;
        
        // Calculate amount in selected currency
        const amountInCurrency = totalAmount * currencyRate;
        
        // Create transaction
        const transactionResponse = await fetch(`${window.API_BASE}/payments/create_transaction`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify({
                order_id: orderId,
                currency_id: currencyId,
                payment_method: paymentMethod,
                amount_paid: amountInCurrency
            })
        });
        
        const transactionResult = await transactionResponse.json();
        
        if (!transactionResult.success) {
            alert('Order created but payment failed: ' + transactionResult.message);
            if (placeOrderBtn) {
                placeOrderBtn.disabled = false;
                placeOrderBtn.textContent = 'Place Order';
            }
            return;
        }
        
        // Clear cart
        cart = [];
        localStorage.removeItem("cart");
        
        // Redirect to receipt page
        const transactionId = transactionResult.data.transaction_id;
        window.location.href = `receipt.html?transaction_id=${transactionId}`;
        
    } catch (error) {
        console.error('Error:', error);
        alert('Error placing order. Please try again.');
        if (placeOrderBtn) {
            placeOrderBtn.disabled = false;
            placeOrderBtn.textContent = 'Place Order';
        }
    }
}

// Currency change handler
if (currencySelect) {
    // Set initial value immediately from localStorage to ensure consistency
    const storedCurrency = localStorage.getItem('selectedCurrency') || 'PHP';
    selectedCurrency = storedCurrency;
    currencySelect.value = selectedCurrency;
    
    currencySelect.addEventListener("change", (e) => {
        selectedCurrency = e.target.value;
        localStorage.setItem('selectedCurrency', selectedCurrency);
        renderCart();
    });
}

// Listen for storage changes (when currency changes in another tab/page)
window.addEventListener('storage', function(e) {
    if (e.key === 'selectedCurrency') {
        selectedCurrency = e.newValue || 'PHP';
        if (currencySelect) {
            currencySelect.value = selectedCurrency;
        }
        // Re-fetch currencies to get updated rates, then render
        fetchCurrencies();
    }
});

// Also check for currency changes in the same window
// This handles cases where currency is changed in products page and user navigates to cart
function checkCurrencyUpdate() {
    const storedCurrency = localStorage.getItem('selectedCurrency');
    if (storedCurrency && storedCurrency !== selectedCurrency) {
        selectedCurrency = storedCurrency;
        if (currencySelect) {
            currencySelect.value = selectedCurrency;
        }
        if (currencies.length > 0) {
            renderCart();
        }
    }
}

// Initialize
if (placeOrderBtn) {
    placeOrderBtn.addEventListener("click", placeOrder);
}

// Fetch currencies and branches first, then render cart
Promise.all([
    fetchCurrencies(),
    fetchBranches()
]).then(() => {
    // After currencies and branches are loaded, check for any currency updates
    checkCurrencyUpdate();
    // Sync currency selector with stored currency
    const storedCurrency = localStorage.getItem('selectedCurrency');
    if (storedCurrency && currencySelect) {
        selectedCurrency = storedCurrency;
        currencySelect.value = selectedCurrency;
    }
    renderCart();
});

// Check for currency updates periodically (in case user navigates from products page)
setInterval(checkCurrencyUpdate, 500);

// Make functions globally available
window.removeFromCart = removeFromCart;
window.placeOrder = placeOrder;





