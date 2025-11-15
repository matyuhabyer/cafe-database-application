# Servlet Testing Guide

## Overview
This guide shows you how to test the LoginServlet and RegisterServlet endpoints.

## Endpoints

### Login Servlet
- **URL**: `http://localhost:8080/cafedbapp/api/auth/login`
- **Method**: `POST`
- **Content-Type**: `application/json`

### Register Servlet
- **URL**: `http://localhost:8080/cafedbapp/api/auth/register`
- **Method**: `POST`
- **Content-Type**: `application/json`

---

## Method 1: Using the Test HTML Page (Easiest)

1. **Start your server** (Tomcat/GlassFish in NetBeans)
2. **Open**: `http://localhost:8080/cafedbapp/test-servlets.html`
3. **Fill in the form** and click the buttons
4. **Check the browser console** (F12) for responses

---

## Method 2: Using cURL (Command Line)

### Test Login (Customer)
```bash
curl -X POST http://localhost:8080/cafedbapp/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"testuser\",\"password\":\"password123\",\"role\":\"customer\"}"
```

### Test Login (Employee - Staff)
```bash
curl -X POST http://localhost:8080/cafedbapp/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"staff1\",\"password\":\"password123\",\"role\":\"staff\"}"
```

### Test Login (Employee - Manager)
```bash
curl -X POST http://localhost:8080/cafedbapp/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"manager1\",\"password\":\"password123\",\"role\":\"manager\"}"
```

### Test Login (Employee - Admin)
```bash
curl -X POST http://localhost:8080/cafedbapp/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin1\",\"password\":\"password123\",\"role\":\"admin\"}"
```

### Test Register
```bash
curl -X POST http://localhost:8080/cafedbapp/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"newuser\",\"password\":\"password123\",\"name\":\"John Doe\",\"email\":\"john@example.com\",\"phone_num\":\"1234567890\"}"
```

### Test with Cookie Session (Login then use session)
```bash
# Login and save cookies
curl -X POST http://localhost:8080/cafedbapp/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"testuser\",\"password\":\"password123\",\"role\":\"customer\"}" \
  -c cookies.txt

# Use session for subsequent requests
curl -X GET http://localhost:8080/cafedbapp/api/some-protected-endpoint \
  -b cookies.txt
```

---

## Method 3: Using Postman

### Setup Postman Request

1. **Create New Request**
   - Method: `POST`
   - URL: `http://localhost:8080/cafedbapp/api/auth/login`

2. **Headers Tab**
   - Key: `Content-Type`
   - Value: `application/json`

3. **Body Tab**
   - Select: `raw`
   - Select: `JSON` (dropdown)
   - Enter JSON:
   ```json
   {
     "username": "testuser",
     "password": "password123",
     "role": "customer"
   }
   ```

4. **Click Send**

### Test Register in Postman

1. **URL**: `http://localhost:8080/cafedbapp/api/auth/register`
2. **Method**: `POST`
3. **Headers**: `Content-Type: application/json`
4. **Body** (JSON):
   ```json
   {
     "username": "newuser",
     "password": "password123",
     "name": "John Doe",
     "email": "john@example.com",
     "phone_num": "1234567890"
   }
   ```

---

## Method 4: Using JavaScript (Browser Console)

Open browser console (F12) and run:

### Test Login
```javascript
fetch('http://localhost:8080/cafedbapp/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  credentials: 'include', // Important for sessions
  body: JSON.stringify({
    username: 'testuser',
    password: 'password123',
    role: 'customer'
  })
})
.then(response => response.json())
.then(data => {
  console.log('Success:', data);
})
.catch(error => {
  console.error('Error:', error);
});
```

### Test Register
```javascript
fetch('http://localhost:8080/cafedbapp/api/auth/register', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  credentials: 'include',
  body: JSON.stringify({
    username: 'newuser',
    password: 'password123',
    name: 'John Doe',
    email: 'john@example.com',
    phone_num: '1234567890'
  })
})
.then(response => response.json())
.then(data => {
  console.log('Success:', data);
})
.catch(error => {
  console.error('Error:', error);
});
```

---

## Expected Responses

### Successful Login Response
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "user_id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "phone_num": "1234567890",
    "loyalty_id": 1,
    "card_number": "LC-000001",
    "points": 0,
    "role": "customer"
  }
}
```

### Successful Register Response
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "customer_id": 2,
    "name": "Jane Doe",
    "email": "jane@example.com",
    "phone_num": "0987654321",
    "loyalty_id": 2,
    "card_number": "LC-000002",
    "points": 0,
    "role": "customer"
  }
}
```

