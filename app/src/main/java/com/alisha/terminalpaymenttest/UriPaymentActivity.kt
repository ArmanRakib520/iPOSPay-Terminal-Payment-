package com.alisha.terminalpaymenttest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class UriPaymentActivity : AppCompatActivity() {
    
    private lateinit var tpnEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var tipEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var saleButton: Button
    private lateinit var resultTextView: TextView
    
    private lateinit var paymentLauncher: ActivityResultLauncher<Intent>
    
    private var isTerminalRegistered = false
    private val TAG = "UriPaymentActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uri_payment)
        
        // Initialize UI elements
        tpnEditText = findViewById(R.id.tpnEditText)
        amountEditText = findViewById(R.id.amountEditText)
        tipEditText = findViewById(R.id.tipEditText)
        registerButton = findViewById(R.id.registerButton)
        saleButton = findViewById(R.id.saleButton)
        resultTextView = findViewById(R.id.resultTextView)
        
        // Initialize activity result launcher
        setupActivityResultLauncher()
        
        // Setup button listeners
        setupButtonListeners()
    }
    
    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun setupActivityResultLauncher() {
        paymentLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val data = result.data
                val transactionResult = data?.getStringExtra("transactionResult")
                
                if (transactionResult != null) {
                    try {
                        displayTransactionResult(transactionResult)
                    } catch (e: Exception) {
                        resultTextView.text = "Error parsing result: ${e.message}"
                    }
                } else {
                    resultTextView.text = "No transaction result returned"
                }
            } else if (result.resultCode == RESULT_CANCELED) {
                resultTextView.text = "Transaction was canceled"
            }
        }
    }
    
    private fun setupButtonListeners() {
        // Register terminal button
        registerButton.setOnClickListener {
            val tpn = tpnEditText.text.toString().trim()
            
            if (tpn.length != 12) {
                showDialog("Validation Error", "TPN must be 12 digits")
                return@setOnClickListener
            }
            
            registerTerminal(tpn)
        }
        
        // Process sale button
        saleButton.setOnClickListener {
            val amountText = amountEditText.text.toString().trim()
            val tipText = tipEditText.text.toString().trim()
            
            if (amountText.isEmpty()) {
                showDialog("Validation Error", "Please enter an amount")
                return@setOnClickListener
            }
            
            try {
                val amount = amountText.toDouble()
                val tip = if (tipText.isEmpty()) 0.0 else tipText.toDouble()
                
                if (amount <= 0) {
                    showDialog("Validation Error", "Amount must be greater than 0")
                    return@setOnClickListener
                }
                
                processSale(amount, tip)
            } catch (e: NumberFormatException) {
                showDialog("Validation Error", "Invalid amount format")
            }
        }
    }
    
    private fun registerTerminal(tpn: String) {
        try {
            // Create registration JSON object
            val jsonRequest = JSONObject().apply {
                put("tpn", tpn)
                put("applicationType", "DVPAYLITE")
            }
            
            // Send request to register terminal using URI scheme
            launchDvPayLiteUri("register", jsonRequest.toString())
            
            resultTextView.text = "Registering terminal with TPN: $tpn...\n\n"
        } catch (e: Exception) {
            showDialog("Error", "Error: ${e.message}")
            resultTextView.text = "Error: ${e.message}"
        }
    }
    
    private fun processSale(amount: Double, tip: Double = 0.0) {
        try {
            // Format amount and tip to 2 decimal places
            val formattedAmount = String.format(Locale.US, "%.2f", amount)
            val formattedTip = String.format(Locale.US, "%.2f", tip)
            
            // Generate a unique reference ID
            val refId = UUID.randomUUID().toString().substring(0, 10)
            
            // Create sale transaction JSON object
            val jsonRequest = JSONObject().apply {
                put("type", "SALE")
                put("paymentType", "CREDIT")
                put("amount", formattedAmount)
                put("tip", formattedTip)
                put("applicationType", "DVPAYLITE")
                put("refId", refId)
                put("receiptType", "Both") // Print both merchant and customer receipts
            }
            
            // Send request to process sale using URI scheme
            launchDvPayLiteUri("pay", jsonRequest.toString())
            
            resultTextView.text = "Processing sale...\nAmount: $formattedAmount\nTip: $formattedTip\nRef ID: $refId\n\n"
        } catch (e: Exception) {
            showDialog("Error", "Error: ${e.message}")
            resultTextView.text = "Error: ${e.message}"
        }
    }
    
    private fun launchDvPayLiteUri(host: String, data: String) {
        try {
            // URI format: denovopay://[host]?data=[json-data-url-encoded]
            val encodedData = URLEncoder.encode(data, "UTF-8")
            val uriString = "denovopay://$host?data=$encodedData"
            val uri = Uri.parse(uriString)
            
            val intent = Intent(Intent.ACTION_VIEW, uri)
            
            // Verify if there's an app to handle this intent
            if (intent.resolveActivity(packageManager) != null) {
                paymentLauncher.launch(intent)
            } else {
                val errorMessage = "DVPayLite app is not installed on this device"
                showDialog("App Missing", errorMessage)
                resultTextView.text = errorMessage
            }
        } catch (e: Exception) {
            showDialog("Launch Error", "Error launching DVPayLite: ${e.message}")
            resultTextView.text = "Error launching DVPayLite: ${e.message}"
        }
    }
    
    private fun displayTransactionResult(transactionResultJson: String) {
        try {
            val result = JSONObject(transactionResultJson)
            
            val status = result.optString("transactionStatus", "UNKNOWN")
            val approved = status.equals("APPROVED", ignoreCase = true)
            
            val amount = result.optString("amount", "0.00")
            val tip = result.optString("tip", "0.00")
            val total = result.optString("totalAmount", "0.00")
            val cardType = result.optString("cardType", "Unknown")
            val last4 = result.optString("last4", "****")
            val authCode = result.optString("authCode", "N/A")
            val refId = result.optString("refId", "N/A")
            val receiptData = result.optString("receiptText", "")
            
            val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            
            val resultBuilder = StringBuilder()
            resultBuilder.append("Payment ${if (approved) "Approved" else "Declined"}\n")
            resultBuilder.append("Transaction Date: $formattedDate\n")
            resultBuilder.append("Status: $status\n")
            resultBuilder.append("Amount: $amount\n")
            resultBuilder.append("Tip: $tip\n")
            resultBuilder.append("Total: $total\n")
            resultBuilder.append("Card Type: $cardType\n")
            resultBuilder.append("Card: xxxx-xxxx-xxxx-$last4\n")
            resultBuilder.append("Auth Code: $authCode\n")
            resultBuilder.append("Reference ID: $refId\n")
            
            if (receiptData.isNotEmpty()) {
                resultBuilder.append("\n--- RECEIPT ---\n$receiptData\n")
            }
            
            resultTextView.text = resultBuilder.toString()
            
            // Check if it's a terminal registration response
            if (result.has("isValidTPN")) {
                val isValidTPN = result.optBoolean("isValidTPN", false)
                val message = result.optString("message", "Unknown result")
                
                if (isValidTPN) {
                    isTerminalRegistered = true
                    saleButton.isEnabled = true
                    
                    resultTextView.text = "Terminal Registration Success:\n$message\n\n"
                    
                    showDialog("Success", "Terminal registered successfully")
                } else {
                    resultTextView.text = "Terminal Registration Failed:\n$message\n\n"
                    
                    showDialog("Registration Failed", "Terminal registration failed: $message")
                }
            }
            
        } catch (e: JSONException) {
            resultTextView.text = "Error parsing transaction result: ${e.message}\n\nRaw data: $transactionResultJson"
            showDialog("Parse Error", "Error parsing transaction result: ${e.message}")
        }
    }
} 