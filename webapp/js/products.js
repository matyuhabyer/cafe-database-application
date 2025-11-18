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
let isCurrencyLoading = false; // Flag to track if currency conversion is in progress
let currencies = []; // Store currencies to get rates for cart conversion

let list = null;
let currencySelect = null;

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

function getProductImageMarkup(item, categoryName, isAvailable) {
    const resolvedSrc = resolveImageUrl(item.image_url);
    if (resolvedSrc) {
        const alt = escapeAttribute(item.name || categoryName || 'Menu item');
        return `<img src="${resolvedSrc}" alt="${alt}" loading="lazy" decoding="async">`;
    }
    const emoji = getCategoryEmoji(categoryName || '', item.is_drink);
    return `<span class="product-placeholder">${emoji}</span>`;
}

// Fetch menu from API
async function fetchMenu(currency = 'PHP') {
    try {
        // Set loading state - disable add to cart buttons
        isCurrencyLoading = true;
        disableAddToCartButtons(true);
        
        // Ensure elements are available
        if (!list) {
            list = document.getElementById("productList");
        }
        if (!list) {
            console.error('productList element not found');
            isCurrencyLoading = false;
            disableAddToCartButtons(false);
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
            isCurrencyLoading = false;
            disableAddToCartButtons(false);
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
                isCurrencyLoading = false;
                disableAddToCartButtons(false);
                return;
            }
            
            renderProducts();
            
            // Currency conversion complete - re-enable buttons
            isCurrencyLoading = false;
            disableAddToCartButtons(false);
        } else {
            console.error('Error fetching menu:', result.message || 'Unknown error', result);
            list.innerHTML = `<p class="error-message">Error loading menu: ${result.message || 'Invalid response format. Please check console for details.'}</p>`;
            isCurrencyLoading = false;
            disableAddToCartButtons(false);
        }
    } catch (error) {
        console.error('Error fetching menu:', error);
        if (list) {
            list.innerHTML = `<p class="error-message">Error connecting to server. Please check your connection and try again.</p>`;
        }
        isCurrencyLoading = false;
        disableAddToCartButtons(false);
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
                try {
                    const price = parseFloat(item.price_amount || 0).toFixed(2);
                    const legends = (item.legends && item.legends.length > 0) 
                        ? item.legends.map(l => `<span class="legend" title="${l.description || ''}">${l.code || ''}</span>`).join('')
                        : '';
                    
                    // Check if item is available
                    // Handle all possible formats: boolean false, number 0, string '0', null, undefined
                    // Explicitly check - treat null/undefined as available (backwards compatibility)
                    const isAvailableValue = item.is_available;
                    let isAvailable = true; // Default to available
                    
                    // Only mark as unavailable if explicitly false, 0, or '0'
                    if (isAvailableValue === false || isAvailableValue === 0 || isAvailableValue === '0') {
                        isAvailable = false;
                    }
                    
                    // Debug log for unavailable items to help diagnose disappearing items
                    if (!isAvailable) {
                        console.log('Rendering UNAVAILABLE item:', {
                            menu_id: item.menu_id,
                            name: item.name,
                            is_available: isAvailableValue,
                            type: typeof isAvailableValue
                        });
                    }
                    
                    const imageMarkup = getProductImageMarkup(item, category.name, isAvailable);
                    
                    const productCard = document.createElement('div');
                    productCard.className = 'product' + (isAvailable ? '' : ' product-unavailable');
                    productCard.innerHTML = `
                        ${!isAvailable ? '<div class="unavailable-badge">Not Available</div>' : ''}
                        <div class="product-image ${!isAvailable ? 'image-unavailable' : ''}">
                            ${imageMarkup}
                        </div>
                        <div class="product-body">
                            <div class="product-header">
                                <h4 class="${!isAvailable ? 'text-strikethrough' : ''}">${item.name || 'Unnamed Item'}${legends}</h4>
                            </div>
                            <p class="product-description ${!isAvailable ? 'text-unavailable' : ''}">${item.description || 'Delicious item from our menu.'}</p>
                            <div class="product-price-section">
                                <span class="product-price ${!isAvailable ? 'text-unavailable' : ''}">${symbol}${price}</span>
                            </div>
                            <div class="product-actions">
                                <button onclick="quickAddToCart(${item.menu_id})" class="btn-add-to-cart" data-menu-id="${item.menu_id}" ${!isAvailable ? 'disabled' : ''}>
                                    <span>🛒</span> Add to Cart
                                </button>
                                <button onclick="viewProductDetails(${item.menu_id})" class="btn-view" ${!isAvailable ? 'disabled' : ''}>View Details</button>
                            </div>
                        </div>
                    `;
                    itemsContainer.appendChild(productCard);
                    
                    // Verify the card was actually appended
                    if (!productCard.parentElement) {
                        console.error('Failed to append product card for item:', item.menu_id, item.name);
                    }
                } catch (error) {
                    console.error('Error rendering item:', item.menu_id, item.name, error);
                }
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
    // Prevent viewing details while currency conversion is in progress
    if (isCurrencyLoading) {
        showCartNotification('Please wait while prices are loading...');
        return;
    }
    
    // Check availability before fetching details
    const item = findItemInMenu(menuId);
    if (item) {
        const isAvailable = item.is_available !== false && item.is_available !== 0 && item.is_available !== '0';
        if (!isAvailable) {
            showCartNotification('This item is not available. Please select another item.', 'error');
            return;
        }
    }
    
    try {
        const response = await fetch(`${window.API_BASE}/menu/get_item?menu_id=${menuId}&currency=${currentCurrency}`);
        const result = await response.json();
        
        if (result.success) {
            const item = result.data;
            // Check availability from API response as well
            const isAvailable = item.is_available !== false && item.is_available !== 0 && item.is_available !== '0';
            if (!isAvailable) {
                showCartNotification('This item is not available. Please select another item.', 'error');
                return;
            }
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
    const isAvailable = item.is_available !== false && item.is_available !== 0 && item.is_available !== '0';
    const imageMarkup = getProductImageMarkup(item, item.category_name || '', isAvailable);
    
    let modalHTML = `
        <div class="modal" id="productModal">
            <div class="modal-content ${!isAvailable ? 'modal-unavailable' : ''}">
                <span class="close" onclick="closeModal()">&times;</span>
                ${!isAvailable ? '<div class="unavailable-badge-modal">Not Available</div>' : ''}
                <div class="modal-product-image ${!isAvailable ? 'image-unavailable' : ''}">
                    ${imageMarkup}
                </div>
                <h2 class="${!isAvailable ? 'text-strikethrough' : ''}">${item.name}</h2>
                <p class="${!isAvailable ? 'text-unavailable' : ''}">${item.description || 'Delicious item from our menu.'}</p>
                <p style="color: #6d4c41; font-size: 1.2rem; font-weight: 600; margin: 15px 0; ${!isAvailable ? 'opacity: 0.5; text-decoration: line-through;' : ''}">
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
            <button onclick="addToCartFromModal(${item.menu_id})" class="btn-add-cart" ${!isAvailable ? 'disabled' : ''}>
                <span>🛒</span> ${isAvailable ? 'Add to Cart' : 'Not Available'}
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
        
        // If currency is loading or item is unavailable, disable the add to cart button in the modal
        const modalAddToCartBtn = modal.querySelector('.btn-add-cart');
        if (modalAddToCartBtn) {
            if (isCurrencyLoading) {
                modalAddToCartBtn.disabled = true;
                modalAddToCartBtn.style.opacity = '0.6';
                modalAddToCartBtn.style.cursor = 'not-allowed';
                modalAddToCartBtn.title = 'Loading prices... Please wait.';
            } else if (!isAvailable) {
                modalAddToCartBtn.disabled = true;
                modalAddToCartBtn.style.opacity = '0.6';
                modalAddToCartBtn.style.cursor = 'not-allowed';
                modalAddToCartBtn.title = 'This item is not available.';
            }
        }
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
    // Prevent adding to cart while currency conversion is in progress
    if (isCurrencyLoading) {
        showCartNotification('Please wait while prices are loading...');
        return;
    }
    
    // Calculate price (simplified - in production, recalculate from API)
    const item = findItemInMenu(menuId);
    if (!item) {
        alert('Item not found');
        return;
    }
    
    // Check if item is available
    const isAvailable = item.is_available !== false && item.is_available !== 0 && item.is_available !== '0';
    if (!isAvailable) {
        showCartNotification('This item is not available. Please select another item.', 'error');
        return;
    }
    
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
    
    // Convert price from display currency to PHP base currency for storage
    const basePrice = convertToBasePrice(price, currentCurrency);
    
    const cartItem = {
        menu_id: menuId,
        name: item.name,
        price: basePrice, // Store in PHP base currency
        quantity: quantity,
        drink_option_id: drinkOptionId,
        extras: extras,
        image_url: resolveImageUrl(item.image_url) || item.image_url || null
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

// Disable/enable all product action buttons (Add to Cart, View Details, and Currency Selector)
function disableAddToCartButtons(disable) {
    // Disable all "Add to Cart" buttons on the page
    const addToCartButtons = document.querySelectorAll('.btn-add-to-cart');
    addToCartButtons.forEach(button => {
        button.disabled = disable;
        if (disable) {
            button.style.opacity = '0.6';
            button.style.cursor = 'not-allowed';
            button.title = 'Loading prices... Please wait.';
        } else {
            button.style.opacity = '1';
            button.style.cursor = 'pointer';
            button.title = '';
        }
    });
    
    // Disable all "View Details" buttons on the page
    const viewDetailsButtons = document.querySelectorAll('.btn-view');
    viewDetailsButtons.forEach(button => {
        button.disabled = disable;
        if (disable) {
            button.style.opacity = '0.6';
            button.style.cursor = 'not-allowed';
            button.title = 'Loading prices... Please wait.';
        } else {
            button.style.opacity = '1';
            button.style.cursor = 'pointer';
            button.title = '';
        }
    });
    
    // Disable currency selector while loading
    if (currencySelect) {
        currencySelect.disabled = disable;
        if (disable) {
            currencySelect.style.opacity = '0.6';
            currencySelect.style.cursor = 'not-allowed';
            currencySelect.title = 'Loading prices... Please wait.';
        } else {
            currencySelect.style.opacity = '1';
            currencySelect.style.cursor = 'pointer';
            currencySelect.title = '';
        }
    }
    
    // Also disable "Add to Cart" button in modal if it exists
    const modalAddToCartBtn = document.querySelector('#productModal .btn-add-cart');
    if (modalAddToCartBtn) {
        modalAddToCartBtn.disabled = disable;
        if (disable) {
            modalAddToCartBtn.style.opacity = '0.6';
            modalAddToCartBtn.style.cursor = 'not-allowed';
            modalAddToCartBtn.title = 'Loading prices... Please wait.';
        } else {
            modalAddToCartBtn.style.opacity = '1';
            modalAddToCartBtn.style.cursor = 'pointer';
            modalAddToCartBtn.title = '';
        }
    }
}

// Quick add to cart (adds with default options)
window.quickAddToCart = function(menuId) {
    // Prevent adding to cart while currency conversion is in progress
    if (isCurrencyLoading) {
        showCartNotification('Please wait while prices are loading...');
        return;
    }
    
    const item = findItemInMenu(menuId);
    if (!item) {
        alert('Item not found');
        return;
    }
    
    // Check if item is available
    const isAvailable = item.is_available !== false && item.is_available !== 0 && item.is_available !== '0';
    if (!isAvailable) {
        showCartNotification('This item is not available. Please select another item.', 'error');
        return;
    }
    
    // For drinks, show modal for customization
    // For non-drinks, add directly to cart
    if (item.is_drink && item.drink_options && item.drink_options.length > 0) {
        // Show modal for drinks to select temperature
        viewProductDetails(menuId);
    } else {
        // Add directly to cart for non-drinks
        // Convert price from display currency to PHP base currency for storage
        const basePrice = convertToBasePrice(item.price_amount, currentCurrency);
        
        const cartItem = {
            menu_id: menuId,
            name: item.name,
            price: basePrice, // Store in PHP base currency
            quantity: 1,
            drink_option_id: null,
            extras: [],
            image_url: resolveImageUrl(item.image_url) || item.image_url || null
        };
        
        cart.push(cartItem);
        localStorage.setItem("cart", JSON.stringify(cart));
        
        // Show success notification
        showCartNotification(`${item.name} added to cart!`);
    }
}

// Show cart notification
function showCartNotification(message, type = 'success') {
    // Create notification element
    const notification = document.createElement('div');
    const bgColor = type === 'error' ? '#d32f2f' : '#6d4c41';
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: ${bgColor};
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

// Fetch currencies to get rates for cart conversion
async function fetchCurrenciesForProducts() {
    try {
        const response = await fetch(`${window.API_BASE}/menu/get_currencies`);
        const result = await response.json();
        
        if (result.success && result.data) {
            currencies = result.data;
        }
    } catch (error) {
        console.error('Error fetching currencies:', error);
    }
}

// Convert price from display currency to PHP (base currency)
function convertToBasePrice(priceInCurrency, currencyCode) {
    // If no currencies loaded yet or currency is PHP, return as-is (PHP is base currency)
    if (!currencies || currencies.length === 0 || currencyCode === 'PHP') {
        return priceInCurrency;
    }
    
    const currency = currencies.find(c => c.code === currencyCode);
    if (!currency || !currency.rate) {
        console.warn('Currency not found for conversion:', currencyCode);
        return priceInCurrency; // Fallback: assume it's already in PHP
    }
    // Convert to PHP base by dividing by the currency rate
    // Rate is conversion from PHP to the currency, so to go back divide
    return priceInCurrency / currency.rate;
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
        
        // Fetch currencies first to get rates
        fetchCurrenciesForProducts();
        
        // Initialize currency selector - read from localStorage first
        if (currencySelect) {
            console.log('Currency selector found:', currencySelect);
            // Get currency from localStorage to ensure consistency
            const storedCurrency = localStorage.getItem('selectedCurrency') || 'PHP';
            currentCurrency = storedCurrency;
            currencySelect.value = currentCurrency;
            
            // Remove existing listeners to avoid duplicates
            const newSelect = currencySelect.cloneNode(true);
            currencySelect.parentNode.replaceChild(newSelect, currencySelect);
            currencySelect = newSelect;
            currencySelect.value = currentCurrency; // Set again after cloning
            
            currencySelect.addEventListener("change", (e) => {
                // Prevent currency change while loading
                if (isCurrencyLoading) {
                    // Revert to current currency
                    e.target.value = currentCurrency;
                    showCartNotification('Please wait while prices are loading...');
                    return;
                }
                
                const newCurrency = e.target.value;
                currentCurrency = newCurrency;
                localStorage.setItem('selectedCurrency', newCurrency);
                fetchMenu(newCurrency);
            });
        } else {
            console.warn('Currency selector not found');
        }
        
        // Fetch menu with current currency from localStorage
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





