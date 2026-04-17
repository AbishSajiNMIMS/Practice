package com.example.safebank;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class TransactionHistoryActivity extends AppCompatActivity {

    private LinearLayout transactionsContainer;
    private DBHelper dbHelper;
    private String userPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        dbHelper = new DBHelper(this);
        
        SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
        userPhone = sp.getString("current_user_phone", "");

        transactionsContainer = findViewById(R.id.transactionsContainer);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        updateUI();
    }

    private void updateUI() {
        transactionsContainer.removeAllViews();
        List<DBHelper.Transaction> list = dbHelper.getAllTransactions(userPhone);
        
        if (list.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No transaction history");
            tvEmpty.setPadding(20, 50, 20, 20);
            tvEmpty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tvEmpty.setTextColor(getResources().getColor(R.color.text_grey));
            transactionsContainer.addView(tvEmpty);
            return;
        }

        for (DBHelper.Transaction t : list) {
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

            transactionsContainer.addView(itemView);
        }
    }
}