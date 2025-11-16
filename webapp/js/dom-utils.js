/**
 * DOM Utilities - Functions to ensure DOM elements are ready
 */

/**
 * Wait for an element to exist in the DOM
 * @param {string} selector - CSS selector or element ID
 * @param {number} timeout - Maximum time to wait in milliseconds (default: 5000)
 * @param {number} interval - Check interval in milliseconds (default: 100)
 * @returns {Promise<HTMLElement>} - The found element
 */
function waitForElement(selector, timeout = 5000, interval = 100) {
    return new Promise((resolve, reject) => {
        const startTime = Date.now();
        
        const checkElement = () => {
            const element = typeof selector === 'string' 
                ? (selector.startsWith('#') 
                    ? document.getElementById(selector.substring(1))
                    : document.querySelector(selector))
                : selector;
            
            if (element) {
                resolve(element);
                return;
            }
            
            if (Date.now() - startTime >= timeout) {
                reject(new Error(`Element ${selector} not found within ${timeout}ms`));
                return;
            }
            
            setTimeout(checkElement, interval);
        };
        
        // Start checking immediately
        checkElement();
    });
}

/**
 * Wait for multiple elements to exist
 * @param {string[]} selectors - Array of CSS selectors or element IDs
 * @param {number} timeout - Maximum time to wait in milliseconds (default: 5000)
 * @returns {Promise<HTMLElement[]>} - Array of found elements
 */
function waitForElements(selectors, timeout = 5000) {
    return Promise.all(selectors.map(selector => waitForElement(selector, timeout)));
}

/**
 * Wait for DOM to be fully ready (including dynamically loaded content)
 * @param {Function} callback - Function to call when DOM is ready
 * @param {number} maxWait - Maximum time to wait in milliseconds (default: 10000)
 */
function waitForDOMReady(callback, maxWait = 10000) {
    const startTime = Date.now();
    
    const checkReady = () => {
        // Check if document is ready
        if (document.readyState === 'complete') {
            // Small delay to ensure all scripts have executed
            setTimeout(callback, 50);
            return;
        }
        
        if (Date.now() - startTime >= maxWait) {
            console.warn('DOM ready check timed out, proceeding anyway');
            callback();
            return;
        }
        
        setTimeout(checkReady, 100);
    };
    
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            setTimeout(checkReady, 100);
        });
    } else {
        checkReady();
    }
}

/**
 * Wait for a specific condition to be true
 * @param {Function} condition - Function that returns true when condition is met
 * @param {number} timeout - Maximum time to wait in milliseconds (default: 5000)
 * @param {number} interval - Check interval in milliseconds (default: 100)
 * @returns {Promise<void>}
 */
function waitForCondition(condition, timeout = 5000, interval = 100) {
    return new Promise((resolve, reject) => {
        const startTime = Date.now();
        
        const checkCondition = () => {
            try {
                if (condition()) {
                    resolve();
                    return;
                }
            } catch (error) {
                reject(error);
                return;
            }
            
            if (Date.now() - startTime >= timeout) {
                reject(new Error(`Condition not met within ${timeout}ms`));
                return;
            }
            
            setTimeout(checkCondition, interval);
        };
        
        checkCondition();
    });
}

/**
 * Initialize when DOM is ready with element checks
 * @param {Function} initFunction - Initialization function to call
 * @param {string[]} requiredElements - Array of element IDs/selectors that must exist
 * @param {Object} options - Options object
 * @param {number} options.timeout - Maximum time to wait (default: 5000)
 * @param {boolean} options.waitForAccessControl - Wait for access control to complete (default: false)
 */
async function initializeWhenReady(initFunction, requiredElements = [], options = {}) {
    const {
        timeout = 5000,
        waitForAccessControl = false
    } = options;
    
    try {
        // Wait for DOM to be ready
        await new Promise((resolve) => {
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', resolve);
            } else {
                resolve();
            }
        });
        
        // Wait for required elements if specified
        if (requiredElements.length > 0) {
            await waitForElements(requiredElements, timeout);
        }
        
        // Wait for access control if needed
        if (waitForAccessControl && typeof window.applyPageAccessControl === 'function') {
            try {
                const accessControlPromise = window.applyPageAccessControl();
                const timeoutPromise = new Promise((resolve) => setTimeout(resolve, 2000));
                await Promise.race([accessControlPromise, timeoutPromise]);
            } catch (error) {
                console.error('Access control error:', error);
                // Continue anyway
            }
        }
        
        // Small delay to ensure everything is settled
        await new Promise(resolve => setTimeout(resolve, 50));
        
        // Call initialization function
        initFunction();
        
    } catch (error) {
        console.error('Error waiting for DOM ready:', error);
        // Try to initialize anyway
        try {
            initFunction();
        } catch (initError) {
            console.error('Error during initialization:', initError);
        }
    }
}

// Make functions globally available
window.waitForElement = waitForElement;
window.waitForElements = waitForElements;
window.waitForDOMReady = waitForDOMReady;
window.waitForCondition = waitForCondition;
window.initializeWhenReady = initializeWhenReady;

