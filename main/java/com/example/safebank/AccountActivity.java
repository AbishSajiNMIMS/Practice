package com.example.safebank;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class AccountActivity extends AppCompatActivity {

    private TextView tvAccName, tvAccNo, tvAccPhone;
    private DBHelper dbHelper;
    private static final int PERMISSION_SEND_SMS = 123;
    private String userName, userPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        dbHelper = new DBHelper(this);
        
        SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
        userPhone = sp.getString("current_user_phone", "");

        DBHelper.AccountInfo info = dbHelper.getAccountInfo(userPhone);

        tvAccName = findViewById(R.id.tvAccName);
        tvAccNo = findViewById(R.id.tvAccNo);
        tvAccPhone = findViewById(R.id.tvAccPhone);

        tvAccName.setText(info.name);
        tvAccNo.setText(info.accountNo);
        tvAccPhone.setText(info.phone);
        
        userName = info.name;

        LinearLayout optionBills = findViewById(R.id.optionBills);
        LinearLayout optionLoans = findViewById(R.id.optionLoans);
        LinearLayout optionFD = findViewById(R.id.optionFD);
        LinearLayout optionLogout = findViewById(R.id.optionLogout);

        optionBills.setOnClickListener(v -> {
            Intent intent = new Intent(this, BillsActivity.class);
            startActivity(intent);
        });
        
        optionLoans.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoansActivity.class);
            startActivity(intent);
        });

        optionFD.setOnClickListener(v -> {
            Intent intent = new Intent(this, FixedDepositActivity.class);
            startActivity(intent);
        });

        optionLogout.setOnClickListener(v -> logout());

        // Bottom Navigation
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, Home.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navCards).setOnClickListener(v -> {
            Intent intent = new Intent(this, CardsActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void logout() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_SEND_SMS);
        } else {
            sendLogoutSmsAndFinish();
        }
    }

    private void sendLogoutSmsAndFinish() {
        try {
            if (userPhone != null && !userPhone.isEmpty()) {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(userPhone, null, "SafeBank: " + userName + " has logged out", null, null);
            }
            
            // Clear session
            SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
            sp.edit().remove("current_user_phone").apply();
            
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Logout SMS failed", Toast.LENGTH_SHORT).show();
            
            // Still logout even if SMS fails
            SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
            sp.edit().remove("current_user_phone").apply();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_SEND_SMS) {
            sendLogoutSmsAndFinish();
        }
    }
}