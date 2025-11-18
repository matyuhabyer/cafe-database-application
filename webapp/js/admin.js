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
console.log('Admin.js - Base URL:', window.BASE_URL, 'API Base:', window.API_BASE);

const adminState = {
    summary: null,
    branches: [],
    employees: [],
    settings: {}
};

const EMPLOYEE_ROLE_OPTIONS = [
    { value: 'staff', label: 'Staff' },
    { value: 'manager', label: 'Manager' },
    { value: 'admin', label: 'Admin' }
];

function getRoleLabel(role) {
    if (!role) return 'N/A';
    return role.charAt(0).toUpperCase() + role.slice(1);
}

function formatCurrency(value) {
    const num = Number(value) || 0;
    return `₱${num.toFixed(2)}`;
}

function setTextContent(id, value) {
    const el = document.getElementById(id);
    if (el) {
        el.textContent = value;
    }
}

function setSectionMessage(id, message, variant = 'info') {
    const el = document.getElementById(id);
    if (!el) return;
    const className = variant === 'error'
        ? 'error-message'
        : variant === 'empty'
            ? 'empty-state'
            : 'loading';
    el.innerHTML = `<p class="${className}">${message}</p>`;
}

// Load branches
async function loadBranches() {
    try {
        const response = await fetch(`${window.API_BASE}/admin/get_branches`, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const result = await response.json();
        adminState.branches = result.success && Array.isArray(result.data) ? result.data : [];
        renderBranches();
        populateEmployeeBranchSelect();
    } catch (error) {
        console.error('Error loading branches:', error);
        setSectionMessage('branchesList', 'Error loading branches. Please try again later.', 'error');
    }
}

function renderBranches() {
    const container = document.getElementById('branchesList');
    if (!container) return;
    if (!adminState.branches.length) {
        setSectionMessage('branchesList', 'No branches found.', 'empty');
        return;
    }
    container.innerHTML = adminState.branches.map(branch => `
        <div class="branch-item">
            <h4>${branch.name}</h4>
            <p><strong>Address:</strong> ${branch.address || 'N/A'}</p>
            <p><strong>Contact:</strong> ${branch.contact_num || 'N/A'}</p>
            <p><strong>Manager:</strong> ${branch.manager_name || 'Unassigned'}</p>
            <div class="branch-actions">
                <button class="btn btn-secondary" onclick="editBranch(${branch.branch_id})">Edit</button>
                <button class="btn btn-danger" onclick="deleteBranch(${branch.branch_id})">Delete</button>
            </div>
        </div>
    `).join('');
}

function resetBranchForm() {
    const form = document.getElementById('branchForm');
    if (form) form.reset();
    const hiddenId = document.getElementById('branchIdInput');
    if (hiddenId) hiddenId.value = '';
    const title = document.getElementById('branchFormTitle');
    if (title) title.textContent = 'Add Branch';
    const deleteBtn = document.getElementById('branchDeleteBtn');
    if (deleteBtn) deleteBtn.style.display = 'none';
}

async function submitBranchForm(event) {
    event.preventDefault();
    const branchId = document.getElementById('branchIdInput').value;
    const payload = {
        action: branchId ? 'update' : 'create',
        name: document.getElementById('branchNameInput').value.trim(),
        address: document.getElementById('branchAddressInput').value.trim(),
        contact_num: document.getElementById('branchContactInput').value.trim(),
        manager_id: document.getElementById('branchManagerSelect').value || null
    };
    if (!payload.name) {
        alert('Branch name is required.');
        return;
    }
    if (branchId) {
        payload.branch_id = parseInt(branchId, 10);
    }

    try {
        const response = await fetch(`${window.API_BASE}/admin/manage_branch`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(payload)
        });
        const result = await response.json();
        if (!response.ok || !result.success) {
            throw new Error(result.message || 'Branch operation failed.');
        }
        await loadBranches();
        resetBranchForm();
        loadDashboardSummary();
        alert(result.message || 'Branch saved successfully.');
    } catch (error) {
        console.error('Branch save error:', error);
        alert(error.message);
    }
}

function editBranch(branchId) {
    const branch = adminState.branches.find(b => b.branch_id === Number(branchId));
    if (!branch) return;
    document.getElementById('branchFormTitle').textContent = 'Edit Branch';
    document.getElementById('branchIdInput').value = branch.branch_id;
    document.getElementById('branchNameInput').value = branch.name || '';
    document.getElementById('branchAddressInput').value = branch.address || '';
    document.getElementById('branchContactInput').value = branch.contact_num || '';
    document.getElementById('branchManagerSelect').value = branch.manager_id || '';
    const deleteBtn = document.getElementById('branchDeleteBtn');
    if (deleteBtn) deleteBtn.style.display = 'inline-block';
}

async function deleteBranch(branchId) {
    if (!window.confirm('Delete this branch? This cannot be undone.')) {
        return;
    }
    try {
        const response = await fetch(`${window.API_BASE}/admin/manage_branch`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ action: 'delete', branch_id: branchId })
        });
        const result = await response.json();
        if (!response.ok || !result.success) {
            throw new Error(result.message || 'Unable to delete branch.');
        }
        await loadBranches();
        resetBranchForm();
        loadDashboardSummary();
        alert('Branch deleted successfully.');
    } catch (error) {
        console.error('Delete branch error:', error);
        alert(error.message);
    }
}

function populateManagerOptions() {
    const select = document.getElementById('branchManagerSelect');
    if (!select) return;
    const managers = (adminState.employees || []).filter(emp =>
        emp.role === 'manager' || emp.role === 'admin'
    );
    select.innerHTML = `<option value="">Unassigned</option>` +
        managers.map(emp => `<option value="${emp.employee_id}">${emp.name} (${emp.role})</option>`).join('');
}

function populateEmployeeBranchSelect() {
    const branchSelect = document.getElementById('employeeBranchInput');
    if (!branchSelect) return;
    const options = [`<option value="">Unassigned</option>`];
    (adminState.branches || []).forEach(branch => {
        options.push(`<option value="${branch.branch_id}">${branch.name}</option>`);
    });
    branchSelect.innerHTML = options.join('');
}

const employeeModalRefs = {
    initialized: false
};

