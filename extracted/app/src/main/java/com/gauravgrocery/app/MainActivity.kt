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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Green = Color(0xFF247A42)
private val PaleGreen = Color(0xFFF0F7EE)
private val Ink = Color(0xFF193B27)

data class Grocery(val id: Int, val name: String, val category: String, val unit: String, val price: Int, val emoji: String)

private val groceries = listOf(
    Grocery(1, "Tomatoes", "Vegetables", "1 kg", 30, "🍅"),
    Grocery(2, "Potatoes", "Vegetables", "1 kg", 35, "🥔"),
    Grocery(3, "Onions", "Vegetables", "1 kg", 40, "🧅"),
    Grocery(4, "Bananas", "Fruits", "1 dozen", 50, "🍌"),
    Grocery(5, "Apples", "Fruits", "1 kg", 140, "🍎"),
    Grocery(6, "Oranges", "Fruits", "1 kg", 80, "🍊"),
    Grocery(7, "Milk", "Dairy", "1 litre", 60, "🥛"),
    Grocery(8, "Curd", "Dairy", "400 g", 35, "🥣"),
    Grocery(9, "Rice", "Staples", "1 kg", 65, "🍚"),
    Grocery(10, "Wheat Flour", "Staples", "5 kg", 240, "🌾"),
    Grocery(11, "Eggs", "Dairy", "6 pieces", 55, "🥚"),
    Grocery(12, "Cooking Oil", "Staples", "1 litre", 145, "🫒")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GauravGroceryApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GauravGroceryApp() {
    val context = LocalContext.current
    val quantities = remember { mutableStateMapOf<Int, Int>() }
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showCart by remember { mutableStateOf(false) }
    var showCheckout by remember { mutableStateOf(false) }
    val categories = listOf("All", "Fruits", "Vegetables", "Dairy", "Staples")
    val cartCount = quantities.values.sum()
    val total = groceries.sumOf { it.price * (quantities[it.id] ?: 0) }
    val filtered = groceries.filter {
        (selectedCategory == "All" || it.category == selectedCategory) &&
            it.name.contains(search, ignoreCase = true)
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Green, secondary = Green, background = Color(0xFFFAFCF9))) {
        Scaffold(
            containerColor = Color(0xFFFAFCF9),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Gaurav Grocery", fontWeight = FontWeight.Bold, color = Green, fontSize = 22.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = Green, modifier = Modifier.size(14.dp))
                                Text("Fresh groceries delivered to you", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    },
                    actions = {
                        BadgedBox(badge = { if (cartCount > 0) Badge { Text(cartCount.toString()) } }) {
                            IconButton(onClick = { showCart = true }) {
                                Icon(Icons.Default.ShoppingCart, "Open cart", tint = Green)
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (cartCount > 0 && !showCart && !showCheckout) {
                    Surface(shadowElevation = 8.dp, color = Color.White) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("$cartCount item(s)", color = Color.Gray, fontSize = 12.sp)
                                Text("₹$total", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
                            }
                            Button(onClick = { showCart = true }, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                                Text("View cart")
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        ) { padding ->
            when {
                showCheckout -> CheckoutScreen(
                    total = total,
                    onBack = { showCheckout = false },
                    onPlaceOrder = { paymentMethod ->
                        Toast.makeText(
                            context,
                            "Demo checkout selected: $paymentMethod. Payment is not processed.",
                            Toast.LENGTH_LONG
                        ).show()
                        // Keep cart until a real backend confirms payment/order creation.
                        showCheckout = false
                        showCart = false
                    }
                )
                showCart -> CartScreen(quantities, onBack = { showCart = false }, onCheckout = {
                    if (cartCount > 0) showCheckout = true
                })
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Surface(color = PaleGreen, shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                                Text("Freshness delivered 🥬", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                                Text("Daily essentials from Gaurav Grocery.", color = Color(0xFF4D6C55))
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = search, onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search groceries...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true, shape = RoundedCornerShape(14.dp)
                        )
                    }
                    item { Text("Shop by category", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Ink) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            categories.forEach { category ->
                                FilterChip(selected = selectedCategory == category,
                                    onClick = { selectedCategory = category }, label = { Text(category) })
                            }
                        }
                    }
                    item { Text("Popular groceries", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Ink) }
                    items(filtered, key = { it.id }) { grocery ->
                        GroceryRow(grocery, quantities[grocery.id] ?: 0,
                            onAdd = { quantities[grocery.id] = (quantities[grocery.id] ?: 0) + 1 },
                            onRemove = {
                                val next = (quantities[grocery.id] ?: 0) - 1
                                if (next <= 0) quantities.remove(grocery.id) else quantities[grocery.id] = next
                            })
                    }
                    if (filtered.isEmpty()) item { Text("No groceries found.", color = Color.Gray) }
                }
            }
        }
    }
}

@Composable
private fun GroceryRow(grocery: Grocery, quantity: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(68.dp).background(PaleGreen, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Text(grocery.emoji, fontSize = 34.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(grocery.name, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 16.sp)
                Text(grocery.unit, color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text("₹${grocery.price}", color = Green, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            if (quantity == 0) {
                OutlinedButton(onClick = onAdd, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) { Text("ADD") }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Remove, "Remove one") }
                    Text(quantity.toString(), fontWeight = FontWeight.Bold)
                    IconButton(onClick = onAdd, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Add, "Add one") }
                }
            }
        }
    }
}

@Composable
private fun CartScreen(quantities: Map<Int, Int>, onBack: () -> Unit, onCheckout: () -> Unit) {
    val total = groceries.sumOf { it.price * (quantities[it.id] ?: 0) }
    val cartItems = groceries.filter { (quantities[it.id] ?: 0) > 0 }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Continue shopping") }
            Spacer(Modifier.weight(1f))
            Text("Your cart", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        Spacer(Modifier.height(12.dp))
        if (cartItems.isEmpty()) {
            Text("Your cart is empty. Add some groceries first.", color = Color.Gray)
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cartItems) { grocery ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(grocery.emoji, fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(grocery.name, fontWeight = FontWeight.SemiBold)
                            Text("${quantities[grocery.id]} × ₹${grocery.price}", color = Color.Gray)
                        }
                        Text("₹${grocery.price * (quantities[grocery.id] ?: 0)}", fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("₹$total", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Green)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onCheckout, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Proceed to checkout") }
        }
    }
}

@Composable
private fun CheckoutScreen(total: Int, onBack: () -> Unit, onPlaceOrder: (String) -> Unit) {
    var customerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Online payment (demo)") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Back to cart") }
        Text("Checkout", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Delivery details", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(customerName, { customerName = it }, label = { Text("Full name") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, label = { Text("10-digit mobile number") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(address, { address = it }, label = { Text("Full delivery address") },
            modifier = Modifier.fillMaxWidth(), minLines = 3)
        Text("Payment method", fontWeight = FontWeight.SemiBold)
        listOf("Online payment (demo)", "Cash on delivery").forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = paymentMethod == option, onClick = { paymentMethod = option })
                Text(option)
            }
        }
        Spacer(Modifier.weight(1f))
        Surface(color = PaleGreen, shape = RoundedCornerShape(14.dp)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Order total", fontWeight = FontWeight.SemiBold)
                Text("₹$total", fontWeight = FontWeight.Bold, color = Green, fontSize = 20.sp)
            }
        }
        Button(
            onClick = { onPlaceOrder(paymentMethod) },
            enabled = customerName.isNotBlank() && phone.length == 10 && address.isNotBlank() && total > 0,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Green)
        ) { Text(if (paymentMethod.startsWith("Online")) "Continue to payment" else "Place COD demo order") }
        Text("Demo UI only. No payment is charged and no order is sent to a store.", color = Color.Gray, fontSize = 12.sp)
    }
}
