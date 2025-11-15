<%-- 
    Document   : admin
    Created on : Nov 15, 2025, 5:26:47 PM
    Author     : Matthew Javier
--%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Admin Panel - The Waiting Room Café</title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>

  <jsp:include page="includes/header.jsp" />

  <main>
    <h2>Admin Panel</h2>
    <div id="adminContent">
      <h3>Branches</h3>
      <div id="branchesList"></div>
      
      <h3>Reports</h3>
      <div id="reportsContent"></div>
    </div>
  </main>

  <script src="js/admin.js"></script>
</body>
</html>





