package com.example.safebank;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class Home extends AppCompatActivity {

    private TextView tvBalance, tvGreeting, tvUserPhone, tvAccNoDisplay;
    private LinearLayout transactionsContainer;
    private DBHelper dbHelper;
    private String userName, userPhone;
    private static final int PERMISSION_SEND_SMS = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        dbHelper = new DBHelper(this);
        
        SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
        userPhone = sp.getString("current_user_phone", "");
        
        if (userPhone.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        DBHelper.AccountInfo info = dbHelper.getAccountInfo(userPhone);
        userName = info.name;

        tvBalance = findViewById(R.id.tvBalance);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        tvAccNoDisplay = findViewById(R.id.tvAccNoDisplay);
        transactionsContainer = findViewById(R.id.transactionsContainer);
        
        LinearLayout btnLoans = findViewById(R.id.btnLoans);
        LinearLayout btnFD = findViewById(R.id.btnFD);
        LinearLayout btnBills = findViewById(R.id.btnBills);
        TextView btnViewAllTransactions = findViewById(R.id.btnViewAllTransactions);

        if (userName != null && !userName.isEmpty()) {
            tvGreeting.setText("Hey, " + userName);
        } else {
            tvGreeting.setText("Hey, User");
        }
        
        tvUserPhone.setText(userPhone);
        if (tvAccNoDisplay != null) tvAccNoDisplay.setText(info.accountNo);

        // Quick Actions Navigation
        btnLoans.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, LoansActivity.class);
            startActivity(intent);
        });

        btnFD.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, FixedDepositActivity.class);
            startActivity(intent);
        });

        btnBills.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, BillsActivity.class);
            startActivity(intent);
        });

        // Navigation to Transaction History
        btnViewAllTransactions.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, TransactionHistoryActivity.class);
            startActivity(intent);
        });

        // Bottom Nav Listeners
        findViewById(R.id.navHome).setOnClickListener(v -> {
            // Already on home
        });

        findViewById(R.id.navCards).setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, CardsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.navAccount).setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, AccountActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        DBHelper.AccountInfo info = dbHelper.getAccountInfo(userPhone);
        userName = info.name;
        if (userName != null && !userName.isEmpty()) tvGreeting.setText("Hey, " + userName);
        tvUserPhone.setText(userPhone);
        if (tvAccNoDisplay != null) tvAccNoDisplay.setText(info.accountNo);
        updateUI();
    }

    private void updateUI() {
        // Update Balance
        double balance = dbHelper.getBalance(userPhone);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        tvBalance.setText(formatter.format(balance).replace(".00", ""));

        // Update Transactions
        transactionsContainer.removeAllViews();
        List<DBHelper.Transaction> list = dbHelper.getAllTransactions(userPhone);
        
        if (list.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No recent transactions");
            tvEmpty.setPadding(20, 20, 20, 20);
            tvEmpty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tvEmpty.setTextColor(getResources().getColor(R.color.text_grey));
            transactionsContainer.addView(tvEmpty);
            return;
        }

        // Only show first 3 transactions on home
        int count = 0;
        for (DBHelper.Transaction t : list) {
            if (count >= 3) break;
            
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_transaction, transactionsContainer, false);
            
            TextView title = itemView.findViewById(R.id.tvTransTitle);
            TextView date = itemView.findViewById(R.id.tvTransDate);
            TextView amount = itemView.findViewById(R.id.tvTransAmount);
            ImageView icon = itemView.findViewById(R.id.ivTransIcon);

            title.setText(t.title);
            date.setText(t.date);
            amount.setText(t.amount);

            if (t.type.equals("DEBIT")) {
                amount.setTextColor(getResources().getColor(R.color.text_black));
                icon.setImageResource(android.R.drawable.stat_sys_upload);
                icon.setColorFilter(getResources().getColor(R.color.red_debit));
            } else {
                amount.setTextColor(getResources().getColor(R.color.green_credit));
                icon.setImageResource(android.R.drawable.stat_sys_download);
                icon.setColorFilter(getResources().getColor(R.color.green_credit));
            }

            itemView.setOnClickListener(v -> {
                Toast.makeText(this, "Transaction Details for: " + t.title, Toast.LENGTH_SHORT).show();
            });

            transactionsContainer.addView(itemView);
            count++;
        }
    }
}