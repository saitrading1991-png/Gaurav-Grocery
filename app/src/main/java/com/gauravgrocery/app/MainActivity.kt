
package com.gauravgrocery.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

private val Green = Color(0xFF247A42)
private val PaleGreen = Color(0xFFF0F7EE)
private val Ink = Color(0xFF193B27)
private val PageBackground = Color(0xFFF8FAF6)

data class Grocery(
    val id: Int,
    val name: String,
    val category: String,
    val unit: String,
    val price: Int,
    val imageUrl: String
)

private val groceries = listOf(
    Grocery(1, "Tomatoes", "Vegetables", "1 kg", 30,
        "https://images.unsplash.com/photo-1546094096-0df4bcaaa337?auto=format&fit=crop&w=400&q=80"),
    Grocery(2, "Potatoes", "Vegetables", "1 kg", 35,
        "https://images.unsplash.com/photo-1518977676601-b53f82aba655?auto=format&fit=crop&w=400&q=80"),
    Grocery(3, "Onions", "Vegetables", "1 kg", 40,
        "https://images.unsplash.com/photo-1508747703725-719777637510?auto=format&fit=crop&w=400&q=80"),
    Grocery(4, "Bananas", "Fruits", "1 dozen", 50,
        "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?auto=format&fit=crop&w=400&q=80"),
    Grocery(5, "Apples", "Fruits", "1 kg", 140,
        "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?auto=format&fit=crop&w=400&q=80"),
    Grocery(6, "Oranges", "Fruits", "1 kg", 80,
        "https://images.unsplash.com/photo-1547514701-42782101795e?auto=format&fit=crop&w=400&q=80"),
    Grocery(7, "Milk", "Dairy", "1 litre", 60,
        "https://images.unsplash.com/photo-1563636619-e9143da7973b?auto=format&fit=crop&w=400&q=80"),
    Grocery(8, "Curd", "Dairy", "400 g", 35,
        "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=400&q=80"),
    Grocery(9, "Rice", "Staples", "1 kg", 65,
        "https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=400&q=80"),
    Grocery(10, "Wheat Flour", "Staples", "5 kg", 240,
        "https://images.unsplash.com/photo-1627485937980-221c88ac04f9?auto=format&fit=crop&w=400&q=80"),
    Grocery(11, "Eggs", "Dairy", "6 pieces", 55,
        "https://images.unsplash.com/photo-1518569656558-1f25e69d93d7?auto=format&fit=crop&w=400&q=80"),
    Grocery(12, "Cooking Oil", "Staples", "1 litre", 145,
        "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=400&q=80")
)

// Firebase references

private val auth: FirebaseAuth by lazy {
    FirebaseAuth.getInstance()
}

private val db: FirebaseFirestore by lazy {
    FirebaseFirestore.getInstance()
}

private fun saveGroceryOrder(
    customerName: String,
    phone: String,
    address: String,
    items: List<Map<String, Any>>,
    totalPrice: Long,
    onResult: (Boolean, String) -> Unit
) {
    fun saveWithUid(uid: String) {
        val order = hashMapOf<String, Any>(
            "ownerUid" to uid,
            "customerName" to customerName.trim(),
            "phone" to phone.trim(),
            "address" to address.trim(),
            "items" to items,
            "totalPrice" to totalPrice,
            "status" to "Pending",
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("orders")
            .add(order)
            .addOnSuccessListener { document ->
                onResult(
                    true,
                    "Order saved successfully! ID: ${document.id}"
                )
            }
            .addOnFailureListener { error ->
                onResult(
                    false,
                    error.localizedMessage ?: "Could not save order"
                )
            }
    }

    val currentUser = auth.currentUser

    if (currentUser != null) {
        saveWithUid(currentUser.uid)
    } else {
        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid

                if (uid == null) {
                    onResult(false, "Firebase user ID not found")
                } else {
                    saveWithUid(uid)
                }
            }
            .addOnFailureListener { error ->
                onResult(
                    false,
                    error.localizedMessage ?: "Anonymous sign-in failed"
                )
            }
    }
}

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

