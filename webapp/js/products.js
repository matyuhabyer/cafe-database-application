/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/JavaScript.js to edit this template
 */

// Get the context path - same method as test-servlets.html
// Only declare if not already declared (to avoid conflicts with inline scripts)
if (typeof window.BASE_URL === 'undefined') {
    // Get the pathname and remove the filename to get the base path
    const pathname = window.location.pathname;
    const basePath = pathname.substring(0, pathname.lastIndexOf('/'));
    window.BASE_URL = window.location.origin + basePath;
    window.API_BASE = window.BASE_URL + '/api';
}
// Use window properties directly to avoid any redeclaration issues
// Access via window to avoid variable conflicts
console.log('Products.js - Base URL:', window.BASE_URL, 'API Base:', window.API_BASE);
console.log('Current location:', window.location.href);

// Global variables
let menuData = [];
let currentCurrency = localStorage.getItem('selectedCurrency') || 'PHP';
let cart = JSON.parse(localStorage.getItem("cart") || "[]");

let list = null;
let currencySelect = null;

// Fetch menu from API
async function fetchMenu(currency = 'PHP') {
    try {
        // Ensure elements are available
        if (!list) {
            list = document.getElementById("productList");
        }
        if (!list) {
            console.error('productList element not found');
            return;
        }
        
        console.log('Fetching menu with currency:', currency);
        console.log('API URL:', `${window.API_BASE}/menu/get_menu?currency=${currency}`);
        
        const apiUrl = `${window.API_BASE}/menu/get_menu?currency=${currency}`;
        console.log('Fetching from URL:', apiUrl);
        
        const response = await fetch(apiUrl);
        
        console.log('Response status:', response.status, response.statusText);
        console.log('Response headers:', Object.fromEntries(response.headers.entries()));
        
        if (!response.ok) {
            const errorText = await response.text();
            console.error('HTTP Error Response:', errorText);
            list.innerHTML = `<p class="error-message">Error loading menu: ${response.status} ${response.statusText}<br>${errorText.substring(0, 200)}</p>`;
            return;
        }
        
        const result = await response.json();
        console.log('Menu API Response:', result);
        console.log('Response structure:', {
            hasSuccess: 'success' in result,
            successValue: result.success,
            hasData: 'data' in result,
            dataType: result.data ? typeof result.data : 'null',
            dataKeys: result.data ? Object.keys(result.data) : []
        });
        
        // Handle response - check if data is directly in result or in result.data
        let dataToUse = null;
        if (result.success && result.data) {
            dataToUse = result.data;
        } else if (result.categories || result.currency) {
            // Fallback: if response structure is different, use result directly
            console.warn('Using direct result data (unexpected structure)');
            dataToUse = result;
        }
        
        if (dataToUse) {
            menuData = dataToUse;
            currentCurrency = currency;
            localStorage.setItem('selectedCurrency', currency); // Persist currency
            console.log('Menu data loaded:', {
                currency: menuData.currency,
                categoriesCount: menuData.categories ? menuData.categories.length : 0,
                totalItems: menuData.categories ? menuData.categories.reduce((sum, cat) => sum + (cat.items ? cat.items.length : 0), 0) : 0
            });
            
            // Verify we have categories
            if (!menuData.categories || menuData.categories.length === 0) {
                console.warn('No categories in menu data');
                list.innerHTML = '<p class="error-message">No menu items found in database. Please add items to the menu.</p>';
                return;
            }
            
            renderProducts();
        } else {
            console.error('Error fetching menu:', result.message || 'Unknown error', result);
            list.innerHTML = `<p class="error-message">Error loading menu: ${result.message || 'Invalid response format. Please check console for details.'}</p>`;
        }
    } catch (error) {
        console.error('Error fetching menu:', error);
        if (list) {
            list.innerHTML = `<p class="error-message">Error connecting to server. Please check your connection and try again.</p>`;
        }
    }
}

