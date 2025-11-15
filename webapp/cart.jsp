<%-- 
    Document   : cart
    Created on : Nov 15, 2025, 5:26:26 PM
    Author     : Matthew Javier
--%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Your Cart - The Waiting Room Café</title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>

  <jsp:include page="includes/header.jsp" />

  <main>
    <h2>Your Cart</h2>
    <div id="cartItems"></div>
    <button id="placeOrder">Place Order</button>
  </main>

  <script src="js/cart.js"></script>

</body>
</html>