@Composable
fun GroceryApp() {
    val context = LocalContext.current

    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showCart by remember { mutableStateOf(false) }
    var showCheckout by remember { mutableStateOf(false) }
    var isSavingOrder by remember { mutableStateOf(false) }

    val cart = remember { mutableStateMapOf<Int, Int>() }

    val categories = listOf(
        "All", "Vegetables", "Fruits", "Dairy", "Staples"
    )

    val filteredGroceries = groceries.filter { item ->
        item.name.contains(searchText, ignoreCase = true) &&
            (selectedCategory == "All" ||
                item.category == selectedCategory)
    }

    val cartItems = groceries.filter {
        cart.getOrDefault(it.id, 0) > 0
    }

    val totalItems = cart.values.sum()

    val totalPrice = cartItems.sumOf {
        it.price * cart.getOrDefault(it.id, 0)
    }

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Green)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gaurav Grocery",
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Fresh groceries, delivered with care",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = {
                        if (showCheckout) {
                            showCheckout = false
                            showCart = true
                        } else {
                            showCart = !showCart
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Shopping cart",
                        tint = Color.White
                    )
                }

                Text(
                    text = "$totalItems",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        bottomBar = {
            if (totalItems > 0 && !showCart && !showCheckout) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("$totalItems items", color = Ink)
                        Text(
                            text = "₹$totalPrice",
                            color = Green,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { showCart = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green
                        )
                    ) {
                        Text("View Cart")
                    }
                }
            }
        }
    ) { padding ->
        when {
            showCheckout -> {
                CheckoutScreen(
                    totalPrice = totalPrice,
                    onBack = {
                        showCheckout = false
                    },
                    isSaving = isSavingOrder,
                    onPlaceOrder = { customerName, phone, address ->
                        if (isSavingOrder) {
                            return@CheckoutScreen
                        }

                        isSavingOrder = true

                        val orderItems = cartItems.map { item ->
                            mapOf<String, Any>(
                                "productId" to item.id,
                                "name" to item.name,
                                "unit" to item.unit,
                                "price" to item.price,
                                "quantity" to cart.getOrDefault(item.id, 0)
                            )
                        }

                        saveGroceryOrder(
                            customerName = customerName,
                            phone = phone,
                            address = address,
                            items = orderItems,
                            totalPrice = totalPrice.toLong()
                        ) { success, message ->
                            isSavingOrder = false

                            Toast.makeText(
                                context,
                                message,
                                Toast.LENGTH_LONG
                            ).show()

                            if (success) {
                                cart.clear()
                                showCart = false
                                showCheckout = false
                            }
                        }
                    },
                    modifier = Modifier.padding(padding)
                )
            }

            showCart -> {
                CartScreen(
                    cartItems = cartItems,
                    cart = cart,
                    totalPrice = totalPrice,
                    onBack = { showCart = false },
                    onQuantityChange = { id, quantity ->
                        if (quantity <= 0) {
                            cart.remove(id)
                        } else {
                            cart[id] = quantity
                        }
                    },
                    onCheckout = {
                        if (cartItems.isNotEmpty()) {
                            showCheckout = true
                            showCart = false
                        }
                    },
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                ProductScreen(
                    searchText = searchText,
                    onSearchChange = { searchText = it },
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategoryChange = { selectedCategory = it },
                    products = filteredGroceries,
                    cart = cart,
                    onAdd = { id ->
                        cart[id] = cart.getOrDefault(id, 0) + 1
                    },
                    onRemove = { id ->
                        val quantity = cart.getOrDefault(id, 0)
                        if (quantity <= 1) {
                            cart.remove(id)
                        } else {
                            cart[id] = quantity - 1
                        }
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
fun ProductScreen(
    searchText: String,
    onSearchChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    products: List<Grocery>,
    cart: Map<Int, Int>,
    onAdd: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            placeholder = { Text("Search groceries") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(12.dp))

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PaleGreen,
                        selectedLabelColor = Green
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Fresh groceries",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (products.isEmpty()) {
            Text(
                text = "No products found.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        quantity = cart.getOrDefault(product.id, 0),
                        onAdd = { onAdd(product.id) },
                        onRemove = { onRemove(product.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Grocery,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .size(96.dp)
                    .background(PaleGreen, RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Text(
                    text = product.unit,
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "₹${product.price}",
                    fontSize = 18.sp,
                    color = Green,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                if (quantity == 0) {
                    Button(
                        onClick = onAdd,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green
                        ),
                        contentPadding = PaddingValues(
                            horizontal = 18.dp,
                            vertical = 6.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add"
                        )
                        Text("Add")
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Remove one"
                            )
                        }

                        Text(
                            text = "$quantity",
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = onAdd,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add one"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartScreen(
    cartItems: List<Grocery>,
    cart: Map<Int, Int>,
    totalPrice: Int,
    onBack: () -> Unit,
    onQuantityChange: (Int, Int) -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = "Your Cart",
                fontSize = 23.sp,
                fontWeight = FontWeigh
