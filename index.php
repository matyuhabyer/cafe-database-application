<?php session_start(); ?>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>The Waiting Room Café | Login</title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>

  <?php include('includes/header.php'); ?>

  <div class="container login-container">
    <h2>Login to Continue</h2>
    <form method="post">
      <input type="text" name="username" placeholder="Username" required>
      <input type="password" name="password" placeholder="Password" required>
      <select name="role" required>
        <option value="customer">Customer</option>
        <option value="staff">Staff</option>
        <option value="admin">Admin</option>
      </select>
      <button type="submit" name="login">Login</button>
    </form>
  </div>

  <?php
  if (isset($_POST['login'])) {
    $_SESSION['role'] = $_POST['role'];
    echo "<script>
      localStorage.setItem('role', '{$_POST['role']}');
      window.location.href = '{$_POST['role']}.php';
    </script>";
  }
  ?>

</body>
</html>
