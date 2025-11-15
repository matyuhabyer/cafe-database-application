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
console.log('Admin.js - Base URL:', BASE_URL, 'API Base:', API_BASE);

// Load branches
async function loadBranches() {
    try {
        const response = await fetch(`${API_BASE}/admin/get_branches`, {
            credentials: 'include'
        });
        const result = await response.json();
        
        const branchesList = document.getElementById('branchesList');
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
        const response = await fetch(`${API_BASE}/admin/get_reports`, {
            credentials: 'include'
        });
        const result = await response.json();
        
        const reportsContent = document.getElementById('reportsContent');
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

// Initialize
loadBranches();
loadReports();
