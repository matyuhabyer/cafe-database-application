<%-- 
    Document   : orders
    Created on : Nov 15, 2025, 5:26:33 PM
    Author     : Matthew Javier
--%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Order History - The Waiting Room Café</title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>

  <jsp:include page="includes/header.jsp" />

  <main>
    <h2>Your Order History</h2>
    <div id="orderHistory"></div>
  </main>

  <script>
    async function loadOrders() {
      try {
        const response = await fetch('api/orders/get_orders');
        const result = await response.json();
        const container = document.getElementById('orderHistory');
        
        if (result.success && result.data && result.data.length > 0) {
          container.innerHTML = result.data.map(order => `
            <div class="order">
              <h4>Order #${order.order_id}</h4>
              <p><strong>Date:</strong> ${order.order_date}</p>
              <p><strong>Total:</strong> ₱${order.total_amount}</p>
              <p><strong>Status:</strong> ${order.status}</p>
              <p><strong>Points Earned:</strong> ${order.earned_points}</p>
              <button onclick="viewOrderDetails(${order.order_id})">View Details</button>
            </div>
          `).join('');
        } else {
          container.innerHTML = '<p>No orders yet.</p>';
        }
      } catch (error) {
        console.error('Error:', error);
        document.getElementById('orderHistory').innerHTML = '<p>Error loading orders.</p>';
      }
    }
    
    function viewOrderDetails(orderId) {
      window.location.href = 'order_details.jsp?order_id=' + orderId;
    }
    
    loadOrders();
  </script>

</body>
</html>