function ensureEmployeeModalRefs() {
    if (employeeModalRefs.initialized) return employeeModalRefs;
    employeeModalRefs.modal = document.getElementById('employeeModal');
    employeeModalRefs.form = document.getElementById('employeeForm');
    employeeModalRefs.title = document.getElementById('employeeModalTitle');
    employeeModalRefs.idInput = document.getElementById('employeeIdInput');
    employeeModalRefs.nameInput = document.getElementById('employeeNameInput');
    employeeModalRefs.usernameInput = document.getElementById('employeeUsernameInput');
    employeeModalRefs.passwordInput = document.getElementById('employeePasswordInput');
    employeeModalRefs.roleInput = document.getElementById('employeeRoleInput');
    employeeModalRefs.branchInput = document.getElementById('employeeBranchInput');
    employeeModalRefs.contactInput = document.getElementById('employeeContactInput');
    employeeModalRefs.errorBox = document.getElementById('employeeModalError');
    employeeModalRefs.closeBtn = document.getElementById('employeeModalClose');
    employeeModalRefs.cancelBtn = document.getElementById('employeeModalCancel');
    employeeModalRefs.submitBtn = document.getElementById('employeeModalSubmit');

    if (employeeModalRefs.form) {
        employeeModalRefs.form.addEventListener('submit', handleEmployeeFormSubmit);
    }
    const closeModal = () => closeEmployeeModal();
    if (employeeModalRefs.closeBtn) employeeModalRefs.closeBtn.addEventListener('click', closeModal);
    if (employeeModalRefs.cancelBtn) employeeModalRefs.cancelBtn.addEventListener('click', closeModal);
    if (employeeModalRefs.modal) {
        employeeModalRefs.modal.addEventListener('click', (event) => {
            if (event.target === employeeModalRefs.modal) {
                closeEmployeeModal();
            }
        });
    }

    employeeModalRefs.initialized = true;
    return employeeModalRefs;
}

function openEmployeeModal(mode = 'create', employeeId = null) {
    const refs = ensureEmployeeModalRefs();
    populateEmployeeBranchSelect();
    setEmployeeModalError('');
    if (!refs.modal) return;

    refs.form.dataset.mode = mode;
    refs.idInput.value = employeeId ? String(employeeId) : '';
    refs.passwordInput.value = '';
    refs.usernameInput.disabled = mode === 'edit';
    refs.passwordInput.placeholder = mode === 'edit' ? 'Leave blank to keep current password' : '******';

    if (mode === 'edit' && employeeId) {
        const employee = adminState.employees.find(emp => emp.employee_id === Number(employeeId));
        if (!employee) {
            alert('Unable to find employee record.');
            return;
        }
        refs.title.textContent = `Edit ${employee.name}`;
        refs.nameInput.value = employee.name || '';
        refs.usernameInput.value = employee.username || '';
        refs.roleInput.value = employee.role || 'staff';
        refs.branchInput.value = employee.branch_id || '';
        refs.contactInput.value = employee.contact_num || '';
    } else {
        refs.title.textContent = 'Add Employee';
        refs.form.reset();
        refs.usernameInput.disabled = false;
        refs.roleInput.value = 'staff';
        refs.branchInput.value = '';
    }

    refs.modal.classList.add('show');
    refs.modal.setAttribute('aria-hidden', 'false');
}

function closeEmployeeModal() {
    const refs = ensureEmployeeModalRefs();
    if (!refs.modal) return;
    refs.modal.classList.remove('show');
    refs.modal.setAttribute('aria-hidden', 'true');
    setEmployeeModalError('');
}

function setEmployeeModalError(message) {
    const refs = ensureEmployeeModalRefs();
    if (!refs.errorBox) return;
    if (message) {
        refs.errorBox.style.display = 'block';
        refs.errorBox.textContent = message;
    } else {
        refs.errorBox.style.display = 'none';
        refs.errorBox.textContent = '';
    }
}

async function handleEmployeeFormSubmit(event) {
    event.preventDefault();
    const refs = ensureEmployeeModalRefs();
    if (!refs.form) return;
    const mode = refs.form.dataset.mode || 'create';
    const payload = {
        action: mode === 'edit' ? 'update' : 'create',
        name: refs.nameInput.value.trim(),
        username: refs.usernameInput.value.trim(),
        role: refs.roleInput.value,
        contact_num: refs.contactInput.value.trim(),
        branch_id: refs.branchInput.value ? Number(refs.branchInput.value) : null
    };
    if (!payload.name) {
        setEmployeeModalError('Name is required.');
        return;
    }
    if (mode === 'create' && !payload.username) {
        setEmployeeModalError('Username is required.');
        return;
    }
    const password = refs.passwordInput.value;
    if (password) {
        if (password.length < 6) {
            setEmployeeModalError('Password must be at least 6 characters long.');
            return;
        }
        payload.password = password;
    } else if (mode === 'create') {
        setEmployeeModalError('Password is required for new employees.');
        return;
    }
    if (mode === 'edit') {
        payload.employee_id = Number(refs.idInput.value);
    }

    try {
        const response = await fetch(`${window.API_BASE}/admin/manage_employee`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(payload)
        });
        const result = await response.json();
        if (!response.ok || !result.success) {
            throw new Error(result.message || 'Unable to save employee.');
        }
        closeEmployeeModal();
        await loadEmployees();
        await loadBranches();
        alert(result.message || 'Employee saved successfully.');
    } catch (error) {
        console.error('Employee save error:', error);
        setEmployeeModalError(error.message);
    }
}

async function deactivateEmployeeAccount(employeeId) {
    if (!window.confirm('Deactivate this employee? They will lose branch access.')) {
        return;
    }
    try {
        const response = await fetch(`${window.API_BASE}/admin/manage_employee`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({
                action: 'deactivate',
                employee_id: employeeId
            })
        });
        const result = await response.json();
        if (!response.ok || !result.success) {
            throw new Error(result.message || 'Unable to deactivate employee.');
        }
        await loadEmployees();
        alert('Employee deactivated successfully.');
    } catch (error) {
        console.error('Deactivate employee error:', error);
        alert(error.message);
    }
}

async function loadDashboardSummary() {
    const section = document.getElementById('dashboardSection');
    if (!section) return;
    try {
        const response = await fetch(`${window.API_BASE}/admin/dashboard_summary`, {
            credentials: 'include'
        });
        const result = await response.json();
        if (!response.ok || !result.success) {
            throw new Error(result.message || 'Unable to load summary.');
        }
        adminState.summary = result.data || {};
        renderDashboardSummary(adminState.summary);
    } catch (error) {
        console.error('Dashboard summary error:', error);
        setSectionMessage('branchPerformanceTable', error.message, 'error');
    }
}

