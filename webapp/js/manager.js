'use strict';

(function () {
    if (typeof window.BASE_URL === 'undefined') {
        window.BASE_URL = window.location.origin + window.location.pathname.replace(/\/[^/]*$/, '');
    }
    if (typeof window.API_BASE === 'undefined') {
        window.API_BASE = window.BASE_URL + '/api';
    }

    const STATUS_CLASSES = {
        pending: 'status-pending',
        confirmed: 'status-confirmed',
        completed: 'status-completed',
        cancelled: 'status-cancelled'
    };

    const STATUS_LABELS = {
        pending: 'Pending',
        confirmed: 'Preparing',
        completed: 'Completed',
        cancelled: 'Cancelled'
    };

    const state = {
        orders: [],
        menuItems: [],
        categories: [],
        selectedMenu: null,
        transactions: [],
        employees: [],
        branchId: null
    };

    const getElement = (id) => document.getElementById(id);

    const formatCurrency = (value) => {
        const num = Number(value) || 0;
        return `₱${num.toFixed(2)}`;
    };

    const formatDateTime = (value) => {
        if (!value) return 'N/A';
        try {
            return new Date(value).toLocaleString();
        } catch (error) {
            return value;
        }
    };

    const statusLabel = (status) => STATUS_LABELS[status] || (status ? status.charAt(0).toUpperCase() + status.slice(1) : 'Pending');

    async function requestJSON(url, options = {}) {
        const response = await fetch(url, { credentials: 'include', ...options });
        const text = await response.text();
        let payload = {};
        try {
            payload = text ? JSON.parse(text) : {};
        } catch (error) {
            payload = {};
        }
        if (!response.ok || payload.success === false) {
            const message = payload && payload.message ? payload.message : `Request failed (${response.status})`;
            throw new Error(message);
        }
        return payload;
    }

    async function loadBranchInfo() {
        try {
            const result = await requestJSON(`${window.API_BASE}/admin/get_branches`);
            const branchInfoEl = getElement('branchInfo');
            const branches = result.data || [];
            if (branchInfoEl) {
                if (branches.length === 0) {
                    branchInfoEl.innerHTML = '<p class="error-message">Branch information not available.</p>';
                    return;
                }
                const branch = branches[0];
                state.branchId = branch.branch_id || null;
                branchInfoEl.innerHTML = `
                    <div class="branch-info-card">
                        <h3>${branch.name}</h3>
                        <p><strong>Address:</strong> ${branch.address || 'N/A'}</p>
                        <p><strong>Contact:</strong> ${branch.contact_num || 'N/A'}</p>
                        <p><strong>Manager:</strong> ${branch.manager_name || 'N/A'}</p>
                    </div>
                `;
            }
        } catch (error) {
            console.error('Branch info error:', error);
            const branchInfoEl = getElement('branchInfo');
            if (branchInfoEl) {
                branchInfoEl.innerHTML = `<p class="error-message">${error.message}</p>`;
            }
        }
    }

    async function loadPerformanceOverview() {
        try {
            const result = await requestJSON(`${window.API_BASE}/manager/dashboard`);
            const data = result.data || {};
            const stats = data.stats || {};

            setStatValue('statTodaySales', formatCurrency(stats.today_sales || 0));
            setStatValue('statTodayTransactions', stats.today_transactions || 0);
            setStatValue('statPendingOrders', stats.pending_orders || 0);
            setStatValue('statAvgTicket', formatCurrency(stats.avg_ticket_value || 0));

            renderTopItems(data.top_items || []);
            renderPendingPreview(data.pending_orders_preview || []);
            renderBranchInsights(data);
        } catch (error) {
            console.error('Performance load error:', error);
            const quickStats = getElement('quickStats');
            if (quickStats) {
                quickStats.innerHTML = `<p class="error-message">${error.message}</p>`;
            }
        }
    }

    function setStatValue(id, value) {
        const el = getElement(id);
        if (el) el.textContent = value;
    }

    function renderTopItems(items) {
        const list = getElement('topItemsList');
        if (!list) return;
        if (!items.length) {
            list.innerHTML = '<li class="empty-state">No data available yet.</li>';
            return;
        }
        list.innerHTML = items.map(item => `
            <li>
                <span>${item.name}</span>
                <span>${item.total_qty} sold • ${formatCurrency(item.total_sales)}</span>
            </li>
        `).join('');
    }

    function setInsightText(id, value) {
        const el = getElement(id);
        if (el) el.textContent = value;
    }

    function renderBranchInsights(data) {
        const stats = data.stats || {};
        const topItems = data.top_items || [];
        const pendingPreview = data.pending_orders_preview || [];

        const bestSeller = topItems.length ? topItems[0] : null;
        const bestName = bestSeller ? bestSeller.name : 'Awaiting sales';
        const bestQty = bestSeller ? (bestSeller.total_qty || bestSeller.total_quantity || 0) : 0;
        setInsightText('insightTopItem', bestName);
        setInsightText('insightTopItemQty', `${bestQty} sold this week`);

        const pendingOrders = stats.pending_orders || 0;
        setInsightText('insightPendingOrders', pendingOrders);

        const avgTicket = stats.avg_ticket_value || 0;
        setInsightText('insightAvgTicket', formatCurrency(avgTicket));
        setInsightText('insightAvgTicketNote', `Target: ${formatCurrency((stats.target_avg_ticket || avgTicket))}`);

        const nextOrderCard = getElement('insightNextOrder');
        if (!nextOrderCard) return;
        if (pendingPreview.length) {
            const next = pendingPreview[0];
            nextOrderCard.innerHTML = `
                <strong>Next Order Reminder</strong>
                <span>Order #${next.order_id} • ${formatCurrency(next.total_amount)}</span>
                <small>${next.customer_name || 'Guest'} • ${formatDateTime(next.order_date)}</small>
            `;
        } else {
            nextOrderCard.innerHTML = `
                <strong>Next Order Reminder</strong>
                <span>Everything is clear. Enjoy the calm before the rush!</span>
            `;
        }
    }

    function renderPendingPreview(orders) {
        const container = getElement('pendingPreviewList');
        if (!container) return;
        if (!orders.length) {
            container.innerHTML = '<p class="empty-state">No pending orders. Great job!</p>';
            return;
        }
        container.innerHTML = orders.map(order => `
            <div class="pending-preview-item">
                <div>
                    <strong>#${order.order_id}</strong><br>
                    <small>${order.customer_name || 'Customer'}</small>
                </div>
                <div style="text-align:right;">
                    <span>${formatCurrency(order.total_amount)}</span><br>
                    <small>${formatDateTime(order.order_date)}</small>
                </div>
            </div>
        `).join('');
    }

    async function loadReports(reportType = 'items') {
        try {
            const response = await requestJSON(`${window.API_BASE}/admin/get_reports?report_type=${encodeURIComponent(reportType)}`);
            const reportsContent = getElement('reportsContent');
            if (!reportsContent) return;

            const reports = response.data && response.data.reports ? response.data.reports : null;
            if (!reports) {
                reportsContent.innerHTML = '<p class="empty-state">No reports available.</p>';
                return;
            }

            const topItems = reports.top_items || [];
            const sales = reports.sales || {};

            let html = `
                <div class="report-card">
                    <h4>💰 Total Sales</h4>
                    <p class="stat-value">${formatCurrency(sales.total_sales_php || 0)}</p>
                    <p>${sales.total_transactions || 0} transactions</p>
                </div>
            `;

            if (topItems.length) {
                html += `
                    <div class="report-card">
                        <h4>🏅 Top Sellers</h4>
                        <ul class="top-items-list">
                            ${topItems.slice(0, 5).map(item => `
                                <li>
                                    <span>${item.name}</span>
                                    <span>${item.total_quantity} sold</span>
                                </li>
                            `).join('')}
                        </ul>
                    </div>
                `;
            }

            reportsContent.innerHTML = html;
        } catch (error) {
            console.error('Reports error:', error);
            const reportsContent = getElement('reportsContent');
            if (reportsContent) {
                reportsContent.innerHTML = `<p class="error-message">${error.message}</p>`;
            }
        }
    }

    async function loadOrders() {
        try {
            const response = await requestJSON(`${window.API_BASE}/orders/get_orders`);
            state.orders = response.data || [];
            renderOrders();
        } catch (error) {
            console.error('Orders error:', error);
            setSectionMessage('ordersList', error.message, 'error');
        }
    }

    function renderOrders() {
        const container = getElement('ordersList');
        if (!container) return;

        const filterSelect = getElement('orderStatusFilter');
        const filter = filterSelect ? filterSelect.value : 'all';
        let orders = state.orders;
        if (filter !== 'all') {
            orders = orders.filter(order => order.status === filter);
        }

        if (!orders.length) {
            setSectionMessage('ordersList', 'No orders found for the selected filter.', 'empty');
            return;
        }

        let html = '<div class="orders-container">';
        orders.forEach(order => {
            const statusClass = STATUS_CLASSES[order.status] || 'status-pending';
            html += `
                <div class="order-item">
                    <div class="order-header">
                        <h4>Order #${order.order_id}</h4>
                        <span class="order-status ${statusClass}">${statusLabel(order.status)}</span>
                    </div>
                    <div class="order-details">
                        <div>
                            <strong>Customer</strong>
                            <p>${order.customer_name || 'N/A'}</p>
                        </div>
                        <div>
                            <strong>Date</strong>
                            <p>${formatDateTime(order.order_date)}</p>
                        </div>
                        <div>
                            <strong>Total</strong>
                            <p>${formatCurrency(order.total_amount)}</p>
                        </div>
                    </div>
                    <div class="order-actions">
                        ${renderOrderActions(order)}
                        <button class="btn btn-primary" onclick="viewOrderDetails(${order.order_id})">View Details</button>
                    </div>
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;
    }

    function renderOrderActions(order) {
        const actions = [];
        if (order.status === 'pending') {
            actions.push(`<button class="btn btn-success" onclick="updateOrderStatus(${order.order_id}, 'confirmed')">Confirm</button>`);
            actions.push(`<button class="btn btn-danger" onclick="updateOrderStatus(${order.order_id}, 'cancelled')">Cancel</button>`);
        } else if (order.status === 'confirmed') {
            actions.push(`<button class="btn btn-success" onclick="updateOrderStatus(${order.order_id}, 'completed')">Complete</button>`);
            actions.push(`<button class="btn btn-danger" onclick="updateOrderStatus(${order.order_id}, 'cancelled')">Cancel</button>`);
        }
        return actions.join('');
    }

    async function updateOrderStatus(orderId, status) {
        if (!window.confirm(`Are you sure you want to mark this order as ${statusLabel(status)}?`)) {
            return;
        }
        try {
            const response = await requestJSON(`${window.API_BASE}/orders/update_order_status`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    order_id: orderId,
                    status,
                    remarks: `Order ${status} by manager`
                })
            });
            if (response.success) {
                await Promise.all([loadOrders(), loadPerformanceOverview()]);
                alert('Order updated successfully.');
            }
        } catch (error) {
            console.error('Update order status error:', error);
            alert(`Unable to update order: ${error.message}`);
        }
    }

    async function loadMenuData() {
        try {
            const [categoriesRes, menuRes] = await Promise.all([
                requestJSON(`${window.API_BASE}/menu/get_categories`),
                requestJSON(`${window.API_BASE}/menu/get_menu?currency=PHP`)
            ]);

            state.categories = categoriesRes.data || [];
            const menuData = menuRes.data || {};
            const categories = menuData.categories || [];

            state.menuItems = [];
            categories.forEach(category => {
                const items = category.items || [];
                items.forEach(item => {
                    state.menuItems.push({
                        menu_id: item.menu_id,
                        name: item.name,
                        description: item.description,
                        image_url: item.image_url,
                        price_php: item.price_php,
                        is_drink: item.is_drink,
                        is_available: item.is_available,
                        category_id: category.category_id,
                        category_name: category.name
                    });
                });
            });

            renderMenuTable();
            populateCategorySelect();
        } catch (error) {
            console.error('Menu load error:', error);
            setSectionMessage('menuItemsContainer', error.message, 'error');
        }
    }

    function renderMenuTable() {
        const container = getElement('menuItemsContainer');
        if (!container) return;

        if (!state.menuItems.length) {
            container.innerHTML = '<p class="empty-state">No menu items found.</p>';
            return;
        }

        container.innerHTML = `
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Category</th>
                            <th>Price</th>
                            <th>Type</th>
                            <th>Status</th>
                            <th style="width:140px;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${state.menuItems.map(item => `
                            <tr>
                                <td>${item.name}</td>
                                <td>${item.category_name || 'N/A'}</td>
                                <td>${formatCurrency(item.price_php)}</td>
                                <td>${item.is_drink ? 'Drink' : 'Food'}</td>
                                <td>
                                    <span class="badge ${item.is_available ? 'badge-available' : 'badge-unavailable'}">
                                        ${item.is_available ? 'Available' : 'Disabled'}
                                    </span>
                                </td>
                                <td>
                                    <button class="btn btn-secondary" onclick="editMenuItem(${item.menu_id})">Edit</button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    }

    function populateCategorySelect() {
        const select = getElement('menuCategoryInput');
        if (!select) return;
        if (!state.categories.length) {
            select.innerHTML = '<option value="">No categories found</option>';
            return;
        }
        select.innerHTML = state.categories.map(cat => `
            <option value="${cat.category_id}">${cat.name}</option>
        `).join('');
    }

    function resetMenuForm() {
        const form = getElement('menuForm');
        if (!form) return;
        form.reset();
        state.selectedMenu = null;
        const title = getElement('menuFormTitle');
        if (title) title.textContent = 'Add Menu Item';
        getElement('menuIdInput').value = '';
        getElement('menuIsAvailableInput').checked = true;
        const deleteBtn = getElement('menuDeleteBtn');
        if (deleteBtn) deleteBtn.style.display = 'none';
    }

    function editMenuItem(menuId) {
        const menu = state.menuItems.find(item => item.menu_id === Number(menuId));
        if (!menu) return;
        state.selectedMenu = menu;
        getElement('menuFormTitle').textContent = 'Edit Menu Item';
        getElement('menuIdInput').value = menu.menu_id;
        getElement('menuNameInput').value = menu.name || '';
        getElement('menuCategoryInput').value = menu.category_id || '';
        getElement('menuPriceInput').value = Number(menu.price_php || 0).toFixed(2);
        getElement('menuImageInput').value = menu.image_url || '';
        getElement('menuDescriptionInput').value = menu.description || '';
        getElement('menuIsDrinkInput').checked = !!menu.is_drink;
        getElement('menuIsAvailableInput').checked = !!menu.is_available;
        const deleteBtn = getElement('menuDeleteBtn');
        if (deleteBtn) deleteBtn.style.display = 'inline-block';
    }

    async function submitMenuForm(event) {
        event.preventDefault();
        const menuId = getElement('menuIdInput').value;
        const payload = {
            action: menuId ? 'update' : 'create',
            name: getElement('menuNameInput').value.trim(),
            category_id: parseInt(getElement('menuCategoryInput').value, 10),
            price_amount: parseFloat(getElement('menuPriceInput').value),
            description: getElement('menuDescriptionInput').value.trim(),
            image_url: getElement('menuImageInput').value.trim(),
            is_drink: getElement('menuIsDrinkInput').checked,
            is_available: getElement('menuIsAvailableInput').checked
        };

        if (!payload.name || Number.isNaN(payload.category_id) || Number.isNaN(payload.price_amount)) {
            alert('Please fill out all required fields.');
            return;
        }

        if (menuId) {
            payload.menu_id = parseInt(menuId, 10);
        }

        try {
            await requestJSON(`${window.API_BASE}/admin/manage_menu`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            await loadMenuData();
            resetMenuForm();
            alert('Menu item saved successfully.');
        } catch (error) {
            console.error('Menu save error:', error);
            alert(`Unable to save menu item: ${error.message}`);
        }
    }

    async function deleteMenuItem() {
        const menuId = getElement('menuIdInput').value;
        if (!menuId) return;
        if (!window.confirm('Delete this menu item? This action cannot be undone.')) {
            return;
        }
        try {
            await requestJSON(`${window.API_BASE}/admin/manage_menu`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: 'delete', menu_id: parseInt(menuId, 10) })
            });
            await loadMenuData();
            resetMenuForm();
            alert('Menu item deleted.');
        } catch (error) {
            console.error('Delete menu error:', error);
            alert(`Unable to delete menu item: ${error.message}`);
        }
    }

    async function loadEmployees() {
        try {
            const response = await requestJSON(`${window.API_BASE}/manager/staff`);
            state.employees = response.data || [];
            renderEmployees();
        } catch (error) {
            console.error('Employees error:', error);
            setSectionMessage('employeesList', error.message, 'error');
        }
    }

    function renderEmployees() {
        const container = getElement('employeesList');
        if (!container) return;
        if (!state.employees.length) {
            container.innerHTML = '<p class="empty-state">No employees assigned to this branch.</p>';
            return;
        }
        container.innerHTML = `
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Role</th>
                            <th>Contact</th>
                            <th>Orders (30d)</th>
                            <th>Last Activity</th>
                            <th style="width: 150px;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${state.employees.map(emp => `
                            <tr>
                                <td>${emp.name}</td>
                                <td>${emp.role ? emp.role.charAt(0).toUpperCase() + emp.role.slice(1) : 'N/A'}</td>
                                <td>${emp.contact_num || 'N/A'}</td>
                                <td>${emp.orders_processed_last30 || 0}</td>
                                <td>${emp.last_activity ? formatDateTime(emp.last_activity) : 'No activity'}</td>
                                <td>
                                    <button class="btn btn-secondary" onclick="managerOpenEmployeeModal('edit', ${emp.employee_id})">Edit</button>
                                    <button class="btn btn-danger" onclick="managerDeactivateEmployee(${emp.employee_id})">Deactivate</button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    }

    const managerEmployeeModal = {
        initialized: false
    };

    function ensureManagerEmployeeModal() {
        if (managerEmployeeModal.initialized) return managerEmployeeModal;
        managerEmployeeModal.modal = getElement('managerEmployeeModal');
        managerEmployeeModal.form = getElement('managerEmployeeForm');
        managerEmployeeModal.title = getElement('managerEmployeeModalTitle');
        managerEmployeeModal.idInput = getElement('managerEmployeeIdInput');
        managerEmployeeModal.nameInput = getElement('managerEmployeeNameInput');
        managerEmployeeModal.usernameInput = getElement('managerEmployeeUsernameInput');
        managerEmployeeModal.passwordInput = getElement('managerEmployeePasswordInput');
        managerEmployeeModal.contactInput = getElement('managerEmployeeContactInput');
        managerEmployeeModal.errorBox = getElement('managerEmployeeModalError');
        managerEmployeeModal.closeBtn = getElement('managerEmployeeModalClose');
        managerEmployeeModal.cancelBtn = getElement('managerEmployeeModalCancel');

        if (managerEmployeeModal.form) {
            managerEmployeeModal.form.addEventListener('submit', handleManagerEmployeeSubmit);
        }
        const closeModal = () => closeManagerEmployeeModal();
        if (managerEmployeeModal.closeBtn) managerEmployeeModal.closeBtn.addEventListener('click', closeModal);
        if (managerEmployeeModal.cancelBtn) managerEmployeeModal.cancelBtn.addEventListener('click', closeModal);
        if (managerEmployeeModal.modal) {
            managerEmployeeModal.modal.addEventListener('click', (event) => {
                if (event.target === managerEmployeeModal.modal) {
                    closeManagerEmployeeModal();
                }
            });
        }

        managerEmployeeModal.initialized = true;
        return managerEmployeeModal;
    }

    function managerOpenEmployeeModal(mode = 'create', employeeId = null) {
        const refs = ensureManagerEmployeeModal();
        if (!refs.modal) return;
        setManagerEmployeeModalError('');
        refs.form.dataset.mode = mode;
        refs.idInput.value = employeeId ? String(employeeId) : '';
        refs.passwordInput.value = '';
        refs.usernameInput.disabled = mode === 'edit';
        refs.passwordInput.placeholder = mode === 'edit' ? 'Leave blank to keep password' : '******';

        if (mode === 'edit' && employeeId) {
            const employee = state.employees.find(emp => emp.employee_id === Number(employeeId));
            if (!employee) {
                alert('Unable to load employee details.');
                return;
            }
            refs.title.textContent = `Edit ${employee.name}`;
            refs.nameInput.value = employee.name || '';
            refs.usernameInput.value = employee.username || '';
            refs.contactInput.value = employee.contact_num || '';
        } else {
            refs.title.textContent = 'Add Staff Member';
            refs.form.reset();
            refs.usernameInput.disabled = false;
        }

        refs.modal.classList.add('show');
        refs.modal.setAttribute('aria-hidden', 'false');
    }

    function closeManagerEmployeeModal() {
        const refs = ensureManagerEmployeeModal();
        if (!refs.modal) return;
        refs.modal.classList.remove('show');
        refs.modal.setAttribute('aria-hidden', 'true');
        setManagerEmployeeModalError('');
    }

    function setManagerEmployeeModalError(message) {
        const refs = ensureManagerEmployeeModal();
        if (!refs.errorBox) return;
        if (message) {
            refs.errorBox.style.display = 'block';
            refs.errorBox.textContent = message;
        } else {
            refs.errorBox.style.display = 'none';
            refs.errorBox.textContent = '';
        }
    }

    async function handleManagerEmployeeSubmit(event) {
        event.preventDefault();
        const refs = ensureManagerEmployeeModal();
        if (!state.branchId) {
            setManagerEmployeeModalError('Branch information not loaded yet.');
            return;
        }
        const mode = refs.form.dataset.mode || 'create';
        const payload = {
            action: mode === 'edit' ? 'update' : 'create',
            name: refs.nameInput.value.trim(),
            username: refs.usernameInput.value.trim(),
            contact_num: refs.contactInput.value.trim(),
            role: 'staff',
            branch_id: state.branchId
        };
        if (!payload.name) {
            setManagerEmployeeModalError('Full name is required.');
            return;
        }
        if (mode === 'create' && !payload.username) {
            setManagerEmployeeModalError('Username is required.');
            return;
        }
        const password = refs.passwordInput.value;
        if (password) {
            if (password.length < 6) {
                setManagerEmployeeModalError('Password must be at least 6 characters long.');
                return;
            }
            payload.password = password;
        } else if (mode === 'create') {
            setManagerEmployeeModalError('Password is required for new staff.');
            return;
        }
        if (mode === 'edit') {
            payload.employee_id = Number(refs.idInput.value);
        }

        try {
            const response = await requestJSON(`${window.API_BASE}/admin/manage_employee`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (response.success) {
                closeManagerEmployeeModal();
                await loadEmployees();
                alert('Staff record saved.');
            }
        } catch (error) {
            console.error('Manager employee save error:', error);
            setManagerEmployeeModalError(error.message);
        }
    }

    async function managerDeactivateEmployee(employeeId) {
        if (!window.confirm('Deactivate this staff member?')) {
            return;
        }
        try {
            const response = await requestJSON(`${window.API_BASE}/admin/manage_employee`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: 'deactivate', employee_id: employeeId })
            });
            if (response.success) {
                await loadEmployees();
                alert('Staff deactivated successfully.');
            }
        } catch (error) {
            console.error('Deactivate staff error:', error);
            alert(error.message);
        }
    }

    async function loadTransactions(filters = {}) {
        const params = new URLSearchParams();
        Object.entries(filters).forEach(([key, value]) => {
            if (value !== null && value !== undefined && value !== '') {
                params.append(key, value);
            }
        });
        const query = params.toString() ? `?${params.toString()}` : '';
        try {
            const response = await requestJSON(`${window.API_BASE}/manager/transactions${query}`);
            state.transactions = response.data || [];
            renderTransactions();
        } catch (error) {
            console.error('Transactions error:', error);
            setSectionMessage('transactionsList', error.message, 'error');
        }
    }

    function renderTransactions() {
        const container = getElement('transactionsList');
        if (!container) return;
        if (!state.transactions.length) {
            container.innerHTML = '<p class="empty-state">No transactions for the selected filters.</p>';
            renderTransactionSummary([]);
            return;
        }

        container.innerHTML = `
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Order</th>
                            <th>Method</th>
                            <th>Amount</th>
                            <th>Date</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${state.transactions.map(trx => `
                            <tr>
                                <td>${trx.transaction_id}</td>
                                <td>#${trx.order_id}</td>
                                <td>${trx.payment_method}</td>
                                <td>${formatCurrency(trx.amount_paid_php || 0)}</td>
                                <td>${formatDateTime(trx.transaction_date)}</td>
                                <td>${trx.status ? trx.status.charAt(0).toUpperCase() + trx.status.slice(1) : 'N/A'}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
        renderTransactionSummary(state.transactions);
    }

    function renderTransactionSummary(transactions) {
        const countEl = getElement('trxSummaryCount');
        const amountEl = getElement('trxSummaryAmount');
        const pendingEl = getElement('trxSummaryPending');
        if (!countEl || !amountEl || !pendingEl) return;

        const count = transactions.length;
        let totalAmount = 0;
        let pendingAmount = 0;
        transactions.forEach(trx => {
            const amount = Number(trx.amount_paid_php ?? trx.amount_paid ?? 0);
            totalAmount += amount;
            if ((trx.status || '').toLowerCase() === 'pending') {
                pendingAmount += amount;
            }
        });

        countEl.textContent = count;
        amountEl.textContent = formatCurrency(totalAmount);
        pendingEl.textContent = formatCurrency(pendingAmount);
    }

    function setSectionMessage(targetId, message, variant = 'info') {
        const target = getElement(targetId);
        if (!target) return;
        const className = variant === 'error'
            ? 'error-message'
            : variant === 'empty'
                ? 'empty-state'
                : 'loading';
        target.innerHTML = `<p class="${className}">${message}</p>`;
    }

    function handleTransactionFilters(event) {
        event.preventDefault();
        const formData = new FormData(event.target);
        const filters = {};
        formData.forEach((value, key) => { filters[key] = value; });
        loadTransactions(filters);
    }

    function attachEventListeners() {
        const reportTypeSelect = getElement('reportType');
        if (reportTypeSelect) {
            reportTypeSelect.addEventListener('change', (event) => {
                loadReports(event.target.value);
            });
        }

        const orderFilter = getElement('orderStatusFilter');
        if (orderFilter) {
            orderFilter.addEventListener('change', () => renderOrders());
        }

        const menuForm = getElement('menuForm');
        if (menuForm) {
            menuForm.addEventListener('submit', submitMenuForm);
        }

        const menuResetBtn = getElement('menuFormReset');
        if (menuResetBtn) {
            menuResetBtn.addEventListener('click', resetMenuForm);
        }

        const menuDeleteBtn = getElement('menuDeleteBtn');
        if (menuDeleteBtn) {
            menuDeleteBtn.addEventListener('click', deleteMenuItem);
        }

        const transactionFilters = getElement('transactionFilters');
        if (transactionFilters) {
            transactionFilters.addEventListener('submit', handleTransactionFilters);
        }

        ensureManagerEmployeeModal();
    }

    async function initializeManagerDashboard() {
        const role = localStorage.getItem('role') || 'guest';
        if (role !== 'manager' && role !== 'admin') {
            window.location.href = 'home.html';
            return;
        }

        attachEventListeners();

        await Promise.all([
            loadBranchInfo(),
            loadPerformanceOverview(),
            loadReports('items'),
            loadOrders(),
            loadMenuData(),
            loadEmployees(),
            loadTransactions()
        ]).catch(error => console.error('Initialization error:', error));
    }

    document.addEventListener('DOMContentLoaded', async () => {
        if (typeof window.applyPageAccessControl === 'function') {
            try {
                await window.applyPageAccessControl();
            } catch (error) {
                console.error('Access control error:', error);
            }
        }
        initializeManagerDashboard();
    });

    window.updateOrderStatus = updateOrderStatus;
    window.viewOrderDetails = (orderId) => { window.location.href = `order_details.html?order_id=${orderId}`; };
    window.editMenuItem = editMenuItem;
    window.managerOpenEmployeeModal = managerOpenEmployeeModal;
    window.managerDeactivateEmployee = managerDeactivateEmployee;
})();


