<%-- 
    Document   : register
    Created on : Nov 15, 2025, 5:25:21 PM
    Author     : Matthew Javier
--%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>The Waiting Room Café | Register</title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>

  <jsp:include page="includes/header.jsp" />

  <div class="container login-container">
    <h2>Create an Account</h2>
    <form method="post" id="registerForm">
      <input type="text" name="username" id="username" placeholder="Username" required>
      <input type="password" name="password" id="password" placeholder="Password (min 6 characters)" required>
      <input type="text" name="name" id="name" placeholder="Full Name" required>
      <input type="email" name="email" id="email" placeholder="Email" required>
      <input type="tel" name="phone_num" id="phone_num" placeholder="Phone Number" required>
      <button type="submit" name="register">Register</button>
    </form>
    <p><a href="index.jsp">Already have an account? Login here</a></p>
  </div>

  <script>
    // Get the context path dynamically - same method as test-servlets.html
    const BASE_URL = window.location.origin + window.location.pathname.replace(/\/[^/]*$/, '');
    const API_BASE = BASE_URL + '/api/auth';
    
    console.log('Base URL:', BASE_URL);
    console.log('API Base:', API_BASE);
    
    document.getElementById('registerForm').addEventListener('submit', async function(e) {
      e.preventDefault();
      const username = document.getElementById('username').value.trim();
      const password = document.getElementById('password').value;
      const name = document.getElementById('name').value.trim();
      const email = document.getElementById('email').value.trim();
      const phoneNum = document.getElementById('phone_num').value.trim();
      
      console.log('Register attempt:', { username, name, email, phoneNum, passwordLength: password.length });
      
      // Show loading state
      const submitButton = e.target.querySelector('button[type="submit"]');
      const originalText = submitButton.textContent;
      submitButton.disabled = true;
      submitButton.textContent = 'Registering...';
      
      try {
        const response = await fetch(`${API_BASE}/register`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'include',
          body: JSON.stringify({ username, password, name, email, phone_num: phoneNum })
        });
        
        console.log('Response status:', response.status);
        console.log('Response headers:', response.headers);
        
        if (!response.ok) {
          console.error('HTTP Error:', response.status, response.statusText);
        }
        
        const result = await response.json();
        console.log('Register response:', result);
        
        if (result.success) {
          alert('Registration successful! Redirecting to login...');
          window.location.href = 'index.jsp';
        } else {
          alert('Registration failed: ' + (result.message || result.error || 'Unknown error'));
          console.error('Registration failed:', result);
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