function renderDashboardSummary(summary) {
    if (!summary) return;
    const totals = summary.totals || {};
    setTextContent('summaryTotalSales', formatCurrency(totals.total_sales || 0));
    setTextContent('summaryTotalTransactions', totals.total_transactions || 0);
    setTextContent('summaryActiveOrders', totals.active_orders || 0);

    const users = summary.active_users || {};
    setTextContent('summaryCustomers', users.customers || 0);
    setTextContent('summaryEmployees', users.employees || 0);

    renderBranchPerformance(summary.branch_performance || []);
    renderLoyaltyMetrics(summary.loyalty_metrics || {});
}

function renderBranchPerformance(performance) {
    const container = document.getElementById('branchPerformanceTable');
    if (!container) return;
    if (!performance.length) {
        setSectionMessage('branchPerformanceTable', 'No branch performance data.', 'empty');
        return;
    }
    container.innerHTML = `
        <table class="data-table">
            <thead>
                <tr>
                    <th>Branch</th>
                    <th>Total Sales</th>
                    <th>Transactions</th>
                    <th>Pending Orders</th>
                </tr>
            </thead>
            <tbody>
                ${performance.map(branch => `
                    <tr>
                        <td>${branch.name}</td>
                        <td>${formatCurrency(branch.total_sales || 0)}</td>
                        <td>${branch.transactions || 0}</td>
                        <td>${branch.pending_orders || 0}</td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

function renderLoyaltyMetrics(metrics) {
    setTextContent('summaryLoyaltyCards', metrics.total_cards || 0);
    setTextContent('summaryLoyaltyPoints', metrics.total_points || 0);
    setTextContent('summaryRecentRedemptions', metrics.recent_redemptions || 0);
}

function initExportButtons() {
    document.querySelectorAll('[data-export-report]').forEach(button => {
        button.addEventListener('click', () => {
            const type = button.dataset.exportReport;
            window.open(`${window.API_BASE}/admin/export_report?report_type=${encodeURIComponent(type)}`, '_blank');
        });
    });
}

async function loadSystemSettings() {
    const form = document.getElementById('systemSettingsForm');
    if (!form) return;
    try {
        const response = await fetch(`${window.API_BASE}/admin/system_settings`, {
            credentials: 'include'
        });
        const result = await response.json();
        if (!response.ok || !result.success) {
            throw new Error(result.message || 'Unable to load system settings.');
        }
        adminState.settings = result.data || {};
        document.getElementById('settingBackupEnabled').checked = adminState.settings.backup_enabled === 'true';
        document.getElementById('settingPasswordPolicy').value = adminState.settings.password_policy || '';
        document.getElementById('settingLoyaltyMultiplier').value = adminState.settings.loyalty_multiplier || '';
        document.getElementById('settingSecurityEmail').value = adminState.settings.security_email || '';
    } catch (error) {
        console.error('System settings load error:', error);
        alert(error.message);
    }
}

async function saveSystemSettings(event) {
    event.preventDefault();
    const payload = {
        settings: {
            backup_enabled: document.getElementById('settingBackupEnabled').checked ? 'true' : 'false',
            password_policy: document.getElementById('settingPasswordPolicy').value.trim(),
            loyalty_multiplier: document.getElementById('settingLoyaltyMultiplier').value,
            security_email: document.getElementById('settingSecurityEmail').value.trim()
        }
    };
    try {
        const response = await fetch(`${window.API_BASE}/admin/system_settings`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(payload)
        });
        const result = await response.json();
        if (!response.ok || !result.success) {
            throw new Error(result.message || 'Unable to save settings.');
        }
        adminState.settings = result.data || {};
        alert('System settings updated.');
    } catch (error) {
        console.error('System settings save error:', error);
        alert(error.message);
    }
}