### Error Response (Invalid Credentials)
```json
{
  "success": false,
  "message": "Invalid username or password",
  "error": "Invalid username or password"
}
```

### Error Response (Missing Fields)
```json
{
  "success": false,
  "message": "Missing required fields: username, password, role",
  "error": "Missing required fields: username, password, role"
}
```

### Error Response (Database Connection Failed)
```json
{
  "success": false,
  "message": "Database connection failed",
  "error": "Database connection failed"
}
```

---

## Test Scenarios

### Login Tests

1. **Valid Customer Login**
   - Username: existing customer username
   - Password: correct password
   - Role: "customer"
   - Expected: 200 OK with user data

2. **Valid Employee Login (Staff)**
   - Username: existing staff username
   - Password: correct password
   - Role: "staff"
   - Expected: 200 OK with employee data

3. **Invalid Username**
   - Username: "nonexistent"
   - Password: any
   - Role: "customer"
   - Expected: 401 Unauthorized

4. **Invalid Password**
   - Username: existing username
   - Password: "wrongpassword"
   - Role: "customer"
   - Expected: 401 Unauthorized

5. **Missing Fields**
   - Send incomplete JSON
   - Expected: 400 Bad Request

6. **Invalid Role**
   - Role: "invalidrole"
   - Expected: 400 Bad Request

7. **Database Connection Failure**
   - Stop SSH tunnel
   - Expected: 500 Internal Server Error

### Register Tests

1. **Valid Registration**
   - All required fields with valid data
   - Expected: 201 Created with user data

2. **Duplicate Username**
   - Username: already exists
   - Expected: 400 Bad Request

3. **Duplicate Email**
   - Email: already registered
   - Expected: 400 Bad Request

4. **Invalid Email Format**
   - Email: "notanemail"
   - Expected: 400 Bad Request

5. **Password Too Short**
   - Password: "12345" (less than 6 characters)
   - Expected: 400 Bad Request

6. **Missing Required Fields**
   - Send incomplete JSON
   - Expected: 400 Bad Request

---

## Troubleshooting

### Issue: "Connection refused" or "Network error"
- ✅ Check if server is running
- ✅ Verify URL is correct: `http://localhost:8080/cafedbapp/api/auth/login`
- ✅ Check if port 8080 is correct (may be different in your setup)

### Issue: "CORS error" in browser
- ✅ Make sure you're testing from the same origin
- ✅ Or add CORS headers to servlet response

### Issue: "Database connection failed"
- ✅ Verify SSH tunnel is active
- ✅ Run `TestConnection.java` to verify database access
- ✅ Check server logs for detailed error messages

### Issue: "404 Not Found"
- ✅ Verify servlet URL mapping: `/api/auth/login`
- ✅ Check if application context path is correct
- ✅ Ensure servlet is deployed correctly

### Issue: "500 Internal Server Error"
- ✅ Check server console/logs for detailed error
- ✅ Verify database connection
- ✅ Check if all required tables exist

---

## Viewing Server Logs

### In NetBeans:
1. Go to **Window** → **Output**
2. Select **Server** tab
3. Look for error messages and debug output

### Check Console Output:
- Login attempts are logged with: `System.out.println()`
- Errors are logged with: `System.err.println()`
- Look for messages like:
  - "Login attempt - Username: ..."
  - "Database connection failed: ..."
  - "Login error: ..."

---

## Quick Test Checklist

Before testing, ensure:
- [ ] Server is running (Tomcat/GlassFish)
- [ ] SSH tunnel is active (for database)
- [ ] Database connection works (run TestConnection.java)
- [ ] Application is deployed correctly
- [ ] Browser console is open (F12) for debugging

---

## Advanced: Automated Testing Script

Create a file `test-servlets.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080/cafedbapp/api/auth"

echo "Testing Register..."
curl -X POST $BASE_URL/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123","name":"Test User","email":"test@example.com","phone_num":"1234567890"}' \
  -w "\nStatus: %{http_code}\n\n"

echo "Testing Login..."
curl -X POST $BASE_URL/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123","role":"customer"}' \
  -w "\nStatus: %{http_code}\n\n"
```

Make it executable: `chmod +x test-servlets.sh`
Run: `./test-servlets.sh`

