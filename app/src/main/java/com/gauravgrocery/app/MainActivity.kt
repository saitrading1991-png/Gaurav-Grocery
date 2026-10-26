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
import com.google.firebase.firestore.firestore

data class GroceryProduct(
    val id: Int,
    val name: String,
    val category: String,
    val price: Double,
    val unit: String,
    val emoji: String
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

private val groceryProducts = listOf(
    GroceryProduct(1, "Fresh Apples", "Fruits", 120.0, "1 kg", "🍎"),
    GroceryProduct(2, "Bananas", "Fruits", 50.0, "1 dozen", "🍌"),
    GroceryProduct(3, "Oranges", "Fruits", 90.0, "1 kg", "🍊"),
    GroceryProduct(4, "Potatoes", "Vegetables", 35.0, "1 kg", "🥔"),
    GroceryProduct(5, "Tomatoes", "Vegetables", 40.0, "1 kg", "🍅"),
    GroceryProduct(6, "Onions", "Vegetables", 45.0, "1 kg", "🧅"),
    GroceryProduct(7, "Fresh Milk", "Dairy", 32.0, "500 ml", "🥛"),
    GroceryProduct(8, "Butter", "Dairy", 60.0, "100 g", "🧈"),
    GroceryProduct(9, "Bread", "Bakery", 40.0, "1 pack", "🍞"),
    GroceryProduct(10, "Eggs", "Daily Needs", 75.0, "6 pieces", "🥚"),
    GroceryProduct(11, "Rice", "Staples", 65.0, "1 kg", "🍚"),
    GroceryProduct(12, "Cooking Oil", "Staples", 150.0, "1 litre", "🫗")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryApp() {
    val context = LocalContext.current
    val cart = remember { mutableStateListOf<CartItem>() }

    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showCart by remember { mutableStateOf(false) }
    var showCheckout by remember { mutableStateOf(false) }

    val categories = listOf(
        "All", "Fruits", "Vegetables", "Dairy",
        "Bakery", "Daily Needs", "Staples"
    )

    val filteredProducts = groceryProducts.filter { product ->
        product.name.contains(searchText, ignoreCase = true) &&
            (selectedCategory == "All" ||
                product.category == selectedCategory)
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
                    onBack = { showCheckout = false },
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
                    onBack = { showCart = false },
                    onQuantityChange = { product, change ->
                        val index = cart.indexOfFirst {
                            it.product.id == product.id
                        }

                        if (index >= 0) {
                            val newQuantity =
                                cart[index].quantity + change

                            if (newQuantity <= 0) {
                                cart.removeAt(index)
                            } else {
                                cart[index] = cart[index].copy(
                                    quantity = newQuantity
                                )
                            }
                        }
                    },
                    onCheckout = {
                        showCheckout = true
                    }
                )
            }

            else -> {
                ProductScreen(
                    modifier = Modifier.padding(innerPadding),
                    products = filteredProducts,
                    categories = categories,
                    searchText = searchText,
                    selectedCategory = selectedCategory,
                    onSearchChange = { searchText = it },
                    onCategoryChange = { selectedCategory = it },
                    onAddToCart = { product ->
                        val index = cart.indexOfFirst {
                            it.product.id == product.id
                        }

                        if (index >= 0) {
                            cart[index] = cart[index].copy(
                                quantity = cart[index].quantity + 1
                            )
                        } else {
                            cart.add(CartItem(product, 1))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ProductScreen(
    modifier: Modifier = Modifier,
    products: List<GroceryProduct>,
    categories: List<String>,
    searchText: String,
    selectedCategory: String,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAddToCart: (GroceryProduct) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChange,
            label = { Text("Search groceries") },
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
                    onClick = { onCategoryChange(category) },
                    label = { Text(category) }
                )
            }
        }

        Text(
            "Fresh groceries",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(products, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    onAdd = { onAddToCart(product) }
                )
            }

            if (products.isEmpty()) {
                item { Text("No products found.") }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: GroceryProduct,
    onAdd: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
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
                Text(product.name, fontWeight = FontWeight.Bold)
                Text(
                    product.unit,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "₹${product.price.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(onClick = onAdd) {
                Text("Add")
            }
        }
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
            Text("Your cart is empty.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cart, key = { it.product.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.product.emoji)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    item.product.name,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "₹${(item.product.price * item.quantity).toInt()}"
                                )
                            }

                            IconButton(
                                onClick = {
                                    onQuantityChange(item.product, -1)
                                }
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "Decrease"
                                )
                            }

                            Text("${item.quantity}")

                            IconButton(
                                onClick = {
                                    onQuantityChange(item.product, 1)
                                }
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Increase"
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(
                "Total: ₹${totalPrice.toInt()}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Button(
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Proceed to Checkout")
            }
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Continue Shopping")
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
            .padding(16.dp)
    ) {
        Text(
            "Checkout",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))
        Text("Order total: ₹${totalPrice.toInt()}")

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("Customer name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Delivery address") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (
                    customerName.isBlank() ||
                    address.isBlank() ||
                    phone.filter { it.isDigit() }.length < 10
                ) {
                    return@Button
                }

                onPlaceOrder(
                    customerName.trim(),
                    phone.trim(),
                    address.trim()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Place Order")
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
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
    val auth = Firebase.auth
    val db = Firebase.firestore

    if (cart.isEmpty()) {
        onError("Your cart is empty.")
        return
    }

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

    fun writeOrder() {
        db.collection("orders")
            .add(order)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.localizedMessage ?: "Could not save order"
                )
            }
    }

    if (auth.currentUser != null) {
        writeOrder()
    } else {
        auth.signInAnonymously()
            .addOnSuccessListener {
                writeOrder()
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.localizedMessage ?: "Authentication failed"
                )
            }
    }
}