// Render products by category
function renderProducts() {
    // Ensure list element is available
    if (!list) {
        list = document.getElementById("productList");
    }
    if (!list) {
        console.error('productList element not found');
        return;
    }
    
    if (!menuData) {
        console.error('Menu data is not available');
        list.innerHTML = '<p class="error-message">Menu data not loaded. Please refresh the page.</p>';
        return;
    }
    
    if (!menuData.categories || menuData.categories.length === 0) {
        console.warn('No categories found in menu data', menuData);
        list.innerHTML = '<p class="error-message">No menu items available. Please check if there are items in the database.</p>';
        return;
    }
    
    list.innerHTML = "";
    const symbol = menuData.currency?.symbol || '₱';
    
    let totalItems = 0;
    let categoriesWithItems = 0;
    
    menuData.categories.forEach((category, index) => {
        console.log(`Category ${index}:`, category.name, 'Items:', category.items ? category.items.length : 0);
        
        if (category.items && category.items.length > 0) {
            categoriesWithItems++;
            totalItems += category.items.length;
            const categorySection = document.createElement('div');
            categorySection.className = 'category-section';
            categorySection.innerHTML = `<h3 class="category-title">${category.name}</h3>`;
            
            const itemsContainer = document.createElement('div');
            itemsContainer.className = 'product-grid';
            
            category.items.forEach(item => {
                const price = parseFloat(item.price_amount || 0).toFixed(2);
                const legends = (item.legends && item.legends.length > 0) 
                    ? item.legends.map(l => `<span class="legend" title="${l.description || ''}">${l.code || ''}</span>`).join('')
                    : '';
                
                // Get emoji based on category or item type
                const emoji = getCategoryEmoji(category.name, item.is_drink);
                
                const productCard = document.createElement('div');
                productCard.className = 'product';
                productCard.innerHTML = `
                    <div class="product-image">
                        ${emoji}
                    </div>
                    <div class="product-body">
                        <div class="product-header">
                            <h4>${item.name || 'Unnamed Item'}${legends}</h4>
                        </div>
                        <p class="product-description">${item.description || 'Delicious item from our menu.'}</p>
                        <div class="product-price-section">
                            <span class="product-price">${symbol}${price}</span>
                        </div>
                        <div class="product-actions">
                            <button onclick="quickAddToCart(${item.menu_id})" class="btn-add-to-cart">
                                <span>🛒</span> Add to Cart
                            </button>
                            <button onclick="viewProductDetails(${item.menu_id})" class="btn-view">View Details</button>
                        </div>
                    </div>
                `;
                itemsContainer.appendChild(productCard);
            });
            
            categorySection.appendChild(itemsContainer);
            list.appendChild(categorySection);
        }
    });
    
    if (totalItems === 0) {
        console.error('No items found in any category', {
            categoriesCount: menuData.categories.length,
            categories: menuData.categories.map(c => ({
                name: c.name,
                itemsCount: c.items ? c.items.length : 0
            }))
        });
        list.innerHTML = '<p class="error-message">No menu items available in any category. Please add items to the database.</p>';
    } else {
        console.log(`✅ Rendered ${totalItems} menu items across ${categoriesWithItems} categories (out of ${menuData.categories.length} total categories)`);
    }
}

// View product details - make it globally accessible
window.viewProductDetails = async function(menuId) {
    try {
        const response = await fetch(`${window.API_BASE}/menu/get_item?menu_id=${menuId}&currency=${currentCurrency}`);
        const result = await response.json();
        
        if (result.success) {
            const item = result.data;
            showProductModal(item);
        } else {
            alert('Error loading product details: ' + result.message);
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Error loading product details');
    }
}

// Show product modal with customization options
function showProductModal(item) {
    const symbol = item.currency_symbol || '₱';
    const basePrice = item.price_amount;
    const emoji = item.is_drink ? '☕' : '🍽️';
    
    let modalHTML = `
        <div class="modal" id="productModal">
            <div class="modal-content">
                <span class="close" onclick="closeModal()">&times;</span>
                <div style="text-align: center; font-size: 4rem; margin-bottom: 15px;">${emoji}</div>
                <h2>${item.name}</h2>
                <p>${item.description || 'Delicious item from our menu.'}</p>
                <p style="color: #6d4c41; font-size: 1.2rem; font-weight: 600; margin: 15px 0;">
                    Base Price: ${symbol}${basePrice.toFixed(2)}
                </p>
    `;
    
    // Drink options (temperature)
    if (item.is_drink && item.drink_options && item.drink_options.length > 0) {
        modalHTML += `
            <div class="customization-section">
                <label>Temperature:</label>
                <select id="drinkOption">
                    ${item.drink_options.map(opt => 
                        `<option value="${opt.drink_option_id}" data-modifier="${opt.price_modifier}">
                            ${opt.temperature.charAt(0).toUpperCase() + opt.temperature.slice(1)} 
                            ${opt.price_modifier > 0 ? `(+${symbol}${opt.price_modifier.toFixed(2)})` : ''}
                        </option>`
                    ).join('')}
                </select>
            </div>
        `;
    }
    
    // Extras
    if (item.available_extras && item.available_extras.length > 0) {
        modalHTML += `
            <div class="customization-section">
                <label>Extras:</label>
                <div id="extrasList">
                    ${item.available_extras.map(extra => 
                        `<label>
                            <input type="checkbox" value="${extra.extra_id}" data-price="${extra.price}" data-name="${extra.name}">
                            ${extra.name} (+${symbol}${extra.price.toFixed(2)})
                        </label>`
                    ).join('')}
                </div>
            </div>
        `;
    }
    
    modalHTML += `
            <div class="customization-section">
                <label>Quantity:</label>
                <input type="number" id="itemQuantity" min="1" value="1">
            </div>
            <div class="price-summary">
                <p><strong>Total: <span id="itemTotal">${symbol}${basePrice.toFixed(2)}</span></strong></p>
            </div>
            <button onclick="addToCartFromModal(${item.menu_id})" class="btn-add-cart">
                <span>🛒</span> Add to Cart
            </button>
        </div>
    </div>
    `;
    
    document.body.insertAdjacentHTML('beforeend', modalHTML);
    
    // Show modal
    const modal = document.getElementById('productModal');
    if (modal) {
        modal.classList.add('show');
        
        // Close modal when clicking outside
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                closeModal();
            }
        });
    }
    
    // Update total when options change
    const drinkOption = document.getElementById('drinkOption');
    const extrasList = document.getElementById('extrasList');
    const quantityInput = document.getElementById('itemQuantity');
    
    function updateTotal() {
        let total = basePrice;
        
        // Add drink option modifier
        if (drinkOption) {
            const selectedOption = drinkOption.options[drinkOption.selectedIndex];
            const modifier = parseFloat(selectedOption.dataset.modifier) || 0;
            total += modifier;
        }
        
        // Add extras
        if (extrasList) {
            extrasList.querySelectorAll('input[type="checkbox"]:checked').forEach(checkbox => {
                total += parseFloat(checkbox.dataset.price);
            });
        }
        
        // Multiply by quantity
        const quantity = parseInt(quantityInput.value) || 1;
        total *= quantity;
        
        document.getElementById('itemTotal').textContent = `${symbol}${total.toFixed(2)}`;
    }
    
    if (drinkOption) drinkOption.addEventListener('change', updateTotal);
    if (extrasList) extrasList.addEventListener('change', updateTotal);
    if (quantityInput) quantityInput.addEventListener('input', updateTotal);
}

