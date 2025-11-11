const products = [
  { id: 1, name: "Cappuccino", price: 120 },
  { id: 2, name: "Iced Latte", price: 140 },
  { id: 3, name: "Espresso", price: 100 },
  { id: 4, name: "Pasta Carbonara", price: 180 },
  { id: 5, name: "Club Sandwich", price: 160 },
  { id: 6, name: "Chocolate Cake", price: 150 }
];

const rates = { PHP: 1, USD: 0.017, KRW: 24 };
const list = document.getElementById("productList");
const currencySelect = document.getElementById("currency");

function renderProducts() {
  const currency = currencySelect.value;
  list.innerHTML = "";
  products.forEach(p => {
    const price = (p.price * rates[currency]).toFixed(2);
    list.innerHTML += `
      <div class="product">
        <h3>${p.name}</h3>
        <p>${price} ${currency}</p>
        <button onclick="addToCart(${p.id})">Add to Cart</button>
      </div>
    `;
  });
}

function addToCart(id) {
  const cart = JSON.parse(localStorage.getItem("cart") || "[]");
  const item = products.find(p => p.id === id);
  cart.push(item);
  localStorage.setItem("cart", JSON.stringify(cart));
  alert(`${item.name} added to cart!`);
}

currencySelect.addEventListener("change", renderProducts);
renderProducts();
