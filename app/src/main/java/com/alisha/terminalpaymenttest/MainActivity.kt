package com.alisha.terminalpaymenttest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.denovo.app.invokeiposgo.interfaces.TerminalAddListener
import com.denovo.app.invokeiposgo.interfaces.TransactionListener
import com.denovo.app.invokeiposgo.launcher.IntentApplication
import com.denovo.app.invokeiposgo.models.InvokeApp
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {
    
    private lateinit var tpnEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var tipEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var saleButton: Button
    private lateinit var uriPaymentButton: Button
    private lateinit var resultTextView: TextView
    
    private lateinit var activityResultLauncher: ActivityResultLauncher<android.content.Intent>
    private lateinit var intentApplication: IntentApplication
    
    private var isTerminalRegistered = false
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        // Initialize UI elements
        tpnEditText = findViewById(R.id.tpnEditText)
        amountEditText = findViewById(R.id.amountEditText)
        tipEditText = findViewById(R.id.tipEditText)
        registerButton = findViewById(R.id.registerButton)
        saleButton = findViewById(R.id.saleButton)
        uriPaymentButton = findViewById(R.id.uriPaymentButton)
        resultTextView = findViewById(R.id.resultTextView)
        
        // Initialize DVPayLite
        setupDVPayLite()
        
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
    
    private fun setupDVPayLite() {
        // Initialize result launcher for Intent results
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // This callback handles result from the DVPayLite app
            intentApplication.handleResultCallBack(result)
        }
        
        // Initialize the IntentApplication with context
        intentApplication = IntentApplication(this)
        
        // Set up Terminal Registration Listener
        setupTerminalListener()
        
        // Set up Transaction Listener
        setupTransactionListener()
    }
    
    private fun setupTerminalListener() {
        // Listener for terminal registration results
        intentApplication.setTerminalAddListener(object : TerminalAddListener {
            override fun onTerminalAdded(terminalResult: JSONObject?) {
                // Terminal was successfully registered
                terminalResult?.let {
                    val isValid = it.optBoolean("isValidTPN", false)
                    val message = it.optString("message", "Unknown result")
                    
                    runOnUiThread {
                        if (isValid) {
                            isTerminalRegistered = true
                            saleButton.isEnabled = true
                            
                            val resultText = "Terminal Registration Success:\n$message\n\n"
                            resultTextView.text = resultText
                            
                            showDialog("Success", "Terminal registered successfully")
                        } else {
                            val resultText = "Terminal Registration Failed:\n$message\n\n"
                            resultTextView.text = resultText
                            
                            showDialog("Registration Failed", "Terminal registration failed: $message")
                        }
                    }
                }
            }
            
            override fun onTerminalAddFailed(errorResult: JSONObject) {
                // Terminal registration failed
                val error = errorResult.optString("error", "Unknown error")
                
                runOnUiThread {
                    val resultText = "Terminal Registration Error:\n$error\n\n"
                    resultTextView.text = resultText
                    
                    showDialog("Registration Error", "Registration failed: $error")
                }
            }
            
            override fun onApplicationLaunched(data: JSONObject?) {
                Log.d(TAG, "DVPayLite application launched for registration")
            }
            
            override fun onApplicationLaunchFailed(error: JSONObject?) {
                runOnUiThread {
                    val errorMsg = error?.optString("error_message") ?: "Unknown error"
                    val resultText = "Failed to launch DVPayLite app: $errorMsg\n\n"
                    resultTextView.text = resultText
                    
                    showDialog("Launch Failed", "Failed to launch DVPayLite: $errorMsg")
                }
            }
        })
    }
    
    private fun setupTransactionListener() {
        // Listener for transaction results (sales, refunds, etc.)
        intentApplication.setTransactionListener(object : TransactionListener {
            override fun onTransactionSuccess(result: JSONObject?) {
                // Payment transaction completed
                result?.let {
                    runOnUiThread {
                        val status = it.optString("transactionStatus", "UNKNOWN")
                        val approved = status.equals("APPROVED", ignoreCase = true)
                        
                        val amount = it.optString("amount", "0.00")
                        val tip = it.optString("tip", "0.00")
                        val total = it.optString("totalAmount", "0.00")
                        val cardType = it.optString("cardType", "Unknown")
                        val last4 = it.optString("last4", "****")
                        val authCode = it.optString("authCode", "N/A")
                        val refId = it.optString("refId", "N/A")
                        val receiptData = it.optString("receiptText", "")
                        
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
                        
                        val toastMessage = if (approved) "Payment approved!" else "Payment declined: $status"
                        showDialog(if (approved) "Payment Approved" else "Payment Declined", toastMessage)
                    }
                }
            }
            
            override fun onTransactionFailed(error: JSONObject?) {
                runOnUiThread {
                    val errorMsg = error?.optString("error_message") ?: "Unknown error"
                    val resultText = "Transaction failed: $errorMsg\n\n"
                    resultTextView.text = resultText
                    
                    showDialog("Transaction Failed", "Transaction failed: $errorMsg")
                }
            }
            
            override fun onApplicationLaunched(data: JSONObject?) {
                Log.d(TAG, "DVPayLite application launched for transaction")
            }
            
            override fun onApplicationLaunchFailed(error: JSONObject?) {
                runOnUiThread {
                    val errorMsg = error?.optString("error_message") ?: "Unknown error"
                    val resultText = "Failed to launch DVPayLite app: $errorMsg\n\n"
                    resultTextView.text = resultText
                    
                    showDialog("Launch Failed", "Failed to launch DVPayLite: $errorMsg")
                }
            }
        })
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
        
        // Open URI payment activity button
        uriPaymentButton.setOnClickListener {
            val intent = Intent(this, UriPaymentActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun registerTerminal(tpn: String) {
        try {
            // Create registration JSON object
            val jsonRequest = JSONObject().apply {
                put("tpn", tpn)
                put("applicationType", "DVPAYLITE")
            }
            
            // Send request to register terminal
            intentApplication.addTerminal(jsonRequest, activityResultLauncher)
            
            resultTextView.text = "Registering terminal with TPN: $tpn...\n\n"
        } catch (e: Exception) {
            showDialog("Error", "Error: ${e.message}")
            Log.e(TAG, "Error registering terminal", e)
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
            
            // Send request to process sale
            intentApplication.performTransaction(jsonRequest, activityResultLauncher)
            
            resultTextView.text = "Processing sale...\nAmount: $formattedAmount\nTip: $formattedTip\nRef ID: $refId\n\n"
        } catch (e: Exception) {
            showDialog("Error", "Error: ${e.message}")
            Log.e(TAG, "Error processing sale", e)
        }
    }
}