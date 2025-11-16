# Role-Based Access Control Summary

This document summarizes the role-based access control implementation based on the specified requirements.

## Role Definitions & Permissions

### 1. Admin
**Full Control Over System**

**Permissions:**
- ✅ Manage branches (view, create, update)
- ✅ Manage employees (create, update, deactivate)
- ✅ Manage menu items and pricing
- ✅ View all reports (all branches)
- ✅ View all orders (all branches)
- ✅ Manage user accounts

**UI Access:**
- Admin Panel shows all sections:
  - Branches Management
  - Employee Management
  - Reports (all branches)
  - Order Management (all branches)

**API Endpoints:**
- `/api/admin/get_branches` - All branches
- `/api/admin/manage_employee` - Admin only
- `/api/admin/get_employees` - Admin only
- `/api/admin/manage_menu` - Admin/Manager
- `/api/admin/get_reports` - All branches
- `/api/orders/get_orders` - All orders

---

### 2. Manager
**Branch-Specific Operations**

**Permissions:**
- ✅ View sales reports (their branch only)
- ✅ Manage menu items (their branch context)
- ✅ Monitor transactions (their branch only)
- ✅ View orders (their branch only)
- ✅ View branch information
- ❌ Cannot manage employees (Admin only)
- ❌ Cannot manage branches (Admin only)

**UI Access:**
- Manager Dashboard shows:
  - Branch Information (their branch)
  - Reports (branch-specific)
  - Order Management (branch-specific)

**API Endpoints:**
- `/api/admin/get_branches` - Their branch only
- `/api/admin/manage_menu` - Admin/Manager
- `/api/admin/get_reports` - Branch-specific
- `/api/orders/get_orders` - Branch-specific

**Branch Restrictions:**
- All queries filtered by `branch_id` from session
- Cannot access other branches' data

---

### 3. Staff
**Order Confirmation and Completion**

**Permissions:**
- ✅ View orders (their branch only)
- ✅ Confirm pending orders
- ✅ Complete confirmed orders
- ✅ View order details
- ❌ Cannot view reports
- ❌ Cannot manage menu
- ❌ Cannot manage employees
- ❌ Cannot manage branches

**UI Access:**
- Order Management Dashboard shows:
  - Order Management (branch-specific)
  - Order status update buttons (Confirm/Complete)

**API Endpoints:**
- `/api/orders/get_orders` - Branch-specific
- `/api/orders/update_order_status` - Staff/Manager/Admin
- `/api/orders/get_order_details` - Branch-specific

**Branch Restrictions:**
- Can only process orders from their assigned branch
- All order queries filtered by `branch_id` from session

---

### 4. Customer
**Ordering and Account Management**

**Permissions:**
- ✅ Register account
- ✅ Browse menu (public)
- ✅ Customize items (drink options, extras)
- ✅ Place online orders
- ✅ Make payments
- ✅ Track loyalty points
- ✅ View own order history
- ✅ View own profile
- ❌ Cannot access admin panel
- ❌ Cannot view other customers' orders

**UI Access:**
- Products/Menu page (public)
- Cart page (customer only)
- Orders page (own orders only)
- Profile page (own profile only)

**API Endpoints:**
- `/api/menu/get_menu` - Public
- `/api/menu/get_item` - Public
- `/api/orders/create_order` - Customer only
- `/api/payments/create_transaction` - Customer/Employee
- `/api/orders/get_orders` - Own orders only
- `/api/auth/get_profile` - Own profile only
- `/api/loyalty/get_loyalty` - Own loyalty card only

---

## Implementation Details

### Page Access Control

| Page | Admin | Manager | Staff | Customer | Guest |
|------|-------|---------|-------|----------|-------|
| `index.html` (Login) | Redirect | Redirect | Redirect | Redirect | ✅ |
| `register.html` | Redirect | Redirect | Redirect | Redirect | ✅ |
| `products.html` (Menu) | ✅ | ✅ | ✅ | ✅ | ✅ |
| `cart.html` | ❌ | ❌ | ❌ | ✅ | ❌ |
| `orders.html` | ❌ | ❌ | ❌ | ✅ | ❌ |
| `profile.html` | ✅ | ✅ | ✅ | ✅ | ❌ |
| `admin.html` | ✅ | ✅ | ✅ | ❌ | ❌ |

### Admin Panel Features by Role

| Feature | Admin | Manager | Staff |
|---------|-------|---------|-------|
| Branches Management | ✅ All | ✅ Own Branch | ❌ |
| Employee Management | ✅ | ❌ | ❌ |
| Reports | ✅ All Branches | ✅ Own Branch | ❌ |
| Order Management | ✅ All Branches | ✅ Own Branch | ✅ Own Branch |
| Menu Management | ✅ | ✅ | ❌ |

### API Endpoint Restrictions

| Endpoint | Admin | Manager | Staff | Customer |
|----------|-------|---------|-------|----------|
| `/api/admin/manage_employee` | ✅ | ❌ | ❌ | ❌ |
| `/api/admin/get_employees` | ✅ | ❌ | ❌ | ❌ |
| `/api/admin/manage_menu` | ✅ | ✅ | ❌ | ❌ |
| `/api/admin/get_reports` | ✅ All | ✅ Branch | ❌ | ❌ |
| `/api/admin/get_branches` | ✅ All | ✅ Own | ❌ | ❌ |
| `/api/orders/get_orders` | ✅ All | ✅ Branch | ✅ Branch | ✅ Own |
| `/api/orders/update_order_status` | ✅ | ✅ | ✅ | ❌ |
| `/api/orders/create_order` | ❌ | ❌ | ❌ | ✅ |
| `/api/payments/create_transaction` | ✅ | ✅ | ✅ | ✅ |

### Branch Restrictions

**Enforced in:**
- `UpdateOrderStatusServlet.java` - Staff/Manager can only process orders from their branch
- `CreateTransactionServlet.java` - Staff/Manager can only record payments for their branch
- `GetOrdersServlet.java` - Staff/Manager see only their branch's orders
- `GetReportsServlet.java` - Manager sees only their branch's reports
- `GetBranchesServlet.java` - Manager sees only their branch

**Admin Override:**
- Admins can access all branches
- No branch filtering for admin role

---

## Security Features

1. **Session Verification**: All pages verify session with server on load
2. **Role Validation**: Roles validated against server session data
3. **Branch Isolation**: Staff/Managers restricted to their branch
4. **API Protection**: All endpoints check role and branch restrictions
5. **Automatic Redirects**: Unauthorized access redirects with clear messages

---

## Testing Checklist

- [ ] Admin can access all features
- [ ] Manager can only see their branch's data
- [ ] Staff can only see and process their branch's orders
- [ ] Staff cannot access reports or menu management
- [ ] Customer can place orders and view their own data
- [ ] Customers cannot access admin panel
- [ ] Branch restrictions enforced in all relevant APIs
- [ ] Session expiration handled properly
- [ ] Unauthorized access redirects appropriately

