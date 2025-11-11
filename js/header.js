const role = localStorage.getItem("role") || "guest";
const userRoleSpan = document.getElementById("userRole");
const adminLink = document.getElementById("adminLink");
const logoutBtn = document.getElementById("logoutBtn");

userRoleSpan.textContent = role.charAt(0).toUpperCase() + role.slice(1);

if (role === "admin") adminLink.style.display = "inline-block";
if (role !== "guest") logoutBtn.style.display = "inline-block";

logoutBtn.addEventListener("click", () => {
  localStorage.removeItem("role");
  window.location.href = "index.html";
});