'use strict';

(function () {
    const REFRESH_INTERVAL_MS = 30000;
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
    const CONFIRM_PROMPTS = {
        confirmed: 'Are you sure you want to confirm this order?',
        completed: 'Are you sure you want to mark this order as completed?',
        cancelled: 'Are you sure you want to cancel this order? This action cannot be undone.'
    };
    const SUCCESS_PROMPTS = {
        confirmed: 'Order confirmed successfully!',
        completed: 'Order marked as completed successfully!',
        cancelled: 'Order cancelled successfully!'
    };
    const STATUS_REMARKS = {
        confirmed: 'Order confirmed by staff',
        completed: 'Order completed by staff',
        cancelled: 'Order cancelled by staff'
    };

    if (typeof window.BASE_URL === 'undefined') {
        window.BASE_URL = window.location.origin + window.location.pathname.replace(/\/[^/]*$/, '');
    }
    if (typeof window.API_BASE === 'undefined') {
        window.API_BASE = window.BASE_URL + '/api';
    }

    let allOrders = [];
    let dashboardStats = { active_orders: 0, completed_orders: 0, pending_payments: 0 };
    let pendingPayments = [];
    let orderHistory = [];
    let currencies = [];
    let refreshTimer = null;
    let currentPaymentOrder = null;

    let paymentModalEl = null;
    let paymentModalCloseBtn = null;
    let paymentModalCancelBtn = null;
    let paymentAmountInput = null;
    let paymentCurrencySelect = null;
    let paymentMethodSelect = null;
    let paymentSubmitBtn = null;
    let paymentOrderLabel = null;
    let paymentDetailsLink = null;
    let paymentForm = null;

    function setSectionMessage(targetId, message, variant = 'info') {
        const target = document.getElementById(targetId);
        if (!target) return;
        const className = variant === 'error'
            ? 'error-message'
            : variant === 'empty'
                ? 'empty-state'
                : 'loading';
        target.innerHTML = `<p class="${className}">${message}</p>`;
    }

    function formatCurrency(amount) {
        const value = Number(amount) || 0;
        return `₱${value.toFixed(2)}`;
    }

    function calculatePoints(order) {
        if (!order) return null;
        const numericPoints = typeof order.earned_points === 'number'
            ? order.earned_points
            : parseInt(order.earned_points, 10);
        if (!Number.isNaN(numericPoints) && numericPoints > 0) {
            return numericPoints;
        }
        if (order.loyalty_id) {
            const amount = Number(order.total_amount || 0);
            const fallback = Math.floor(amount / 50);
            return fallback > 0 ? fallback : null;
        }
        return null;
    }

    function formatDateTime(value) {
        if (!value) return 'N/A';
        try {
            return new Date(value).toLocaleString();
        } catch (error) {
            return value;
        }
    }

    function formatStatusLabel(status) {
        const key = (status || '').toLowerCase();
        return STATUS_LABELS[key] || (status ? status.charAt(0).toUpperCase() + status.slice(1) : 'Pending');
    }

    function updateOverviewStats(stats = {}) {
        const activeEl = document.getElementById('activeOrdersCount');
        const completedEl = document.getElementById('completedOrdersCount');
        const pendingPaymentsEl = document.getElementById('pendingPaymentsCount');
        if (activeEl) activeEl.textContent = stats.active_orders ?? 0;
        if (completedEl) completedEl.textContent = stats.completed_orders ?? 0;
        if (pendingPaymentsEl) pendingPaymentsEl.textContent = stats.pending_payments ?? 0;
    }

    function buildActionButtons(order) {
        const buttons = [];

        if (order.status === 'pending') {
            buttons.push(
                `<button onclick="updateOrderStatus(${order.order_id}, 'confirmed')" class="btn btn-success">✓ Confirm Order</button>`,
                `<button onclick="updateOrderStatus(${order.order_id}, 'cancelled')" class="btn btn-danger">✗ Cancel Order</button>`
            );
        } else if (order.status === 'confirmed') {
            buttons.push(
                `<button onclick="updateOrderStatus(${order.order_id}, 'completed')" class="btn btn-success">✓ Mark as Completed</button>`,
                `<button onclick="updateOrderStatus(${order.order_id}, 'cancelled')" class="btn btn-danger">✗ Cancel Order</button>`
            );
        }

        buttons.push(
            `<button onclick="viewOrderDetails(${order.order_id})" class="btn btn-primary">View Details</button>`
        );

        return buttons.join('\n');
    }

    function renderOrders() {
        const ordersList = document.getElementById('ordersList');
        if (!ordersList) return;

        const filterElement = document.getElementById('orderStatusFilter');
        const selectedStatus = filterElement ? filterElement.value : 'all';

        let filteredOrders = allOrders;
        if (selectedStatus !== 'all') {
            filteredOrders = allOrders.filter(order => order.status === selectedStatus);
        }

        if (filteredOrders.length === 0) {
            setSectionMessage('ordersList', 'No orders found.', 'empty');
            return;
        }

        let html = '<div class="orders-container">';
        filteredOrders.forEach(order => {
            const orderDateText = order.order_date ? new Date(order.order_date).toLocaleString() : 'N/A';
            const statusClass = STATUS_CLASSES[order.status] || 'status-pending';
            const statusLabel = formatStatusLabel(order.status);
            const points = calculatePoints(order);

            html += `
                <div class="order-item">
                    <div class="order-header">
                        <h4>Order #${order.order_id}</h4>
                        <span class="order-status ${statusClass}">${statusLabel}</span>
                    </div>
                    <div class="order-details">
                        <div>
                            <strong>Customer:</strong>
                            <p>${order.customer_name || 'N/A'}</p>
                        </div>
                        <div>
                            <strong>Date & Time:</strong>
                            <p>${orderDateText}</p>
                        </div>
                        <div>
                            <strong>Total Amount:</strong>
                            <p>${formatCurrency(order.total_amount)}</p>
                        </div>
                        ${points !== null ? `
                        <div>
                            <strong>Points Earned:</strong>
                            <p>${points}</p>
                        </div>` : ''}
                    </div>
                    <div class="order-actions">
                        ${buildActionButtons(order)}
                    </div>
                </div>
            `;
        });
        html += '</div>';

        ordersList.innerHTML = html;
    }

    async function loadOrdersForStaff(showLoading = false) {
        if (showLoading) {
            setSectionMessage('ordersList', 'Loading orders...');
        }

        try {
            const response = await fetch(`${window.API_BASE}/orders/get_orders`, {
                credentials: 'include'
            });

            const result = await response.json();

            if (response.ok && result && result.success && Array.isArray(result.data)) {
                allOrders = result.data;
                renderOrders();
                return;
            }

            const message = result && result.message ? result.message : 'No orders found.';
            allOrders = [];
            setSectionMessage('ordersList', message, 'empty');
        } catch (error) {
            console.error('Error loading orders:', error);
            allOrders = [];
            setSectionMessage('ordersList', `Error loading orders: ${error.message}`, 'error');
        }
    }

    async function loadStaffDashboardData(showLoading = false) {
        if (showLoading) {
            setSectionMessage('paymentsList', 'Loading pending payments...');
            setSectionMessage('orderHistoryList', 'Loading order history...');
        }

        try {
            const response = await fetch(`${window.API_BASE}/orders/staff_dashboard`, {
                credentials: 'include'
            });
            const result = await response.json();

            if (response.ok && result && result.success && result.data) {
                dashboardStats = result.data.stats || dashboardStats;
                pendingPayments = Array.isArray(result.data.pending_payments) ? result.data.pending_payments : [];
                orderHistory = Array.isArray(result.data.order_history) ? result.data.order_history : [];
                updateOverviewStats(dashboardStats);
                renderPendingPayments();
                renderOrderHistory();
                return;
            }

            const message = result && result.message ? result.message : 'Unable to load dashboard data.';
            setSectionMessage('paymentsList', message, 'error');
            setSectionMessage('orderHistoryList', message, 'error');
        } catch (error) {
            console.error('Error loading staff dashboard:', error);
            setSectionMessage('paymentsList', `Error loading payments: ${error.message}`, 'error');
            setSectionMessage('orderHistoryList', `Error loading history: ${error.message}`, 'error');
        }
    }

    function renderPendingPayments() {
        const container = document.getElementById('paymentsList');
        if (!container) return;

        if (!pendingPayments.length) {
            setSectionMessage('paymentsList', 'All payments are up to date. Great work!', 'empty');
            return;
        }

        let rows = pendingPayments.map(order => `
            <tr>
                <td><strong>#${order.order_id}</strong><br><small>${formatDateTime(order.order_date)}</small></td>
                <td>${order.customer_name || 'Walk-in'}</td>
                <td>${formatCurrency(order.total_amount)}</td>
                <td><span class="status-pill ${STATUS_CLASSES[order.status] || ''}">${formatStatusLabel(order.status)}</span></td>
                <td>
                    <button class="btn btn-success" onclick="openPaymentModal(${order.order_id})">Record Payment</button>
                    <button class="btn btn-secondary" onclick="viewOrderDetails(${order.order_id})">View Details</button>
                </td>
            </tr>
        `).join('');

        container.innerHTML = `
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Order</th>
                            <th>Customer</th>
                            <th>Total</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${rows}
                    </tbody>
                </table>
            </div>
        `;
    }

    function renderOrderHistory() {
        const container = document.getElementById('orderHistoryList');
        if (!container) return;

        if (!orderHistory.length) {
            setSectionMessage('orderHistoryList', 'No order history yet. Process an order to see it here.', 'empty');
            return;
        }

        let html = '<div class="history-list">';
        orderHistory.forEach(entry => {
            html += `
                <div class="history-item">
                    <div class="history-header">
                        <strong>Order #${entry.order_id}</strong>
                        <span class="status-pill ${STATUS_CLASSES[entry.status] || ''}">${formatStatusLabel(entry.status)}</span>
                    </div>
                    <p class="history-meta">${entry.customer_name || 'Customer'} • ${formatCurrency(entry.total_amount)}</p>
                    <p class="history-remarks">${entry.remarks || 'Status update'}</p>
                    <small>${formatDateTime(entry.timestamp)}</small>
                </div>
            `;
        });
        html += '</div>';

        container.innerHTML = html;
    }

    async function loadCurrencies() {
        if (currencies.length > 0) {
            return currencies;
        }
        try {
            const response = await fetch(`${window.API_BASE}/menu/get_currencies`);
            const result = await response.json();
            if (response.ok && result && result.success && Array.isArray(result.data)) {
                currencies = result.data;
            } else {
                throw new Error(result && result.message ? result.message : 'Unable to load currencies');
            }
        } catch (error) {
            console.error('Error loading currencies:', error);
            throw error;
        }
        return currencies;
    }

    function populateCurrencySelect() {
        if (!paymentCurrencySelect) return;
        if (!currencies.length) return;
        paymentCurrencySelect.innerHTML = currencies.map(currency =>
            `<option value="${currency.currency_id}" data-code="${currency.code}" data-rate="${currency.rate}">
                ${currency.code} (${currency.symbol})
            </option>`
        ).join('');
        const phpCurrency = currencies.find(c => c.code === 'PHP');
        paymentCurrencySelect.value = phpCurrency ? phpCurrency.currency_id : currencies[0].currency_id;
    }

    async function openPaymentModal(orderId) {
        if (!paymentModalEl) return;
        const order = pendingPayments.find(item => item.order_id === Number(orderId));
        if (!order) {
            alert('Order not found. Please refresh and try again.');
            return;
        }
        try {
            await loadCurrencies();
            populateCurrencySelect();
        } catch (error) {
            alert('Unable to load currency list. Please try again later.');
            return;
        }
        currentPaymentOrder = order;
        if (paymentOrderLabel) {
            paymentOrderLabel.textContent = `Order #${order.order_id} • ${order.customer_name || 'Customer'}`;
        }
        if (paymentDetailsLink) {
            paymentDetailsLink.href = `order_details.html?order_id=${order.order_id}`;
        }
        if (paymentAmountInput) {
            paymentAmountInput.value = (order.total_amount || 0).toFixed(2);
        }
        paymentModalEl.classList.remove('hidden');
        paymentModalEl.setAttribute('aria-hidden', 'false');
    }

    function closePaymentModal() {
        if (!paymentModalEl) return;
        paymentModalEl.classList.add('hidden');
        paymentModalEl.setAttribute('aria-hidden', 'true');
        currentPaymentOrder = null;
        if (paymentForm) {
            paymentForm.reset();
        }
    }

    async function handlePaymentSubmit(event) {
        event.preventDefault();
        if (!currentPaymentOrder || !paymentAmountInput || !paymentCurrencySelect || !paymentMethodSelect) {
            return;
        }
        const amountPaid = parseFloat(paymentAmountInput.value);
        const currencyId = parseInt(paymentCurrencySelect.value, 10);
        const paymentMethod = paymentMethodSelect.value;

        if (Number.isNaN(amountPaid) || amountPaid <= 0) {
            alert('Enter a valid payment amount.');
            return;
        }
        if (Number.isNaN(currencyId)) {
            alert('Select a currency.');
            return;
        }

        paymentSubmitBtn.disabled = true;
        paymentSubmitBtn.textContent = 'Recording...';

        try {
            const response = await fetch(`${window.API_BASE}/payments/create_transaction`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({
                    order_id: currentPaymentOrder.order_id,
                    currency_id: currencyId,
                    payment_method: paymentMethod,
                    amount_paid: amountPaid
                })
            });
            const result = await response.json();

            if (!response.ok || !result || !result.success) {
                throw new Error(result && result.message ? result.message : 'Unable to record payment.');
            }

            alert('Payment recorded successfully!');
            closePaymentModal();
            await Promise.all([loadStaffDashboardData(), loadOrdersForStaff()]);
        } catch (error) {
            console.error('Error recording payment:', error);
            alert(`Error recording payment: ${error.message}`);
        } finally {
            paymentSubmitBtn.disabled = false;
            paymentSubmitBtn.textContent = 'Record Payment';
        }
    }

    async function updateOrderStatus(orderId, status) {
        const confirmMessage = CONFIRM_PROMPTS[status] || 'Are you sure you want to update this order?';
        if (!window.confirm(confirmMessage)) {
            return;
        }

        try {
            const response = await fetch(`${window.API_BASE}/orders/update_order_status`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include',
                body: JSON.stringify({
                    order_id: orderId,
                    status,
                    remarks: STATUS_REMARKS[status] || `Order ${status} by staff`
                })
            });

            const result = await response.json();

            if (!response.ok || !result || !result.success) {
                throw new Error(result && result.message ? result.message : 'Unable to update order status.');
            }

            alert(SUCCESS_PROMPTS[status] || 'Order status updated successfully!');
            await Promise.all([loadOrdersForStaff(), loadStaffDashboardData()]);
        } catch (error) {
            console.error('Error updating order status:', error);
            alert(`Error updating order status: ${error.message}`);
        }
    }

    function viewOrderDetails(orderId) {
        window.location.href = `order_details.html?order_id=${orderId}`;
    }

    function cacheModalElements() {
        paymentModalEl = document.getElementById('paymentModal');
        paymentModalCloseBtn = document.getElementById('paymentModalClose');
        paymentModalCancelBtn = document.getElementById('paymentModalCancel');
        paymentAmountInput = document.getElementById('paymentAmountInput');
        paymentCurrencySelect = document.getElementById('paymentCurrencySelect');
        paymentMethodSelect = document.getElementById('paymentMethodSelect');
        paymentSubmitBtn = document.getElementById('paymentSubmitBtn');
        paymentOrderLabel = document.getElementById('paymentOrderLabel');
        paymentDetailsLink = document.getElementById('paymentDetailsLink');
        paymentForm = document.getElementById('paymentForm');

        if (paymentModalCloseBtn) paymentModalCloseBtn.addEventListener('click', closePaymentModal);
        if (paymentModalCancelBtn) paymentModalCancelBtn.addEventListener('click', closePaymentModal);
        if (paymentForm) paymentForm.addEventListener('submit', handlePaymentSubmit);
        if (paymentModalEl) {
            paymentModalEl.addEventListener('click', (event) => {
                if (event.target === paymentModalEl) {
                    closePaymentModal();
                }
            });
        }
    }

    function startAutoRefresh() {
        if (refreshTimer) {
            clearInterval(refreshTimer);
        }
        refreshTimer = setInterval(() => {
            loadOrdersForStaff();
            loadStaffDashboardData();
        }, REFRESH_INTERVAL_MS);
    }

    async function initializeStaffDashboard() {
        const role = localStorage.getItem('role') || 'guest';
        if (role !== 'staff') {
            window.location.href = 'home.html';
            return;
        }

        cacheModalElements();

        const filterElement = document.getElementById('orderStatusFilter');
        if (filterElement) {
            filterElement.addEventListener('change', renderOrders);
        }

        await Promise.all([
            loadOrdersForStaff(true),
            loadStaffDashboardData(true)
        ]);
        try {
            await loadCurrencies();
        } catch (error) {
            console.warn('Currencies not available yet:', error);
        }
        startAutoRefresh();
    }

    document.addEventListener('DOMContentLoaded', async () => {
        if (typeof window.applyPageAccessControl === 'function') {
            try {
                await window.applyPageAccessControl();
            } catch (error) {
                console.error('Access control error on staff page:', error);
            }
        }
        await initializeStaffDashboard();
    });

    window.updateOrderStatus = updateOrderStatus;
    window.viewOrderDetails = viewOrderDetails;
    window.openPaymentModal = openPaymentModal;
})();
