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
    // Get the context path dynamically - same method as test-servlets.html
    const BASE_URL = window.location.origin + window.location.pathname.replace(/\/[^/]*$/, '');
    const API_BASE = BASE_URL + '/api/auth';
    
    console.log('Base URL:', BASE_URL);
    console.log('API Base:', API_BASE);
    
    document.getElementById('loginForm').addEventListener('submit', async function(e) {
      e.preventDefault();
      const username = document.getElementById('username').value.trim();
      const password = document.getElementById('password').value; // Don't trim password - spaces might be intentional
      const role = document.getElementById('role').value;
      
      console.log('Login attempt:', { username, role, passwordLength: password.length });
      
      // Show loading state
      const submitButton = e.target.querySelector('button[type="submit"]');
      const originalText = submitButton.textContent;
      submitButton.disabled = true;
      submitButton.textContent = 'Logging in...';
      
      try {
        const response = await fetch(`${API_BASE}/login`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'include',
          body: JSON.stringify({ username, password, role })
        });
        
        console.log('Response status:', response.status);
        console.log('Response headers:', response.headers);
        
        if (!response.ok) {
          console.error('HTTP Error:', response.status, response.statusText);
        }
        
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
          alert('Login failed: ' + (result.message || result.error || 'Unknown error'));
          console.error('Login failed:', result);
        }
      } catch (error) {
        console.error('Error:', error);
        alert('Error connecting to server: ' + error.message + '\n\nCheck browser console (F12) for details.');
      } finally {
        submitButton.disabled = false;
        submitButton.textContent = originalText;
      }
    });
  </script>

</body>
</html>