// Close modal - make it globally accessible
window.closeModal = function() {
    const modal = document.getElementById('productModal');
    if (modal) {
        modal.classList.remove('show');
        setTimeout(() => {
            modal.remove();
        }, 300);
    }
}

// Add to cart from modal - make it globally accessible
window.addToCartFromModal = function(menuId) {
    const quantity = parseInt(document.getElementById('itemQuantity').value) || 1;
    const drinkOption = document.getElementById('drinkOption');
    const drinkOptionId = drinkOption ? parseInt(drinkOption.value) : null;
    const extrasList = document.getElementById('extrasList');
    
    const extras = [];
    if (extrasList) {
        extrasList.querySelectorAll('input[type="checkbox"]:checked').forEach(checkbox => {
            extras.push({
                extra_id: parseInt(checkbox.value),
                name: checkbox.dataset.name,
                quantity: 1
            });
        });
    }
    
    // Calculate price (simplified - in production, recalculate from API)
    const item = findItemInMenu(menuId);
    if (!item) {
        alert('Item not found');
        return;
    }
    
    let price = item.price_amount;
    
    // Add drink option modifier
    if (drinkOption) {
        const selectedOption = drinkOption.options[drinkOption.selectedIndex];
        const modifier = parseFloat(selectedOption.dataset.modifier) || 0;
        price += modifier;
    }
    
    // Add extras
    extras.forEach(extra => {
        const extraData = item.available_extras.find(e => e.extra_id === extra.extra_id);
        if (extraData) {
            price += extraData.price;
        }
    });
    
    const cartItem = {
        menu_id: menuId,
        name: item.name,
        price: price,
        quantity: quantity,
        drink_option_id: drinkOptionId,
        extras: extras
    };
    
    cart.push(cartItem);
    localStorage.setItem("cart", JSON.stringify(cart));
    
    closeModal();
    showCartNotification(`${item.name} added to cart!`);
}

// Helper function to find item in menu data
function findItemInMenu(menuId) {
    if (!menuData.categories) return null;
    
    for (const category of menuData.categories) {
        if (category.items) {
            const item = category.items.find(i => i.menu_id === menuId);
            if (item) return item;
        }
    }
    return null;
}

