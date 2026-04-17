package com.example.safebank;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class CardsActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private String userPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cards);

        dbHelper = new DBHelper(this);
        
        SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
        userPhone = sp.getString("current_user_phone", "");

        DBHelper.AccountInfo info = dbHelper.getAccountInfo(userPhone);

        TextView tvCardHolder = findViewById(R.id.tvCardHolder);
        TextView tvCardNumber = findViewById(R.id.tvCardNumber); // Assuming this exists or using existing view

        if (info != null && info.name != null) {
            tvCardHolder.setText(info.name.toUpperCase());
        }
        
        if (tvCardNumber != null) {
            tvCardNumber.setText(info.accountNo);
        }

        RelativeLayout btnTransactionHistory = findViewById(R.id.btnTransactionHistory);

        btnTransactionHistory.setOnClickListener(v -> {
            Intent intent = new Intent(CardsActivity.this, TransactionHistoryActivity.class);
            startActivity(intent);
        });

        // Bottom Navigation
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navCards = findViewById(R.id.navCards);
        LinearLayout navAccount = findViewById(R.id.navAccount);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, Home.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        navAccount.setOnClickListener(v -> {
            Intent intent = new Intent(this, AccountActivity.class);
            startActivity(intent);
            finish();
        });
    }
}