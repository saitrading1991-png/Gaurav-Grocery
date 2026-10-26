
package com.gauravgrocery.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

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
                        if (showCart || showCheckout) {
                            showCart = false
                            showCheckout = false
                        } else {
                            showCart = true
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
                    onBack = { showCheckout = false },
                    onPlaceOrder = {
                        Toast.makeText(
                            context,
                            "Demo order placed successfully!",
                            Toast.LENGTH_LONG
                        ).show()
                        cart.clear()
                        showCart = false
                        showCheckout = false
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
                    onCheckout = {
                        if (cartItems.isNotEmpty()) {
                            showCheckout = true
                        }
                    },
                    onAdd = { id ->
                        cart[id] = cart.getOrDefault(id, 0) + 1
                    },
                    onRemove = { id ->
                        val count = cart.getOrDefault(id, 0)
                        if (count <= 1) {
                            cart.remove(id)
                        } else {
                            cart[id] = count - 1
                        }
                    },
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 14.dp)
                ) {
                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Fresh picks for you 🥬",
                        color = Ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Everyday essentials at listed prices",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search groceries...") },
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

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PaleGreen,
                                    selectedLabelColor = Green
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 18.dp)
                    ) {
                        items(
                            items = filteredGroceries,
                            key = { it.id }
                        ) { item ->
                            GroceryRow(
                                grocery = item,
                                quantity = cart.getOrDefault(item.id, 0),
                                onAdd = {
                                    cart[item.id] =
                                        cart.getOrDefault(item.id, 0) + 1
                                },
                                onRemove = {
                                    val count =
                                        cart.getOrDefault(item.id, 0)
                                    if (count <= 1) {
                                        cart.remove(item.id)
                                    } else {
                                        cart[item.id] = count - 1
                                    }
                                }
                            )
                        }

                        if (filteredGroceries.isEmpty()) {
                            item {
                                Text(
                                    text = "No groceries found.",
                                    modifier = Modifier.padding(20.dp),
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroceryRow(
    grocery: Grocery,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = grocery.imageUrl,
            contentDescription = grocery.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(82.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PaleGreen)
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = grocery.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Ink
            )
            Text(grocery.category, color = Color.Gray, fontSize = 12.sp)
            Text(grocery.unit, color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "₹${grocery.price}",
                color = Green,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (quantity == 0) {
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green
                ),
                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = 6.dp
                )
            ) {
                Text("ADD")
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(PaleGreen, RoundedCornerShape(10.dp))
                    .padding(horizontal = 2.dp)
            ) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Remove one"
                    )
                }
                Text(
                    text = "$quantity",
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
                IconButton(onClick = onAdd) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add one"
                    )
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
    onCheckout: () -> Unit,
    onAdd: (Int) -> Unit,
    onRemove: (Int) -> Unit,
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
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Your Cart",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
        }

        Spacer(Modifier.height(12.dp))

        if (cartItems.isEmpty()) {
            Text("Your cart is empty.", color = Color.Gray)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = cartItems,
                    key = { it.id }
                ) { item ->
                    val qty = cart.getOrDefault(item.id, 0)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color.White,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "₹${item.price} / ${item.unit}",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Subtotal: ₹${item.price * qty}",
                                color = Green,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { onRemove(item.id) }) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Remove item"
                            )
                        }

                        Text(
                            text = "$qty",
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = { onAdd(item.id) }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add item"
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "₹$totalPrice",
                    fontSize = 22.sp,
                    
fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green
                )
            ) {
                Text("Proceed to Checkout")
            }
        }
    }
}

@Composable
fun CheckoutScreen(
    totalPrice: Int,
    onBack: () -> Unit,
    onPlaceOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = "Checkout",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Order Total: ₹$totalPrice",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Green
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Delivery Address") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (
                    customerName.isNotBlank() &&
                    phone.length == 10 &&
                    phone.all { it.isDigit() } &&
                    address.isNotBlank()
                ) {
                    onPlaceOrder()
                }
            },
            enabled = customerName.isNotBlank() &&
                phone.length == 10 &&
                phone.all { it.isDigit() } &&
                address.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Green
            )
        ) {
            Text("Place Demo Order")
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Demo checkout only. No real payment is processed.",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

