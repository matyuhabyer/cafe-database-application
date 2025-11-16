# Role-Based Access Control Implementation

This document describes the role-based access control (RBAC) system implemented across all pages in The Waiting Room Café application.

## Overview

The RBAC system ensures that users can only access pages and features appropriate to their role. Access control is enforced both on the frontend (page level) and backend (API level).

## User Roles

The system supports the following roles:

1. **Guest** - Not logged in
2. **Customer** - Logged in as a customer
3. **Staff** - Logged in as an employee with staff role
4. **Manager** - Logged in as an employee with manager role
5. **Admin** - Logged in as an employee with admin role

## Implementation Components

### 1. Backend Session Verification

**Endpoint**: `/api/auth/verify_session`

**File**: `src/main/java/itdbadm/cafedbapp/api/auth/VerifySessionServlet.java`

- Verifies if user session is valid
- Returns current user's role, user_type, and user_id
- Invalidates session if no user data is found

### 2. Frontend Access Control Utility

**File**: `src/main/webapp/js/auth.js`

Provides utility functions for:
- Getting current user role
- Checking user type (customer/employee)
- Verifying session with server
- Enforcing page-level access control

**Key Functions**:
- `getCurrentRole()` - Get user's role from localStorage
- `isLoggedIn()` - Check if user is logged in
- `isCustomer()` - Check if user is a customer
- `isEmployee()` - Check if user is an employee
- `isAdmin()` - Check if user is admin
- `isManager()` - Check if user is manager
- `isStaff()` - Check if user is staff
- `verifySession()` - Verify session with server
- `enforceAccessControl()` - Enforce access control with custom options
- `applyPageAccessControl()` - Apply pre-configured access control based on current page

## Page Access Configuration

Each page has specific access requirements defined in `PAGE_ACCESS` object:

| Page | Access Requirements | Redirect If Logged In |
|------|-------------------|----------------------|
| `index.html` (Login) | Allow guests | Yes → `products.html` |
| `register.html` | Allow guests | Yes → `products.html` |
| `products.html` (Menu) | Allow guests | No |
| `cart.html` | Customer only | No |
| `orders.html` | Customer only | No |
| `profile.html` | Any logged-in user | No |
| `admin.html` | Admin, Manager, or Staff | No |
| `test-servlets.html` | Admin only | No |

## Page-Level Implementation

All pages include the access control script and apply it on page load:

```html
<!-- Role-Based Access Control -->
<script src="js/auth.js"></script>
<script>
  document.addEventListener('DOMContentLoaded', function() {
    applyPageAccessControl();
  });
</script>
```

### Example: Customer-Only Page (cart.html)

```html
<!-- Role-Based Access Control -->
<script src="js/auth.js"></script>
<script>
  // Apply access control: require customer login
  document.addEventListener('DOMContentLoaded', function() {
    applyPageAccessControl();
  });
</script>
```

### Example: Public Page (products.html)

```html
<!-- Role-Based Access Control -->
<script src="js/auth.js"></script>
<script>
  // Apply access control: allow guests (public menu)
  document.addEventListener('DOMContentLoaded', function() {
    applyPageAccessControl();
  });
</script>
```

### Example: Employee-Only Page (admin.html)

```html
<!-- Role-Based Access Control -->
<script src="js/auth.js"></script>
<script>
  // Apply access control: require admin/manager/staff login
  document.addEventListener('DOMContentLoaded', function() {
    applyPageAccessControl();
  });
</script>
```

## Access Control Flow

1. **Page Load**: `applyPageAccessControl()` is called
2. **Session Verification**: Server session is verified via `/api/auth/verify_session`
3. **Role Check**: Current role is checked against page requirements
4. **Access Decision**:
   - If access allowed → Page loads normally
   - If access denied → User is redirected with appropriate message
5. **Redirect Logic**:
   - Customers → Redirected to `products.html`
   - Employees → Redirected to `admin.html`
   - Guests → Redirected to `index.html` (login)

## Security Features

1. **Server-Side Verification**: Every page load verifies session with server
2. **Session Invalidation**: Invalid sessions are cleared from localStorage
3. **Role Validation**: Roles are validated against server data
4. **Automatic Redirects**: Unauthorized users are automatically redirected
5. **User Feedback**: Clear error messages inform users why access was denied

## Custom Access Control

For custom access control, use `enforceAccessControl()` directly:

```javascript
await enforceAccessControl({
  allowedRoles: ['customer', 'admin'],
  requireLogin: true,
  allowGuest: false,
  redirectTo: 'index.html',
  redirectIfLoggedIn: null
});
```

## Backend API Protection

All API endpoints are also protected using `SessionUtil`:

- `SessionUtil.isCustomerLoggedIn()` - Customer-only endpoints
- `SessionUtil.isEmployeeLoggedIn()` - Employee-only endpoints
- `SessionUtil.isAdminOrManager()` - Admin/Manager-only endpoints

## Testing Access Control

To test access control:

1. **As Guest**: Try accessing protected pages → Should redirect to login
2. **As Customer**: Try accessing admin pages → Should redirect to products
3. **As Staff**: Try accessing admin pages → Should allow access
4. **As Manager**: Try accessing admin pages → Should allow access
5. **As Admin**: Try accessing all pages → Should allow access

## Notes

- Access control runs on every page load
- Session verification happens asynchronously
- localStorage is used for client-side role storage
- Server session is the source of truth
- Invalid sessions are automatically cleared

