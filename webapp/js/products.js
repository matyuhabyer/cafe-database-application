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
const BASE_URL = window.BASE_URL;
const API_BASE = window.API_BASE;
console.log('Products.js - Base URL:', BASE_URL, 'API Base:', API_BASE);

// Global variables
let menuData = [];
let currentCurrency = 'PHP';
let cart = JSON.parse(localStorage.getItem("cart") || "[]");

const list = document.getElementById("productList");
const currencySelect = document.getElementById("currency");

// Fetch menu from API
async function fetchMenu(currency = 'PHP') {
    try {
        console.log('Fetching menu with currency:', currency);
        console.log('API URL:', `${API_BASE}/menu/get_menu?currency=${currency}`);
        
        const response = await fetch(`${API_BASE}/menu/get_menu?currency=${currency}`);
        
        if (!response.ok) {
            console.error('HTTP Error:', response.status, response.statusText);
            list.innerHTML = `<p class="error-message">Error loading menu: ${response.status} ${response.statusText}</p>`;
            return;
        }
        
        const result = await response.json();
        console.log('Menu API Response:', result);
        
        if (result.success && result.data) {
            menuData = result.data;
            currentCurrency = currency;
            console.log('Menu data loaded:', {
                currency: menuData.currency,
                categoriesCount: menuData.categories ? menuData.categories.length : 0,
                totalItems: menuData.categories ? menuData.categories.reduce((sum, cat) => sum + (cat.items ? cat.items.length : 0), 0) : 0
            });
            renderProducts();
        } else {
            console.error('Error fetching menu:', result.message || 'Unknown error');
            list.innerHTML = `<p class="error-message">Error loading menu: ${result.message || 'Please try again later.'}</p>`;
        }
    } catch (error) {
        console.error('Error fetching menu:', error);
        list.innerHTML = `<p class="error-message">Error connecting to server. Please check your connection and try again.</p>`;
    }
}

// Render products by category
function renderProducts() {
    if (!menuData) {
        console.error('Menu data is not available');
        list.innerHTML = '<p class="error-message">Menu data not loaded. Please refresh the page.</p>';
        return;
    }
    
    if (!menuData.categories || menuData.categories.length === 0) {
        console.warn('No categories found in menu data');
        list.innerHTML = '<p>No menu items available. Please check if there are items in the database.</p>';
        return;
    }
    
    list.innerHTML = "";
    const symbol = menuData.currency?.symbol || '₱';
    
    let totalItems = 0;
    menuData.categories.forEach(category => {
        if (category.items && category.items.length > 0) {
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
                
                const productCard = document.createElement('div');
                productCard.className = 'product';
                productCard.innerHTML = `
                    <div class="product-header">
                        <h4>${item.name || 'Unnamed Item'} ${legends}</h4>
                    </div>
                    <p class="product-description">${item.description || ''}</p>
                    <div class="product-footer">
                        <span class="product-price">${symbol}${price}</span>
                        <button onclick="viewProductDetails(${item.menu_id})" class="btn-view">View Details</button>
                    </div>
                `;
                itemsContainer.appendChild(productCard);
            });
            
            categorySection.appendChild(itemsContainer);
            list.appendChild(categorySection);
        }
    });
    
    if (totalItems === 0) {
        list.innerHTML = '<p>No menu items available in any category. Please add items to the database.</p>';
    } else {
        console.log(`Rendered ${totalItems} menu items across ${menuData.categories.length} categories`);
    }
}

// View product details - make it globally accessible
window.viewProductDetails = async function(menuId) {
    try {
        const response = await fetch(`${API_BASE}/menu/get_item?menu_id=${menuId}&currency=${currentCurrency}`);
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
    
    let modalHTML = `
        <div class="modal" id="productModal">
            <div class="modal-content">
                <span class="close" onclick="closeModal()">&times;</span>
                <h2>${item.name}</h2>
                <p>${item.description || ''}</p>
                <p><strong>Base Price: ${symbol}${basePrice.toFixed(2)}</strong></p>
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
            <button onclick="addToCartFromModal(${item.menu_id})" class="btn-add-cart">Add to Cart</button>
        </div>
    </div>
    `;
    
    document.body.insertAdjacentHTML('beforeend', modalHTML);
    
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
        modal.remove();
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
    alert(`${item.name} added to cart!`);
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

// Currency change handler
if (currencySelect) {
    currencySelect.addEventListener("change", (e) => {
        fetchMenu(e.target.value);
    });
}

// Initialize - wait for DOM to be ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        console.log('DOM loaded, fetching menu...');
        fetchMenu(currentCurrency);
    });
} else {
    // DOM is already ready
    console.log('DOM already ready, fetching menu...');
    fetchMenu(currentCurrency);
}





