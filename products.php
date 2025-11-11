<?php session_start(); ?>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Browse Menu - The Waiting Room Café</title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>

  <?php include('includes/header.php'); ?>

  <section class="menu-header">
    <h2>Our Signature Menu</h2>
    <div class="currency">
      <label>Currency:</label>
      <select id="currency">
        <option value="PHP">PHP</option>
        <option value="USD">USD</option>
        <option value="KRW">KRW</option>
      </select>
    </div>
  </section>

  <section class="product-list" id="productList"></section>

  <script src="js/products.js"></script>
</body>
</html>
