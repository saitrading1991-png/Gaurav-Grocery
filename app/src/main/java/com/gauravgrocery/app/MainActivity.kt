package com.gauravgrocery.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore

data class GroceryProduct(
    val id: String,
    val name: String,
    val category: String,
    val price: Double,
    val unit: String,
    val emoji: String,
    val inStock: Boolean = true
)

data class CartItem(
    val product: GroceryProduct,
    val quantity: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                GroceryApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryApp() {
    val context = LocalContext.current
    val cart = remember { mutableStateListOf<CartItem>() }

    var groceryProducts by remember {
        mutableStateOf<List<GroceryProduct>>(emptyList())
    }

    var productsLoading by remember {
        mutableStateOf(true)
    }

    var productsError by remember {
        mutableStateOf<String?>(null)
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var selectedCategory by remember {
        mutableStateOf("All")
    }

    var showCart by remember {
        mutableStateOf(false)
    }

    var showCheckout by remember {
        mutableStateOf(false)
    }

    // Live products from Firebase Firestore
    DisposableEffect(Unit) {
        val registration: ListenerRegistration =
            Firebase.firestore.collection("products")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        productsError = error.localizedMessage
                            ?: "Products load nahi ho paaye"
                        productsLoading = false
                        return@addSnapshotListener
                    }

                    groceryProducts = snapshot?.documents
                        ?.mapNotNull { document ->
                            val active =
                                document.getBoolean("active") ?: true

                            val inStock =
                                document.getBoolean("inStock") ?: true

                            if (!active) {
                                null
                            } else {
                                val priceValue =
                                    document.get("price") as? Number

                                GroceryProduct(
                                    id = document.id,
                                    name = document.getString("name")
                                        ?: "",
                                    category = document.getString("category")
                                        ?: "Other",
                                    price = priceValue?.toDouble() ?: 0.0,
                                    unit = document.getString("unit")
                                        ?: "1 piece",
                                    emoji = document.getString("emoji")
                                        ?: "🛒",
                                    inStock = inStock
                                )
                            }
                        } ?: emptyList()

                    productsError = null
                    productsLoading = false
                }

        onDispose {
            registration.remove()
        }
    }

    // Keep cart prices and stock in sync with current Firebase products.
    LaunchedEffect(groceryProducts) {
        for (index in cart.indices.reversed()) {
            val latest = groceryProducts.find {
                it.id == cart[index].product.id
            }

            if (latest == null || !latest.inStock) {
                cart.removeAt(index)
            } else {
                cart[index] = cart[index].copy(product = latest)
            }
        }
    }

    val categories = listOf("All") +
        groceryProducts
            .map { it.category }
            .filter { it.isNotBlank() && it != "All" }
            .distinct()
            .sorted()

    // If a category was removed in Admin, return to All.
    LaunchedEffect(categories) {
        if (selectedCategory !in categories) {
            selectedCategory = "All"
        }
    }

    val filteredProducts = groceryProducts.filter { product ->
        product.name.contains(searchText, ignoreCase = true) &&
            (
                selectedCategory == "All" ||
                    product.category == selectedCategory
            )
    }

    val totalItems = cart.sumOf { it.quantity }

    val totalPrice = cart.sumOf {
        it.product.price * it.quantity
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Gaurav Grocery",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Fresh groceries at your doorstep",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            showCart = true
                            showCheckout = false
                        }
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Cart"
                        )
                        Text(" $totalItems")
                    }
                }
            )
        }
    ) { innerPadding ->

        when {
            showCheckout -> {
                CheckoutScreen(
                    totalPrice = totalPrice,
                    onBack = {
                        showCheckout = false
                    },
                    onPlaceOrder = { customerName, phone, address ->

                        saveGroceryOrder(
                            customerName = customerName,
                            phone = phone,
                            address = address,
                            cart = cart.toList(),
                            totalPrice = totalPrice,
                            onSuccess = {
                                cart.clear()
                                showCheckout = false
                                showCart = false

                                Toast.makeText(
                                    context,
                                    "Order placed successfully!",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onError = { message ->
                                Toast.makeText(
                                    context,
                                    message,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                )
            }

            showCart -> {
                CartScreen(
                    cart = cart,
                    totalPrice = totalPrice,
                    onBack = {
                        showCart = false
                    },
                    onQuantityChange = { product, change ->

                        val index = cart.indexOfFirst {
                            it.product.id == product.id
                        }

                        if (index >= 0) {
                            val latestProduct = groceryProducts.find {
                                it.id == product.id
                            }

                            if (latestProduct == null ||
                                !latestProduct.inStock
                            ) {
                                cart.removeAt(index)
                            } else {
                                val newQuantity =
                                    cart[index].quantity + change

                                if (newQuantity <= 0) {
                                    cart.removeAt(index)
                                } else {
                                    cart[index] = CartItem(
                                        product = latestProduct,
                                        quantity = newQuantity
                                    )
                                }
                            }
                        }
                    },
                    onCheckout = {
                        if (cart.isNotEmpty()) {
                            showCheckout = true
                        }
                    }
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when {
                        productsLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment =
                                        Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(Modifier.height(12.dp))
                                    Text("Loading products...")
                                }
                            }
                        }

                        productsError != null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally,
                                verticalArrangement =
                                    Arrangement.Center
                            ) {
                                Text(
                                    "Products load nahi hue",
                                    style =
                                        MaterialTheme.typography.titleLarge
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(productsError ?: "Unknown error")
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Internet aur Firebase Firestore Rules check karein."
                                )
                            }
                        }

                        else -> {
                            ProductScreen(
                                products = filteredProducts,
                                categories = categories,
                                searchText = searchText,
                                selectedCategory = selectedCategory,
                                onSearchChange = {
                                    searchText = it
                                },
                                onCategoryChange = {
                                    selectedCategory = it
                                },
                                onAddToCart = { product ->

                                    if (!product.inStock) {
                                        Toast.makeText(
                                            context,
                                            "Product is out of stock",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        val index = cart.indexOfFirst {
                                            it.product.id == product.id
                                        }

                                        if (index >= 0) {
                                            cart[index] = cart[index].copy(
                                                quantity =
                                                    cart[index].quantity + 1,
                                                product = product
                                            )
                                        } else {
                                            cart.add(
                                                CartItem(product, 1)
                                            )
                                        }

                                        Toast.makeText(
                                            context,
                                            "${product.name} added to cart",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductScreen(
    products: List<GroceryProduct>,
    categories: List<String>,
    searchText: String,
    selectedCategory: String,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAddToCart: (GroceryProduct) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChange,
            label = {
                Text("Search groceries")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = {
                        onCategoryChange(category)
                    },
                    label = {
                        Text(category)
                    }
                )
            }
        }

        Text(
            "Fresh groceries",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (searchText.isNotBlank()) {
                        "No matching products found."
                    } else {
                        "No products available yet."
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = products,
                    key = { it.id }
                ) { product ->
                    ProductCard(
                        product = product,
                        onAdd = {
                            onAddToCart(product)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: GroceryProduct,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                product.emoji,
                style = MaterialTheme.typography.headlineLarge
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    product.name,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    product.category,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    product.unit,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    "₹${formatPrice(product.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (!product.inStock) {
                    Text(
                        "Currently unavailable",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Button(
                onClick = onAdd,
                enabled = product.inStock
            ) {
                Text(
                    if (product.inStock) "Add"
                    else "Out of Stock"
                )
            }
        }
    }
}

private fun formatPrice(price: Double): String {
    return if (price % 1.0 == 0.0) {
        price.toLong().toString()
    } else {
        String.format(
            java.util.Locale.US,
            "%.2f",
            price
        )
    }
}

@Composable
fun CartScreen(
    cart: List<CartItem>,
    totalPrice: Double,
    onBack: () -> Unit,
    onQuantityChange: (GroceryProduct, Int) -> Unit,
    onCheckout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Your Cart",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        if (cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Your cart is empty.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = cart,
                    key = { it.product.id }
                ) { item ->
     
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.product.name,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "₹${formatPrice(item.product.price)} × ${item.quantity}"
                            )
                            Text(
                                "Subtotal: ₹${formatPrice(item.product.price * item.quantity)}"
                            )
                        }

                        IconButton(
                            onClick = {
                                onQuantityChange(item.product, -1)
                            }
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Decrease quantity"
                            )
                        }

                        Text(item.quantity.toString())

                        IconButton(
                            onClick = {
                                onQuantityChange(item.product, 1)
                            }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Increase quantity"
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Total: ₹${formatPrice(totalPrice)}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back to Shop")
            }

            Button(
                onClick = onCheckout,
                enabled = cart.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Checkout")
            }
        }
    }
}

@Composable
fun CheckoutScreen(
    totalPrice: Double,
    onBack: () -> Unit,
    onPlaceOrder: (String, String, String) -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Checkout",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("Customer name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = {
                phone = it.filter { character -> character.isDigit() }
                    .take(10)
            },
            label = { Text("Mobile number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Delivery address") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            "Order total: ₹${formatPrice(totalPrice)}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Button(
            onClick = {
                if (customerName.isBlank() ||
                    phone.length != 10 ||
                    address.isBlank()
                ) {
                    Toast.makeText(
                        LocalContext.current,
                        "Name, valid 10-digit mobile number and address enter karein.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    onPlaceOrder(
                        customerName.trim(),
                        phone,
                        address.trim()
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Place Order")
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Cart")
        }
    }
}

private fun saveGroceryOrder(
    customerName: String,
    phone: String,
    address: String,
    cart: List<CartItem>,
    totalPrice: Double,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val orderItems = cart.map { item ->
        mapOf(
            "productId" to item.product.id,
            "name" to item.product.name,
            "quantity" to item.quantity,
            "unitPrice" to item.product.price
        )
    }

    val order = hashMapOf(
        "customerName" to customerName,
        "phone" to phone,
        "address" to address,
        "items" to orderItems,
        "totalPrice" to totalPrice,
        "status" to "Pending",
        "createdAt" to System.currentTimeMillis()
    )

    Firebase.firestore
        .collection("orders")
        .add(order)
        .addOnSuccessListener {
            onSuccess()
        }
        .addOnFailureListener { error ->
            onError(
                error.localizedMessage ?: "Order save nahi ho paaya."
            )
        }
}

