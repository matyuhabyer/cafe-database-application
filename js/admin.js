const adminProducts = JSON.parse(localStorage.getItem("adminProducts") || "[]");
const form = document.getElementById("addProductForm");
const list = document.getElementById("adminProductList");

function render() {
  list.innerHTML = "";
  adminProducts.forEach((p, i) => {
    list.innerHTML += `<li>${p.name} - ₱${p.price} 
      <button onclick="remove(${i})">Delete</button></li>`;
  });
}

form.addEventListener("submit", e => {
  e.preventDefault();
  const name = document.getElementById("name").value;
  const price = parseFloat(document.getElementById("price").value);
  adminProducts.push({ name, price });
  localStorage.setItem("adminProducts", JSON.stringify(adminProducts));
  render();
  form.reset();
});

function remove(i) {
  adminProducts.splice(i, 1);
  localStorage.setItem("adminProducts", JSON.stringify(adminProducts));
  render();
}

render();