<%-- 
    Document   : index
    Created on : Nov 15, 2025, 5:25:21 PM
    Author     : Matthew Javier
--%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>The Waiting Room Café | Login</title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>

  <jsp:include page="includes/header.jsp" />

  <div class="container login-container">
    <h2>Login to Continue</h2>
    <form method="post" id="loginForm">
      <input type="text" name="username" id="username" placeholder="Username" required>
      <input type="password" name="password" id="password" placeholder="Password" required>
      <select name="role" id="role" required>
        <option value="customer">Customer</option>
        <option value="staff">Staff</option>
        <option value="admin">Admin</option>
      </select>
      <button type="submit" name="login">Login</button>
    </form>
    <p><a href="register.jsp">Don't have an account? Register here</a></p>
  </div>

  <script>
    // Get the context path dynamically
    // Extract context path from URL (e.g., /cafedbapp/ or /cafedbapp-1.0/)
    const pathParts = window.location.pathname.split('/').filter(p => p);
    const contextPath = pathParts[0] || '';
    const apiBase = contextPath ? `/${contextPath}/api` : '/api';
    
    document.getElementById('loginForm').addEventListener('submit', async function(e) {
      e.preventDefault();
      const username = document.getElementById('username').value.trim();
      const password = document.getElementById('password').value; // Don't trim password - spaces might be intentional
      const role = document.getElementById('role').value;
      
      console.log('Login attempt:', { username, role, passwordLength: password.length });
      
      try {
        const response = await fetch(`${apiBase}/auth/login`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'include',
          body: JSON.stringify({ username, password, role })
        });
        
        const result = await response.json();
        console.log('Login response:', result);
        
        if (result.success) {
          localStorage.setItem('role', role);
          localStorage.setItem('user', JSON.stringify(result.data));
          if (role === 'customer') {
            window.location.href = 'products.jsp';
          } else {
            window.location.href = 'admin.jsp';
          }
        } else {
          alert('Login failed: ' + result.message);
          console.error('Login failed:', result);
        }
      } catch (error) {
        console.error('Error:', error);
        alert('Error connecting to server: ' + error.message);
      }
    });
  </script>

</body>
</html>




