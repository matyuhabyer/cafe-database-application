// Utility function to include HTML files
async function includeHTML(elementId, filePath) {
  try {
    const response = await fetch(filePath);
    if (!response.ok) {
      throw new Error(`Failed to load ${filePath}: ${response.statusText}`);
    }
    const html = await response.text();
    const element = document.getElementById(elementId);
    if (element) {
      element.innerHTML = html;
      // Execute any scripts in the included HTML
      const scripts = element.querySelectorAll('script');
      scripts.forEach(oldScript => {
        const newScript = document.createElement('script');
        Array.from(oldScript.attributes).forEach(attr => {
          newScript.setAttribute(attr.name, attr.value);
        });
        newScript.appendChild(document.createTextNode(oldScript.innerHTML));
        oldScript.parentNode.replaceChild(newScript, oldScript);
      });
      
      // If header was loaded, call checkAuth after a short delay to ensure elements exist
      if (filePath.includes('header.html')) {
        // Wait for scripts to execute, then check auth
        // header.js is loaded as part of header.html, so it should be available
        setTimeout(() => {
          if (typeof window.checkAuth === 'function') {
            console.log('Calling checkAuth from include.js');
            window.checkAuth();
          } else {
            // If checkAuth isn't available yet, wait a bit more (header.js might still be loading)
            setTimeout(() => {
              if (typeof window.checkAuth === 'function') {
                console.log('Calling checkAuth from include.js (delayed)');
                window.checkAuth();
              } else {
                // Final retry - header.js should be loaded by now
                setTimeout(() => {
                  if (typeof window.checkAuth === 'function') {
                    console.log('Calling checkAuth from include.js (final retry)');
                    window.checkAuth();
                  } else {
                    console.warn('checkAuth function not available - header.js may not be loaded. This is normal if header.js failed to load.');
                  }
                }, 500);
              }
            }, 300);
          }
        }, 300);
        
        // Note: Logout button is handled in header.js via checkAuth function
        // No need to set up logout button here since it doesn't exist in header.html
      }
    }
  } catch (error) {
    console.error('Error including HTML:', error);
  }
}

