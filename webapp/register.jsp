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
    // Get the context path dynamically
    const contextPath = window.location.pathname.split('/')[1] || '';
    const apiBase = contextPath ? `/${contextPath}/api` : '/api';
    
    document.getElementById('registerForm').addEventListener('submit', async function(e) {
      e.preventDefault();
      const username = document.getElementById('username').value;
      const password = document.getElementById('password').value;
      const name = document.getElementById('name').value;
      const email = document.getElementById('email').value;
      const phoneNum = document.getElementById('phone_num').value;
      
      try {
        const response = await fetch(`${apiBase}/auth/register`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'include',
          body: JSON.stringify({ username, password, name, email, phone_num: phoneNum })
        });
        
        const result = await response.json();
        if (result.success) {
          alert('Registration successful! Redirecting to login...');
          window.location.href = 'index.jsp';
        } else {
          alert('Registration failed: ' + result.message);
        }
      } catch (error) {
        console.error('Error:', error);
        alert('Error connecting to server');
      }
    });
  </script>

</body>
</html>

