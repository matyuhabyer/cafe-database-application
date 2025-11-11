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

  <script>
    const cart = JSON.parse(localStorage.getItem("cart") || "[]");
    const cartDiv = document.getElementById("cartItems");
    let total = 0;

    if (cart.length === 0) cartDiv.innerHTML = "<p>Your cart is empty.</p>";
    else {
      cart.forEach(item => {
        cartDiv.innerHTML += `<div>${item.name} - ₱${item.price}</div>`;
        total += item.price;
      });
      cartDiv.innerHTML += `<hr><p><strong>Total:</strong> ₱${total}</p>`;
    }

    document.getElementById("placeOrder").addEventListener("click", () => {
      const orders = JSON.parse(localStorage.getItem("orders") || "[]");
      orders.push({ id: Date.now(), items: cart, total });
      localStorage.setItem("orders", JSON.stringify(orders));
      localStorage.removeItem("cart");
      alert("Order placed successfully!");
      window.location.href = "orders.php";
    });
  </script>

</body>
</html>