// Load reports
async function loadReports(reportType = 'items') {
    const reportsContent = document.getElementById('reportsContent');
    if (!reportsContent) {
        console.error('reportsContent element not found');
        return;
    }
    
    // Show loading state
    reportsContent.innerHTML = '<div class="loading">Loading reports...</div>';
    
    console.log('Loading reports with reportType:', reportType);
    
    try {
        const url = `${window.API_BASE}/admin/get_reports?report_type=${encodeURIComponent(reportType)}`;
        console.log('Fetching from URL:', url);
        
        const response = await fetch(url, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const result = await response.json();
        console.log('Reports response received:', result);
        
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
            
            // Top Selling Items/Foods/Drinks based on reportType - Display as Chart
            if (reports.top_items && reports.top_items.length > 0) {
                let title = '🏆 Top Selling Items';
                if (reportType === 'foods') {
                    title = '🍽️ Top Selling Foods';
                } else if (reportType === 'drinks') {
                    title = '🥤 Top Selling Drinks';
                }
                
                // Create unique chart ID for each report type
                const chartId = `topItemsChart_${reportType}_${Date.now()}`;
                
                html += `
                    <div class="report-card">
                        <h4>${title} (Top 10)</h4>
                        <div style="position: relative; height: 400px; margin-top: 20px;">
                            <canvas id="${chartId}"></canvas>
                        </div>
                        <div style="margin-top: 15px; padding-top: 15px; border-top: 1px solid #e0e0e0;">
                            <table style="width: 100%; border-collapse: collapse; font-size: 0.9rem;">
                                <thead>
                                    <tr style="background: #6d4c41; color: white;">
                                        <th style="padding: 8px; text-align: left; border: 1px solid #8b6f47;">Rank</th>
                                        <th style="padding: 8px; text-align: left; border: 1px solid #8b6f47;">Item Name</th>
                                        <th style="padding: 8px; text-align: right; border: 1px solid #8b6f47;">Quantity</th>
                                        <th style="padding: 8px; text-align: right; border: 1px solid #8b6f47;">Revenue</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${reports.top_items.map((item, index) => 
                                        `<tr style="background: ${index % 2 === 0 ? '#faf8f3' : '#f8f9fa'};">
                                            <td style="padding: 8px; border: 1px solid #e0e0e0; font-weight: 600; color: #6d4c41;">${index + 1}</td>
                                            <td style="padding: 8px; border: 1px solid #e0e0e0;"><strong>${item.name}</strong></td>
                                            <td style="padding: 8px; text-align: right; border: 1px solid #e0e0e0;">${item.total_quantity}</td>
                                            <td style="padding: 8px; text-align: right; border: 1px solid #e0e0e0; font-weight: 600; color: #6d4c41;">₱${parseFloat(item.total_revenue || 0).toFixed(2)}</td>
                                        </tr>`
                                    ).join('')}
                                </tbody>
                            </table>
                        </div>
                    </div>
                `;
                
                // Render chart after HTML is inserted
                setTimeout(() => {
                    const canvas = document.getElementById(chartId);
                    if (canvas && typeof Chart !== 'undefined') {
                        const ctx = canvas.getContext('2d');
                        
                        // Prepare data for chart (reverse order for horizontal bar - highest at top)
                        const chartData = {
                            labels: reports.top_items.map(item => item.name).reverse(),
                            datasets: [{
                                label: 'Quantity Sold',
                                data: reports.top_items.map(item => item.total_quantity).reverse(),
                                backgroundColor: 'rgba(109, 76, 65, 0.8)',
                                borderColor: 'rgba(109, 76, 65, 1)',
                                borderWidth: 2
                            }]
                        };
                        
                        new Chart(ctx, {
                            type: 'bar',
                            data: chartData,
                            options: {
                                indexAxis: 'y', // Horizontal bar chart
                                responsive: true,
                                maintainAspectRatio: false,
                                plugins: {
                                    legend: {
                                        display: true,
                                        position: 'top',
                                        labels: {
                                            font: {
                                                size: 12,
                                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                                            },
                                            color: '#6d4c41'
                                        }
                                    },
                                    tooltip: {
                                        callbacks: {
                                            label: function(context) {
                                                const index = reports.top_items.length - 1 - context.dataIndex;
                                                const item = reports.top_items[index];
                                                return `Quantity: ${context.parsed.x} | Revenue: ₱${parseFloat(item.total_revenue || 0).toFixed(2)}`;
                                            }
                                        }
                                    }
                                },
                                scales: {
                                    x: {
                                        beginAtZero: true,
                                        title: {
                                            display: true,
                                            text: 'Quantity Sold',
                                            font: {
                                                size: 12,
                                                weight: 'bold',
                                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                                            },
                                            color: '#6d4c41'
                                        },
                                        ticks: {
                                            font: {
                                                size: 11,
                                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                                            },
                                            color: '#666'
                                        },
                                        grid: {
                                            color: 'rgba(0, 0, 0, 0.05)'
                                        }
                                    },
                                    y: {
                                        title: {
                                            display: true,
                                            text: 'Item Name',
                                            font: {
                                                size: 12,
                                                weight: 'bold',
                                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                                            },
                                            color: '#6d4c41'
                                        },
                                        ticks: {
                                            font: {
                                                size: 11,
                                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                                            },
                                            color: '#666',
                                            maxRotation: 45,
                                            minRotation: 0
                                        },
                                        grid: {
                                            color: 'rgba(0, 0, 0, 0.05)'
                                        }
                                    }
                                }
                            }
                        });
                    } else {
                        console.error('Chart.js not loaded or canvas not found:', chartId);
                    }
                }, 100);
            } else {
                html += `
                    <div class="report-card">
                        <h4>🏆 Top Selling ${reportType === 'foods' ? 'Foods' : reportType === 'drinks' ? 'Drinks' : 'Items'}</h4>
                        <p style="text-align: center; color: #666; padding: 20px;">No data available for this period.</p>
                    </div>
                `;
            }
            
            
            html += '</div>';
            reportsContent.innerHTML = html;
            console.log('Reports rendered successfully for type:', reportType);
        } else {
            console.warn('No reports data in response:', result);
            reportsContent.innerHTML = '<p class="error-message">No reports available.</p>';
        }
    } catch (error) {
        console.error('Error loading reports:', error);
        if (reportsContent) {
            reportsContent.innerHTML = `<p class="error-message">Error loading reports: ${error.message}. Please try again later.</p>`;
        }
    }
}

