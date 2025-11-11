<?php session_start(); ?>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Your Cart - The Waiting Room Café</title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>

  <?php include('includes/header.php'); ?>

  <main>
    <h2>Your Cart</h2>
    <div id="cartItems"></div>
    <button id="placeOrder">Place Order</button>
  </main>

  <script src="js/cart.js"></script>

</body>
</html>
