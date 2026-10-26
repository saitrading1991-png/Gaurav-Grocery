
package com.gauravgrocery.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                Surface(modifier = Modifier.fillMaxSize()) {
                    GroceryApp()
                }
            }
        }
    }
}

private val groceryProducts = listOf(
    GroceryProduct(1, "Fresh Apples", "Fruits", 120.0, "1 kg", "🍎"),
    GroceryProduct(2, "Bananas", "Fruits", 50.0, "1 dozen", "🍌"),
    GroceryProduct(3, "Fresh Oranges", "Fruits", 90.0, "1 kg", "🍊"),
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
        val matchesSearch = product.name.contains(
            searchText,
            ignoreCase = true
        )
        val matchesCategory =
            selectedCategory == "All" ||
                product.category == selectedCategory

        matchesSearch && matchesCategory
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
                            text = "Gaurav Grocery",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Fresh groceries at your doorstep",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = {
                            showCart = true
                            showCheckout = false
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            modifier = Modifier.size(18.dp)
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
                            },
                            onError = { message ->
                                Toast.makeText(
                                    android.app.ApplicationProviderPlaceholder
                                        .application,
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
                            cart.add(
                                CartItem(
                                    product = product,
                                    quantity = 1
                                )
                            )
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
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChange,
            label = { Text("Search groceries") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        androidx.compose.foundation.lazy.LazyRow(
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

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Fresh groceries",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
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
                item {
                    Text(
                        text = "No products found.",
                        modifier = Modifier.padding(16.dp)
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
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = product.emoji,
                style = MaterialTheme.typography.headlineLarge
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = product.unit,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "₹${product.price.toInt()}",
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
            text = "Your Cart",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (cart.isEmpty()) {
            Text("Your cart is empty.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cart, key = { it.product.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.product.emoji,
                                style = MaterialTheme.typography.headlineMedium
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = item.product.name,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "₹${(item.product.price * item.quantity).toInt()}"
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    onQuantityChange(item.product, -1)
                                }
                            ) {
                                androidx.compose.material3.Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "Decrease quantity"
                                )
                            }

                            Text(
                                text = "${item.quantity}",
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            OutlinedButton(
                                onClick = {
                                    onQuantityChange(item.product, 1)
                                }
                            ) {
                                androidx.compose.material3.Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Increase quantity"
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "Total: ₹${totalPrice.toInt()}",
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
            text = "Checkout",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Order total: ₹${totalPrice.toInt()}")

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("Customer name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Delivery address") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (
                    customerName.isBlank() ||
                    phone.isBlank() ||
                    address.isBlank()
                ) {
                    return@Button
                }

                if (phone.filter { it.isDigit() }.length < 10) {
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

    fun saveWithSignedInUser() {
        db.collection("orders")
            .add(order)
  
