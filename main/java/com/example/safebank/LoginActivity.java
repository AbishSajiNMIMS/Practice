package com.example.safebank;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Random;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etName, etPhone, etOtp;
    private LinearLayout layoutLogin, layoutOtp;
    private static final int PERMISSION_SEND_SMS = 123;
    private DBHelper dbHelper;
    private String generatedOtp;
    private String tempName, tempPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DBHelper(this);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etOtp = findViewById(R.id.etOtp);
        layoutLogin = findViewById(R.id.layoutLogin);
        layoutOtp = findViewById(R.id.layoutOtp);
        
        Button btnContinue = findViewById(R.id.btnContinue);
        Button btnLogin = findViewById(R.id.btnLogin);
        View tvBackToLogin = findViewById(R.id.tvResendOtp);

        btnContinue.setOnClickListener(v -> {
            tempName = etName.getText().toString();
            tempPhone = etPhone.getText().toString();

            if (tempName.isEmpty() || tempPhone.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_SEND_SMS);
            } else {
                sendOtp();
            }
        });

        btnLogin.setOnClickListener(v -> {
            String enteredOtp = etOtp.getText().toString();
            if (enteredOtp.equals(generatedOtp)) {
                saveAndLogin(tempName, tempPhone);
            } else {
                Toast.makeText(this, "Incorrect OTP", Toast.LENGTH_SHORT).show();
            }
        });

        tvBackToLogin.setOnClickListener(v -> {
            layoutOtp.setVisibility(View.GONE);
            layoutLogin.setVisibility(View.VISIBLE);
        });
    }

    private void sendOtp() {
        generatedOtp = String.format("%06d", new Random().nextInt(1000000));
        String message = "SafeBank Login OTP: " + generatedOtp;
        notifyViaSms(tempPhone, message);
        
        layoutLogin.setVisibility(View.GONE);
        layoutOtp.setVisibility(View.VISIBLE);
        Toast.makeText(this, "OTP sent to " + tempPhone, Toast.LENGTH_SHORT).show();
    }

    private void saveAndLogin(String name, String phone) {
        if (!dbHelper.accountExists(phone)) {
            dbHelper.createAccount(name, phone);
        }
        
        SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
        sp.edit().putString("current_user_phone", phone).apply();

        String welcomeMessage = "Safe Bank : Welcome " + name + "! You have successfully logged in to your account.";
        notifyViaSms(phone, welcomeMessage);

        Intent intent = new Intent(LoginActivity.this, Home.class);
        startActivity(intent);
        finish();
    }

    private void notifyViaSms(String phone, String message) {
        if (phone != null && !phone.isEmpty() && ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager smsManager;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    smsManager = this.getSystemService(SmsManager.class);
                } else {
                    smsManager = SmsManager.getDefault();
                }
                
                if (smsManager != null) {
                    ArrayList<String> parts = smsManager.divideMessage(message);
                    smsManager.sendMultipartTextMessage(phone, null, parts, null, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendOtp();
            } else {
                Toast.makeText(getApplicationContext(), "SMS permission denied. Login failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}