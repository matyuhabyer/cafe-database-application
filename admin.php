<?php session_start(); ?>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Admin Panel - The Waiting Room Café</title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>

  <?php include('includes/header.php'); ?>

  <main>
    <h2>Admin Panel</h2>
    <form id="addProductForm">
      <input type="text" id="name" placeholder="Product Name" required>
      <input type="number" id="price" placeholder="Price (PHP)" required>
      <button type="submit">Add Product</button>
    </form>

    <h3>Product List</h3>
    <ul id="adminProductList"></ul>
  </main>

  <script src="js/admin.js"></script>
</body>
</html>
