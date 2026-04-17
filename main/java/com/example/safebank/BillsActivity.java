package com.example.safebank;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BillsActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private String userName, userPhone;
    private static final int PERMISSION_SEND_SMS = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bills);

        dbHelper = new DBHelper(this);

        SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
        userPhone = sp.getString("current_user_phone", "");

        DBHelper.AccountInfo info = dbHelper.getAccountInfo(userPhone);
        userName = info.name;
        userPhone = info.phone;

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        MaterialCardView billElectricity = findViewById(R.id.billElectricity);
        MaterialCardView billCable = findViewById(R.id.billCable);
        MaterialCardView billMobile = findViewById(R.id.billMobile);

        billElectricity.setOnClickListener(v -> showPaymentDialog("Electricity Bill", 1250.00));
        billCable.setOnClickListener(v -> showPaymentDialog("Cable Bill", 450.00));
        billMobile.setOnClickListener(v -> showPaymentDialog("Mobile Recharge", 599.00));
    }

    private void showPaymentDialog(String title, double amount) {
        new AlertDialog.Builder(this)
                .setTitle("Pay " + title)
                .setMessage("Amount: ₹ " + amount + "\nDo you want to proceed with the payment?")
                .setPositiveButton("Pay Now", (dialog, which) -> processPayment(title, amount))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void processPayment(String title, double amount) {
        double balance = dbHelper.getBalance(userPhone);
        if (balance < amount) {
            Toast.makeText(this, "Insufficient balance", Toast.LENGTH_SHORT).show();
            return;
        }

        dbHelper.updateBalance(userPhone, -amount);
        String date = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date());
        dbHelper.addTransaction(userPhone, title + " Paid", date, "- ₹ " + amount, "DEBIT");

        sendTransactionSms(title, amount);

        Toast.makeText(this, title + " paid successfully!", Toast.LENGTH_SHORT).show();
    }

    private void sendTransactionSms(String title, double amount) {
        if (userPhone == null || userPhone.isEmpty()) return;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(userPhone, null, "SafeBank: Transaction successful. " + title + " of ₹ " + amount + " paid.", null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_SEND_SMS);
        }
    }
}