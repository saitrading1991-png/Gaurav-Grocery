const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

initializeApp();

const db = getFirestore();

exports.reduceStockAfterOrder = onDocumentCreated(
  {
    document: "orders/{orderId}",
    region: "asia-south1"
  },
  async (event) => {
    const orderRef = event.data.ref;

    await db.runTransaction(async (transaction) => {
      const orderSnap = await transaction.get(orderRef);

      if (!orderSnap.exists) return;

      const order = orderSnap.data();

      // Prevent duplicate stock deductions.
      if (order.stockProcessed === true) return;

      if (!Array.isArray(order.items) || order.items.length === 0) {
        throw new Error("Order has no items");
      }

      // Combine duplicate product IDs.
      const quantities = new Map();

      for (const item of order.items) {
        const productId = item.productId;
        const quantity = Number(item.quantity);

        if (
          typeof productId !== "string" ||
          !productId ||
          !Number.isInteger(quantity) ||
          quantity <= 0
        ) {
          throw new Error("Invalid product ID or quantity");
        }

        quantities.set(
          productId,
          (quantities.get(productId) || 0) + quantity
        );
      }

      // Read all products before making any writes.
      const products = [];

      for (const [productId, quantity] of quantities) {
        const ref = db.collection("products").doc(productId);
        const snap = await transaction.get(ref);

        if (!snap.exists) {
          throw new Error(`Product not found: ${productId}`);
        }

        const data = snap.data();
        const stock = data.stockQuantity;

        if (!Number.isInteger(stock) || stock < 0) {
          throw new Error(
            `Invalid stockQuantity for product ${productId}`
          );
        }

        products.push({
          ref,
          stock,
          quantity
        });
      }

      const unavailable = products.some(
        (p) => p.stock < p.quantity
      );

      if (unavailable) {
        transaction.update(orderRef, {
          status: "Stock Issue",
          stockProcessed: true,
          stockMessage: "Insufficient stock; review this order"
        });
        return;
      }

      for (const product of products) {
        const newStock = product.stock - product.quantity;

        transaction.update(product.ref, {
          stockQuantity: newStock,
          inStock: newStock > 0
        });
      }

      transaction.update(orderRef, {
        stockProcessed: true
      });
    });
  }
);
