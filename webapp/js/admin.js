/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/JavaScript.js to edit this template
 */

// API Base URL
const API_BASE = 'api/';

// Load branches
async function loadBranches() {
    try {
        const response = await fetch(`${API_BASE}admin/get_branches`, {
            credentials: 'include'
        });
        const result = await response.json();
        
        const branchesList = document.getElementById('branchesList');
        if (result.success && result.data) {
            branchesList.innerHTML = result.data.map(branch => `
                <div class="branch-item">
                    <h4>${branch.name}</h4>
                    <p>Address: ${branch.address || 'N/A'}</p>
                    <p>Contact: ${branch.contact_num || 'N/A'}</p>
                    <p>Manager: ${branch.manager_name || 'N/A'}</p>
                </div>
            `).join('');
        } else {
            branchesList.innerHTML = '<p>No branches found.</p>';
        }
    } catch (error) {
        console.error('Error loading branches:', error);
        document.getElementById('branchesList').innerHTML = '<p>Error loading branches.</p>';
    }
}

// Load reports
async function loadReports() {
    try {
        const response = await fetch(`${API_BASE}admin/get_reports`, {
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
                        <h4>Sales Summary</h4>
                        <p>Total Sales: ₱${reports.sales.total_sales_php || 0}</p>
                        <p>Total Transactions: ${reports.sales.total_transactions || 0}</p>
                    </div>
                `;
            }
            
            if (reports.top_items && reports.top_items.length > 0) {
                html += `
                    <div class="report-card">
                        <h4>Top Selling Items</h4>
                        <ul>
                            ${reports.top_items.map(item => 
                                `<li>${item.name} - Quantity: ${item.total_quantity}, Revenue: ₱${item.total_revenue}</li>`
                            ).join('')}
                        </ul>
                    </div>
                `;
            }
            
            if (reports.orders_by_status) {
                html += `
                    <div class="report-card">
                        <h4>Orders by Status</h4>
                        <ul>
                            ${Object.entries(reports.orders_by_status).map(([status, count]) => 
                                `<li>${status}: ${count}</li>`
                            ).join('')}
                        </ul>
                    </div>
                `;
            }
            
            html += '</div>';
            reportsContent.innerHTML = html;
        } else {
            reportsContent.innerHTML = '<p>No reports available.</p>';
        }
    } catch (error) {
        console.error('Error loading reports:', error);
        document.getElementById('reportsContent').innerHTML = '<p>Error loading reports.</p>';
    }
}

// Initialize
loadBranches();
loadReports();
