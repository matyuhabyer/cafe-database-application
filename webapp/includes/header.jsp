<%-- 
    Document   : header
    Created on : Nov 15, 2025, 5:27:29?PM
    Author     : Matthew Javier
--%>

<header class="main-header">
  <div class="logo">
    <img src="https://cdn-icons-png.flaticon.com/512/3050/3050525.png" alt="Cafe Logo">
    <h1>The Waiting Room Café</h1>
  </div>

  <nav class="navbar">
    <a href="products.jsp">Home</a>
    <a href="products.jsp">Menu</a>
    <a href="cart.jsp">Cart</a>
    <a href="orders.jsp">Orders</a>
    <a href="admin.jsp" id="adminLink" style="display:none;">Admin</a>
  </nav>

  <div class="user-section">
    <span id="userRole">Guest</span>
    <button id="logoutBtn" class="logout-btn" style="display:none;">Logout</button>
  </div>
</header>

<script src="js/header.js"></script>
