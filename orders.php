<?php session_start(); ?>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Order History - The Waiting Room Café</title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>

  <?php include('includes/header.php'); ?>

  <main>
    <h2>Your Order History</h2>
    <div id="orderHistory"></div>
  </main>

  <script>
    const orders = JSON.parse(localStorage.getItem("orders") || "[]");
    const container = document.getElementById("orderHistory");

    if (orders.length === 0) container.innerHTML = "<p>No orders yet.</p>";
    else {
      orders.forEach(o => {
        container.innerHTML += `
          <div class="order">
            <h4>Order #${o.id}</h4>
            <ul>${o.items.map(i => `<li>${i.name}</li>`).join("")}</ul>
            <p><strong>Total:</strong> ₱${o.total}</p>
          </div>
        `;
      });
    }
  </script>

</body>
</html>