// Load analytics (sales per branch - weekly/monthly/annual)
async function loadAnalytics(period = null) {
    const analyticsContent = document.getElementById('analyticsContent');
    if (!analyticsContent) {
        console.error('analyticsContent element not found');
        return;
    }
    
    if (!period || period === '') {
        analyticsContent.innerHTML = '<p style="text-align: center; color: #666;">Select a period to view analytics...</p>';
        return;
    }
    
    // Show loading state
    analyticsContent.innerHTML = '<div class="loading">Loading analytics...</div>';
    
    console.log('Loading analytics with period:', period);
    
    try {
        const url = `${window.API_BASE}/admin/get_reports?analytics_period=${encodeURIComponent(period)}`;
        console.log('Fetching analytics from URL:', url);
        
        const response = await fetch(url, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const result = await response.json();
        console.log('Analytics response received:', result);
        
        if (result.success && result.data && result.data.reports && result.data.reports.analytics) {
            const analytics = result.data.reports.analytics;
            const branchSales = analytics.branch_sales || [];
            
            console.log('Analytics data received:', analytics);
            console.log('Branch sales:', branchSales);
            
            // Check if there's an error message
            if (analytics.error) {
                analyticsContent.innerHTML = `
                    <div class="report-card">
                        <h4>📊 Analytics</h4>
                        <p class="error-message">${analytics.error}</p>
                        <p style="margin-top: 10px; color: #666; font-size: 0.9rem;">
                            Debug info: Period=${period}, Start Date=${analytics.start_date || analytics.year || 'N/A'}
                        </p>
                    </div>
                `;
                return;
            }
            
            let periodText = '';
            if (period === 'weekly' || period === 'week') {
                periodText = 'This Week';
            } else if (period === 'monthly' || period === 'month') {
                periodText = 'This Month';
            } else if (period === 'annual' || period === 'year') {
                periodText = `Year ${analytics.year || new Date().getFullYear()}`;
            } else {
                periodText = period.charAt(0).toUpperCase() + period.slice(1);
            }
            
            // Create unique chart IDs for each period
            const salesChartId = `analyticsSalesChart_${period}_${Date.now()}`;
            const transactionsChartId = `analyticsTransactionsChart_${period}_${Date.now()}`;
            
            let html = `
                <div class="analytics-section">
                    <div class="report-card">
                        <h4>📊 Sales per Branch - ${periodText}</h4>
            `;
            
            // Show branches even if they have 0 sales (they should still appear from LEFT JOIN)
            // But if branchSales is empty, it means either no branches exist OR stored procedure failed
            if (branchSales && branchSales.length > 0) {
                // Sort branches by total sales (descending)
                const sortedBranches = [...branchSales].sort((a, b) => parseFloat(b.total_sales || 0) - parseFloat(a.total_sales || 0));
                
                html += `
                    <div style="margin-top: 20px;">
                        <h5 style="color: #6d4c41; margin-bottom: 15px; font-size: 1.1rem;">📈 Sales Comparison Chart</h5>
                        <div style="position: relative; height: 350px; margin-bottom: 30px;">
                            <canvas id="${salesChartId}"></canvas>
                        </div>
                    </div>
                    
                    <div style="margin-top: 20px;">
                        <h5 style="color: #6d4c41; margin-bottom: 15px; font-size: 1.1rem;">📊 Transactions Comparison Chart</h5>
                        <div style="position: relative; height: 350px; margin-bottom: 30px;">
                            <canvas id="${transactionsChartId}"></canvas>
                        </div>
                    </div>
                    
                    <div style="margin-top: 15px; padding-top: 15px; border-top: 2px solid #e0e0e0;">
                        <h5 style="color: #6d4c41; margin-bottom: 15px; font-size: 1.1rem;">📋 Detailed Data Table</h5>
                        <table style="width: 100%; border-collapse: collapse; margin-top: 15px;">
                            <thead>
                                <tr style="background: #6d4c41; color: white;">
                                    <th style="padding: 12px; text-align: left; border: 1px solid #8b6f47;">Branch</th>
                                    <th style="padding: 12px; text-align: right; border: 1px solid #8b6f47;">Total Sales</th>
                                    <th style="padding: 12px; text-align: right; border: 1px solid #8b6f47;">Transactions</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${sortedBranches.map((branch, index) => 
                                    `<tr style="background: ${index % 2 === 0 ? '#faf8f3' : '#f8f9fa'};">
                                        <td style="padding: 10px; border: 1px solid #e0e0e0;"><strong>${branch.branch_name || 'N/A'}</strong></td>
                                        <td style="padding: 10px; text-align: right; border: 1px solid #e0e0e0; font-weight: 600; color: #6d4c41;">₱${parseFloat(branch.total_sales || 0).toFixed(2)}</td>
                                        <td style="padding: 10px; text-align: right; border: 1px solid #e0e0e0;">${branch.transaction_count || 0}</td>
                                    </tr>`
                                ).join('')}
                            </tbody>
                            <tfoot>
                                <tr style="background: #fff9e6; font-weight: 700;">
                                    <td style="padding: 12px; border: 1px solid #e0e0e0;"><strong>Total</strong></td>
                                    <td style="padding: 12px; text-align: right; border: 1px solid #e0e0e0; color: #6d4c41;">
                                        ₱${parseFloat(branchSales.reduce((sum, b) => sum + (parseFloat(b.total_sales || 0)), 0)).toFixed(2)}
                                    </td>
                                    <td style="padding: 12px; text-align: right; border: 1px solid #e0e0e0;">
                                        ${branchSales.reduce((sum, b) => sum + (parseInt(b.transaction_count || 0)), 0)}
                                    </td>
                                </tr>
                            </tfoot>
                        </table>
                    </div>
                `;
                
                // Render charts after HTML is inserted
                setTimeout(() => {
                    // Sales Chart
                    const salesCanvas = document.getElementById(salesChartId);
                    if (salesCanvas && typeof Chart !== 'undefined') {
                        const salesCtx = salesCanvas.getContext('2d');
                        
                        const salesChartData = {
                            labels: sortedBranches.map(branch => branch.branch_name || 'N/A'),
                            datasets: [{
                                label: 'Total Sales (₱)',
                                data: sortedBranches.map(branch => parseFloat(branch.total_sales || 0)),
                                backgroundColor: 'rgba(109, 76, 65, 0.8)',
                                borderColor: 'rgba(109, 76, 65, 1)',
                                borderWidth: 2
                            }]
                        };
                        
                        new Chart(salesCtx, {
                            type: 'bar',
                            data: salesChartData,
                            options: {
                                responsive: true,
                                maintainAspectRatio: false,
                                plugins: {
                                    legend: {
                                        display: true,
                                        position: 'top',
                                        labels: {
                                            font: {
                                                size: 12,
                                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                                            },
                                            color: '#6d4c41'
                                        }
                                    },
                                    tooltip: {
                                        callbacks: {
                                            label: function(context) {
                                                const branch = sortedBranches[context.dataIndex];
                                                return `Sales: ₱${parseFloat(branch.total_sales || 0).toFixed(2)} | Transactions: ${branch.transaction_count || 0}`;
                                            }
                                        }
                                    }
                                },
                                scales: {
                                    y: {
                                        beginAtZero: true,
                                        title: {
                                            display: true,
                                            text: 'Total Sales (₱)',
                                            font: {
                                                size: 12,
                                                weight: 'bold',
                                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                                            },
                                            color: '#6d4c41'
                                        },
                                        ticks: {
                                            font: {
                                                size: 11,
                                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                                            },
                                            color: '#666',
                                            callback: function(value) {
                                                return '₱' + value.toFixed(0);
                                            }
                                        },
                                        grid: {
                                            color: 'rgba(0, 0, 0, 0.05)'
                                        }
                                    },
                                    x: {
                                        title: {
                                            display: true,
                                            text: 'Branch',
                                            font: {
                                                size: 12,
                                                weight: 'bold',
                                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                                            },
                                            color: '#6d4c41'
                                        },
                                        ticks: {
                                            font: {
                                                size: 11,
                                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                                            },
                                            color: '#666',
                                            maxRotation: 45,
                                            minRotation: 0
                                        },
                                        grid: {
                                            color: 'rgba(0, 0, 0, 0.05)'
                                        }
                                    }
                                }
                            }
                        });
                    }
                    
                    // Transactions Chart
                    const transactionsCanvas = document.getElementById(transactionsChartId);
                    if (transactionsCanvas && typeof Chart !== 'undefined') {
                        const transactionsCtx = transactionsCanvas.getContext('2d');
                        
                        const transactionsChartData = {
                            labels: sortedBranches.map(branch => branch.branch_name || 'N/A'),
                            datasets: [{
                                label: 'Transaction Count',
                                data: sortedBranches.map(branch => parseInt(branch.transaction_count || 0)),
                                backgroundColor: 'rgba(109, 76, 65, 0.8)',
                                borderColor: 'rgba(109, 76, 65, 1)',
                                borderWidth: 2
                            }]
                        };
                        
                        new Chart(transactionsCtx, {
                            type: 'bar',
                            data: transactionsChartData,
                            options: {
                                responsive: true,
                                maintainAspectRatio: false,
                                plugins: {
                                    legend: {
                                        display: true,
                                        position: 'top',
                                        labels: {
                                            font: {
                                                size: 12,
                                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                                            },
                                            color: '#6d4c41'
                                        }
                                    },
                                    tooltip: {
                                        callbacks: {
                                            label: function(context) {
                                                const branch = sortedBranches[context.dataIndex];
                                                return `Transactions: ${branch.transaction_count || 0} | Sales: ₱${parseFloat(branch.total_sales || 0).toFixed(2)}`;
                                            }
                                        }
                                    }
                                },
                                scales: {
                                    y: {
                                        beginAtZero: true,
                                        title: {
                                            display: true,
                                            text: 'Transaction Count',
                                            font: {
                                                size: 12,
                                                weight: 'bold',
                                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                                            },
                                            color: '#6d4c41'
                                        },
                                        ticks: {
                                            font: {
                                                size: 11,
                                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                                            },
                                            color: '#666',
                                            stepSize: 1
                                        },
                                        grid: {
                                            color: 'rgba(0, 0, 0, 0.05)'
                                        }
                                    },
                                    x: {
                                        title: {
                                            display: true,
                                            text: 'Branch',
                                            font: {
                                                size: 12,
                                                weight: 'bold',
                                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                                            },
                                            color: '#6d4c41'
                                        },
                                        ticks: {
                                            font: {
                                                size: 11,
                                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                                            },
                                            color: '#666',
                                            maxRotation: 45,
                                            minRotation: 0
                                        },
                                        grid: {
                                            color: 'rgba(0, 0, 0, 0.05)'
                                        }
                                    }
                                }
                            }
                        });
                    }
                }, 100);
            } else {
                // If branchSales is empty, it could mean:
                // 1. No branches exist in database
                // 2. Stored procedures are not installed
                // 3. No completed transactions in the date range
                html += `
                    <p style="text-align: center; color: #666; padding: 20px;">
                        No sales data available for this period.<br>
                        <small style="color: #999; margin-top: 10px; display: block;">
                            This could mean:<br>
                            - No branches exist in the database<br>
                            - No completed transactions in this time period<br>
                            - Analytics stored procedures may not be installed
                        </small>
                    </p>
                `;
            }
            
            html += `
                    </div>
                </div>
            `;
            
            analyticsContent.innerHTML = html;
            console.log('Analytics rendered successfully for period:', period);
        } else {
            console.warn('No analytics data in response:', result);
            analyticsContent.innerHTML = '<p class="error-message">No analytics data available. Make sure you are logged in as admin.</p>';
        }
    } catch (error) {
        console.error('Error loading analytics:', error);
        if (analyticsContent) {
            analyticsContent.innerHTML = `<p class="error-message">Error loading analytics: ${error.message}. Please try again later.</p>`;
        }
    }
}

// Load orders for staff/manager/admin
async function loadOrders() {
    try {
        const response = await fetch(`${window.API_BASE}/orders/get_orders`, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const result = await response.json();
        
        const ordersList = document.getElementById('ordersList');
        if (!ordersList) {
            console.error('ordersList element not found');
            return;
        }
        
        if (result.success && result.data && result.data.length > 0) {
            const orders = result.data;
            let html = '<div class="orders-container">';
            
            orders.forEach(order => {
                const orderDate = new Date(order.order_date).toLocaleString();
                const statusClass = order.status === 'completed' ? 'status-completed' : 
                                   order.status === 'confirmed' ? 'status-confirmed' : 
                                   order.status === 'pending' ? 'status-pending' : 'status-cancelled';
                
                html += `
                    <div class="order-item">
                        <div class="order-header">
                            <h4>Order #${order.order_id}</h4>
                            <span class="order-status ${statusClass}">${order.status.charAt(0).toUpperCase() + order.status.slice(1)}</span>
                        </div>
                        <div class="order-details">
                            <p><strong>Customer:</strong> ${order.customer_name || 'N/A'}</p>
                            <p><strong>Branch:</strong> ${order.branch_name || 'N/A'}</p>
                            <p><strong>Date:</strong> ${orderDate}</p>
                            <p><strong>Total:</strong> ₱${parseFloat(order.total_amount || 0).toFixed(2)}</p>
                            ${order.earned_points ? `<p><strong>Points Earned:</strong> ${order.earned_points}</p>` : ''}
                        </div>
                        <div class="order-actions">
                            ${order.status === 'pending' ? `
                                <button onclick="updateOrderStatus(${order.order_id}, 'confirmed')" class="btn-confirm">Confirm Order</button>
                                <button onclick="updateOrderStatus(${order.order_id}, 'cancelled')" class="btn-cancel">Cancel Order</button>
                            ` : ''}
                            ${order.status === 'confirmed' ? `
                                <button onclick="updateOrderStatus(${order.order_id}, 'completed')" class="btn-complete">Mark as Completed</button>
                                <button onclick="updateOrderStatus(${order.order_id}, 'cancelled')" class="btn-cancel">Cancel Order</button>
                            ` : ''}
                            <button onclick="viewOrderDetails(${order.order_id})" class="btn-view">View Details</button>
                        </div>
                    </div>
                `;
            });
            
            html += '</div>';
            ordersList.innerHTML = html;
        } else {
            if (result.success && result.data && result.data.length === 0) {
                ordersList.innerHTML = '<p class="error-message">No orders found.</p>';
            } else {
                ordersList.innerHTML = '<p class="error-message">' + (result.message || 'No orders found.') + '</p>';
            }
        }
    } catch (error) {
        console.error('Error loading orders:', error);
        const ordersList = document.getElementById('ordersList');
        if (ordersList) {
            ordersList.innerHTML = '<p class="error-message">Error loading orders: ' + error.message + '. Please try again later.</p>';
        }
    }
}

// Update order status (for staff)
async function updateOrderStatus(orderId, status) {
    let actionText = '';
    let confirmMessage = '';
    
    if (status === 'confirmed') {
        actionText = 'confirm';
        confirmMessage = 'Are you sure you want to confirm this order?';
    } else if (status === 'completed') {
        actionText = 'complete';
        confirmMessage = 'Are you sure you want to mark this order as completed?';
    } else if (status === 'cancelled') {
        actionText = 'cancel';
        confirmMessage = 'Are you sure you want to cancel this order? This action cannot be undone.';
    } else {
        actionText = 'update';
        confirmMessage = `Are you sure you want to update this order status to ${status}?`;
    }
    
    if (!confirm(confirmMessage)) {
        return;
    }
    
    try {
        let remarks = '';
        if (status === 'confirmed') {
            remarks = 'Order confirmed by staff';
        } else if (status === 'completed') {
            remarks = 'Order completed by staff';
        } else if (status === 'cancelled') {
            remarks = 'Order cancelled by staff';
        }
        
        const response = await fetch(`${window.API_BASE}/orders/update_order_status`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify({
                order_id: orderId,
                status: status,
                remarks: remarks
            })
        });
        
        const result = await response.json();
        
        if (result.success) {
            let successMessage = '';
            if (status === 'confirmed') {
                successMessage = 'Order confirmed successfully!';
            } else if (status === 'completed') {
                successMessage = 'Order marked as completed successfully!';
            } else if (status === 'cancelled') {
                successMessage = 'Order cancelled successfully!';
            } else {
                successMessage = `Order status updated to ${status} successfully!`;
            }
            alert(successMessage);
            loadOrders(); // Reload orders
        } else {
            alert('Error: ' + (result.message || 'Unknown error occurred'));
        }
    } catch (error) {
        console.error('Error updating order status:', error);
        alert('An error occurred while updating order status.');
    }
}

// View order details
function viewOrderDetails(orderId) {
    window.location.href = `order_details.html?order_id=${orderId}`;
}

// Load employees (Admin only)
async function loadEmployees() {
    try {
        const response = await fetch(`${window.API_BASE}/admin/get_employees`, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const result = await response.json();
        
        const employeesList = document.getElementById('employeesList');
        if (!employeesList) {
            console.error('employeesList element not found');
            return;
        }
        
        if (result.success && Array.isArray(result.data)) {
            adminState.employees = result.data;
        } else {
            adminState.employees = [];
        }
        populateManagerOptions();
        renderEmployeesTable();
    } catch (error) {
        console.error('Error loading employees:', error);
        document.getElementById('employeesList').innerHTML = '<p class="error-message">Error loading employees. Please try again later.</p>';
    }
}

function renderEmployeesTable() {
    const container = document.getElementById('employeesList');
    if (!container) return;

    if (!adminState.employees.length) {
        container.innerHTML = '<p class="empty-state">No employees found.</p>';
        return;
    }

    container.innerHTML = `
        <div class="table-responsive">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Role</th>
                        <th>Branch</th>
                        <th>Contact</th>
                        <th style="width: 160px;">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    ${adminState.employees.map(emp => `
                        <tr>
                            <td>${emp.name}</td>
                            <td>${getRoleLabel(emp.role)}</td>
                            <td>${emp.branch_name || 'Unassigned'}</td>
                            <td>${emp.contact_num || 'N/A'}</td>
                            <td>
                                <button class="btn btn-secondary" onclick="openEmployeeModal('edit', ${emp.employee_id})">Edit</button>
                                <button class="btn btn-danger" onclick="deactivateEmployeeAccount(${emp.employee_id})">Deactivate</button>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
}

// Initialize based on role
async function initializeAdminPanel() {
    try {
        const role = localStorage.getItem("role") || "guest";
        const userType = localStorage.getItem("user_type") || "guest";
        
        console.log('Initializing admin panel for role:', role, 'userType:', userType);
        
        // Wait for elements to be available
        const pageTitle = document.getElementById('pageTitle');
        if (!pageTitle) {
            console.error('pageTitle element not found, retrying...');
            setTimeout(initializeAdminPanel, 200);
            return;
        }
        
        // Update page title based on role
        if (role === "staff") {
            pageTitle.textContent = "📦 Order Management";
        } else if (role === "manager") {
            pageTitle.textContent = "📊 Manager Dashboard";
        } else {
            pageTitle.textContent = "⚙️ Admin Panel";
        }
        
        // Setup event listeners for dropdowns
        const analyticsPeriodSelect = document.getElementById('analyticsPeriod');
        if (analyticsPeriodSelect) {
            console.log('Setting up analytics period dropdown listener');
            analyticsPeriodSelect.addEventListener('change', (e) => {
                const period = e.target.value;
                console.log('Analytics period changed to:', period);
                // Map frontend values to backend values
                let backendPeriod = period;
                if (period === 'week') backendPeriod = 'weekly';
                else if (period === 'month') backendPeriod = 'monthly';
                else if (period === 'year') backendPeriod = 'annual';
                loadAnalytics(backendPeriod);
            });
            
            // Load analytics with default selected value on page load
            const defaultPeriod = analyticsPeriodSelect.value || 'monthly';
            let backendPeriod = defaultPeriod;
            if (defaultPeriod === 'week') backendPeriod = 'weekly';
            else if (defaultPeriod === 'month') backendPeriod = 'monthly';
            else if (defaultPeriod === 'year') backendPeriod = 'annual';
            
            // Load analytics after a short delay to ensure page is ready
            setTimeout(() => {
                if (defaultPeriod && defaultPeriod !== 'today') {
                    loadAnalytics(backendPeriod);
                }
            }, 500);
        } else {
            console.warn('analyticsPeriod dropdown not found');
        }
        
        const reportTypeSelect = document.getElementById('reportType');
        if (reportTypeSelect) {
            console.log('Setting up report type dropdown listener');
            reportTypeSelect.addEventListener('change', (e) => {
                const reportType = e.target.value;
                console.log('Report type changed to:', reportType);
                if (reportType) {
                    loadReports(reportType);
                }
            });
            
            // Ensure dropdown has a value
            if (!reportTypeSelect.value) {
                reportTypeSelect.value = 'items';
            }
            console.log('Initial report type:', reportTypeSelect.value);
        } else {
            console.warn('reportType dropdown not found');
        }
        
        // Show/hide sections based on role
        if (role === "admin") {
            // Admin: Show all sections including Analytics
            const adminManagerSections = document.getElementById('adminManagerSections');
            const adminOnlySections = document.getElementById('adminOnlySections');
            
            if (adminManagerSections) adminManagerSections.style.display = 'grid';
            if (adminOnlySections) adminOnlySections.style.display = 'block';
            
            const branchesSection = document.getElementById('branchesSection');
            const analyticsSection = document.getElementById('analyticsSection');
            const reportsSection = document.getElementById('reportsSection');
            const employeesSection = document.getElementById('employeesSection');
            const ordersSection = document.getElementById('ordersSection');
            const dashboardSection = document.getElementById('dashboardSection');
            const exportsSection = document.getElementById('exportsSection');
            const settingsSection = document.getElementById('settingsSection');
            
            if (branchesSection) branchesSection.style.display = 'block';
            if (analyticsSection) analyticsSection.style.display = 'block'; // Show Analytics for admin
            if (reportsSection) reportsSection.style.display = 'block';
            if (employeesSection) employeesSection.style.display = 'block';
            if (ordersSection) ordersSection.style.display = 'block';
            if (dashboardSection) dashboardSection.style.display = 'block';
            if (exportsSection) exportsSection.style.display = 'block';
            if (settingsSection) settingsSection.style.display = 'block';
            
            const branchForm = document.getElementById('branchForm');
            if (branchForm) {
                branchForm.addEventListener('submit', submitBranchForm);
            }
            const branchResetBtn = document.getElementById('branchResetBtn');
            if (branchResetBtn) {
                branchResetBtn.addEventListener('click', resetBranchForm);
            }
            const branchDeleteBtn = document.getElementById('branchDeleteBtn');
            if (branchDeleteBtn) {
                branchDeleteBtn.addEventListener('click', () => {
                    const branchId = document.getElementById('branchIdInput').value;
                    if (branchId) {
                        deleteBranch(parseInt(branchId, 10));
                    }
                });
            }
            const systemSettingsForm = document.getElementById('systemSettingsForm');
            if (systemSettingsForm) {
                systemSettingsForm.addEventListener('submit', saveSystemSettings);
            }
            ensureEmployeeModalRefs();
            initExportButtons();
            resetBranchForm();
            
            console.log('Loading data for admin...');
            
            // Get initial report type from dropdown
            const initialReportType = document.getElementById('reportType')?.value || 'items';
            console.log('Loading reports with initial type:', initialReportType);
            
            await Promise.all([
                loadDashboardSummary(),
                loadBranches(),
                loadReports(initialReportType), // Use dropdown value
                loadEmployees(),
                loadOrders(),
                loadSystemSettings()
            ]);
            
            // Load analytics separately after dropdown is set up
            const analyticsPeriodSelect = document.getElementById('analyticsPeriod');
            if (analyticsPeriodSelect) {
                const defaultPeriod = analyticsPeriodSelect.value || 'monthly';
                let backendPeriod = defaultPeriod;
                if (defaultPeriod === 'week') backendPeriod = 'weekly';
                else if (defaultPeriod === 'month') backendPeriod = 'monthly';
                else if (defaultPeriod === 'year') backendPeriod = 'annual';
                
                if (defaultPeriod && defaultPeriod !== 'today') {
                    setTimeout(() => loadAnalytics(backendPeriod), 300);
                }
            }
        } else if (role === "manager") {
            // Manager: Show reports, menu management (via reports), and orders
            // Analytics section hidden for managers (admin only)
            const adminManagerSections = document.getElementById('adminManagerSections');
            const adminOnlySections = document.getElementById('adminOnlySections');
            
            if (adminManagerSections) adminManagerSections.style.display = 'grid';
            if (adminOnlySections) adminOnlySections.style.display = 'none';
            
            const branchesSection = document.getElementById('branchesSection');
            const analyticsSection = document.getElementById('analyticsSection');
            const reportsSection = document.getElementById('reportsSection');
            const employeesSection = document.getElementById('employeesSection');
            const ordersSection = document.getElementById('ordersSection');
            
            if (branchesSection) branchesSection.style.display = 'block';
            if (analyticsSection) analyticsSection.style.display = 'none'; // Hide Analytics for managers
            if (reportsSection) reportsSection.style.display = 'block';
            if (employeesSection) employeesSection.style.display = 'none';
            if (ordersSection) ordersSection.style.display = 'block';
            
            console.log('Loading data for manager...');
            
            // Get initial report type from dropdown
            const initialReportType = document.getElementById('reportType')?.value || 'items';
            console.log('Loading reports with initial type:', initialReportType);
            
            await Promise.all([
                loadBranches(),
                loadReports(initialReportType), // Use dropdown value
                loadOrders()
            ]);
        } else if (role === "staff") {
            // Staff: Show only order management
            const adminManagerSections = document.getElementById('adminManagerSections');
            const adminOnlySections = document.getElementById('adminOnlySections');
            const ordersSection = document.getElementById('ordersSection');
            
            if (adminManagerSections) adminManagerSections.style.display = 'none';
            if (adminOnlySections) adminOnlySections.style.display = 'none';
            if (ordersSection) ordersSection.style.display = 'block';
            
            console.log('Loading data for staff...');
            await loadOrders();
        } else {
            console.warn('Unknown role:', role);
        }
    } catch (error) {
        console.error('Error initializing admin panel:', error);
    }
}

// Make functions globally available
window.updateOrderStatus = updateOrderStatus;
window.viewOrderDetails = viewOrderDetails;
window.editBranch = editBranch;
window.deleteBranch = deleteBranch;
window.openEmployeeModal = openEmployeeModal;
window.deactivateEmployeeAccount = deactivateEmployeeAccount;

// Initialize when DOM is ready
if (typeof window.initializeWhenReady === 'function') {
    window.initializeWhenReady(
        initializeAdminPanel,
        ['adminContent', 'pageTitle'], // Required elements
        {
            waitForAccessControl: true, // Wait for access control
            timeout: 5000
        }
    );
} else {
    // Fallback if dom-utils.js isn't loaded yet
    async function startAdminInitialization() {
        if (typeof window.applyPageAccessControl === 'function') {
            try {
                const accessControlPromise = window.applyPageAccessControl();
                const timeoutPromise = new Promise((resolve) => setTimeout(resolve, 2000));
                await Promise.race([accessControlPromise, timeoutPromise]);
            } catch (error) {
                console.error('Access control error:', error);
            }
        }
        
        let retries = 0;
        const maxRetries = 50;
        const tryInit = () => {
            const content = document.getElementById('adminContent');
            const title = document.getElementById('pageTitle');
            if (content && title) {
                initializeAdminPanel();
            } else if (retries < maxRetries) {
                retries++;
                setTimeout(tryInit, 100);
            } else {
                console.error('Admin elements not found after multiple retries');
            }
        };
        
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => {
                setTimeout(tryInit, 200);
            });
        } else {
            setTimeout(tryInit, 200);
        }
    }
    
    startAdminInitialization();
}
