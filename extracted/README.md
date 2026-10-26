# Gaurav Grocery — Android delivery app

Kotlin + Jetpack Compose starter app.

## Included
- Branded Gaurav Grocery home screen
- Product search and category filters
- Sample grocery products/prices
- Cart quantities and totals
- Checkout form with name, mobile number, and delivery address
- Online payment (demo option) and cash-on-delivery (demo option)

## Build a debug APK
1. Install Android Studio and JDK 17.
2. Open this folder in Android Studio.
3. Let Gradle sync complete.
4. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Important production steps
The online payment choice here is a UI placeholder, not a real payment integration. For a real delivery business:
1. Add a backend API and database for products, prices, stock, customers, and orders.
2. Create orders on the server; calculate totals on the server (never trust app totals).
3. Integrate a payment provider such as Razorpay or Cashfree through its official Android SDK and backend order/signature verification. Never put secret keys in the Android app.
4. Add admin order management, delivery zones/charges, inventory management, order status notifications, and customer support.
5. Test payment success, failure, cancellation, duplicate callbacks, refunds, privacy, and security before release.

Product prices are examples. This starter does not send customer details to a server or charge a payment method.
## Build from a phone using GitHub Actions (no PC needed)
This project includes `.github/workflows/build-apk.yml`.
1. Upload/extract the project files into a GitHub repository on the `main` branch.
2. Open the repository's **Actions** tab.
3. Select **Build Gaurav Grocery APK** and tap **Run workflow** if needed.
4. Open the completed run and download the `Gaurav-Grocery-debug-APK` artifact.
5. Extract the downloaded artifact ZIP on your Android phone; it contains `app-debug.apk`.

This creates a debug APK for testing, not a signed production release. Payment UI remains demo-only.