// Get emoji based on category
function getCategoryEmoji(categoryName, isDrink) {
    if (isDrink) {
        return '☕';
    }
    const categoryLower = categoryName.toLowerCase();
    if (categoryLower.includes('breakfast') || categoryLower.includes('rice')) {
        return '🍳';
    } else if (categoryLower.includes('pasta')) {
        return '🍝';
    } else if (categoryLower.includes('sandwich')) {
        return '🥪';
    } else if (categoryLower.includes('salad')) {
        return '🥗';
    } else if (categoryLower.includes('coffee')) {
        return '☕';
    } else if (categoryLower.includes('tea')) {
        return '🍵';
    } else if (categoryLower.includes('fizzy') || categoryLower.includes('drink')) {
        return '🥤';
    } else {
        return '🍽️';
    }
}

// Quick add to cart (adds with default options)
window.quickAddToCart = function(menuId) {
    const item = findItemInMenu(menuId);
    if (!item) {
        alert('Item not found');
        return;
    }
    
    // For drinks, show modal for customization
    // For non-drinks, add directly to cart
    if (item.is_drink && item.drink_options && item.drink_options.length > 0) {
        // Show modal for drinks to select temperature
        viewProductDetails(menuId);
    } else {
        // Add directly to cart for non-drinks
        const cartItem = {
            menu_id: menuId,
            name: item.name,
            price: item.price_amount,
            quantity: 1,
            drink_option_id: null,
            extras: []
        };
        
        cart.push(cartItem);
        localStorage.setItem("cart", JSON.stringify(cart));
        
        // Show success notification
        showCartNotification(`${item.name} added to cart!`);
    }
}

// Show cart notification
function showCartNotification(message) {
    // Create notification element
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #6d4c41;
        color: white;
        padding: 15px 25px;
        border-radius: 10px;
        box-shadow: 0 4px 15px rgba(0,0,0,0.2);
        z-index: 10000;
        animation: slideInRight 0.3s ease;
        font-weight: 600;
    `;
    notification.textContent = message;
    
    // Add animation
    const style = document.createElement('style');
    style.textContent = `
        @keyframes slideInRight {
            from {
                transform: translateX(100%);
                opacity: 0;
            }
            to {
                transform: translateX(0);
                opacity: 1;
            }
        }
    `;
    document.head.appendChild(style);
    
    document.body.appendChild(notification);
    
    // Remove after 3 seconds
    setTimeout(() => {
        notification.style.animation = 'slideInRight 0.3s ease reverse';
        setTimeout(() => {
            notification.remove();
            style.remove();
        }, 300);
    }, 3000);
}

// Initialize
function initializeProducts() {
    try {
        // Get elements
        list = document.getElementById("productList");
        currencySelect = document.getElementById("currency");
        
        if (!list) {
            console.error('productList element not found, retrying...');
            setTimeout(initializeProducts, 100);
            return;
        }
        
        console.log('Product list element found:', list);
        
        // Initialize currency selector
        if (currencySelect) {
            console.log('Currency selector found:', currencySelect);
            // Set initial currency from localStorage
            currencySelect.value = currentCurrency;
            
            // Remove existing listeners to avoid duplicates
            const newSelect = currencySelect.cloneNode(true);
            currencySelect.parentNode.replaceChild(newSelect, currencySelect);
            currencySelect = newSelect;
            
            currencySelect.addEventListener("change", (e) => {
                const newCurrency = e.target.value;
                localStorage.setItem('selectedCurrency', newCurrency);
                fetchMenu(newCurrency);
            });
        } else {
            console.warn('Currency selector not found');
        }
        
        // Fetch menu
        console.log('Initializing products page, fetching menu with currency:', currentCurrency);
        fetchMenu(currentCurrency);
    } catch (error) {
        console.error('Error initializing products:', error);
        if (list) {
            list.innerHTML = '<p class="error-message">Error initializing page. Please refresh.</p>';
        }
    }
}

// Initialize when DOM is ready
// Load dom-utils.js first, then use it to ensure elements are ready
if (typeof window.initializeWhenReady === 'function') {
    // Use the utility function to ensure DOM is ready
    window.initializeWhenReady(
        initializeProducts,
        ['productList'], // Required element
        {
            waitForAccessControl: false, // Don't wait for access control (public page)
            timeout: 5000
        }
    );
} else {
    // Fallback if dom-utils.js isn't loaded yet
    async function startInitialization() {
        // Start access control check in parallel (non-blocking for public pages)
        if (typeof window.applyPageAccessControl === 'function') {
            window.applyPageAccessControl().catch(error => {
                console.error('Access control error:', error);
            });
        }
        
        // Wait for element with retry logic
        let retries = 0;
        const maxRetries = 50; // 5 seconds max
        
        const tryInit = () => {
            const list = document.getElementById("productList");
            if (list) {
                initializeProducts();
            } else if (retries < maxRetries) {
                retries++;
                setTimeout(tryInit, 100);
            } else {
                console.error('productList element not found after multiple retries');
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





