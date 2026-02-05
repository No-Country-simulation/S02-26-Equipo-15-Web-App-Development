const express = require("express");
const cors = require("cors");

const app = express();

// CORS: permite que tu React (Vite) le pegue desde localhost:5173
app.use(cors({ origin: ["http://localhost:5173", "http://localhost:5174"] }));
app.use(express.json());

app.post("/checkout/session", (req, res) => {
  console.log("✅ /checkout/session payload:", req.body);

  // Simulamos una "checkout url"
  // Podés cambiarlo por una página local tipo /success si querés
  return res.json({
    url: "https://stripe.com"
  });
});

app.get("/health", (_, res) => res.json({ ok: true }));

const PORT = 8080;
app.listen(PORT, () => {
  console.log(`🚀 Mock API running on http://localhost:${PORT}`);
});
