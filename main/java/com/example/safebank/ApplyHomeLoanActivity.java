package com.example.safebank;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ApplyHomeLoanActivity extends AppCompatActivity {

    private TextView tvIncomeValue, tvLoanValue, tvEmploymentType, tvLoanTitle, tvLoanPromoTitle;
    private MaterialCardView cardIncome, cardLoanAmount, cardEmployment;
    private ImageView ivLoanPromotion;
    private DBHelper dbHelper;
    private String userPhone, userName, loanType;
    private static final int PERMISSION_SEND_SMS = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_home_loan);

        dbHelper = new DBHelper(this);
        
        SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
        userPhone = sp.getString("current_user_phone", "");
        
        DBHelper.AccountInfo info = dbHelper.getAccountInfo(userPhone);
        userName = info.name;

        loanType = getIntent().getStringExtra("LOAN_TYPE");
        if (loanType == null) loanType = "Home Loan";

        tvLoanTitle = findViewById(R.id.tvLoanTitle);
        tvLoanPromoTitle = findViewById(R.id.tvLoanPromoTitle);
        ivLoanPromotion = findViewById(R.id.ivLoanPromotion);
        tvIncomeValue = findViewById(R.id.tvIncomeValue);
        tvLoanValue = findViewById(R.id.tvLoanValue);
        tvEmploymentType = findViewById(R.id.tvEmploymentType);
        
        cardIncome = findViewById(R.id.cardIncome);
        cardLoanAmount = findViewById(R.id.cardLoanAmount);
        cardEmployment = findViewById(R.id.cardEmployment);
        
        ImageView btnBack = findViewById(R.id.btnBack);
        Button btnContinue = findViewById(R.id.btnContinue);

        btnBack.setOnClickListener(v -> finish());

        setupLoanTypeUI();

        cardIncome.setOnClickListener(v -> {
            String[] options = {"₹ 25,000", "₹ 50,000", "₹ 75,000", "₹ 1,00,000"};
            showPicker("Select Monthly Income", options, tvIncomeValue);
        });

        cardLoanAmount.setOnClickListener(v -> {
            String[] options;
            if (loanType.equals("Home Loan")) {
                options = new String[]{"₹ 10,00,000", "₹ 25,00,000", "₹ 50,00,000", "₹ 1,00,00,000"};
            } else if (loanType.equals("Car Loan")) {
                options = new String[]{"₹ 5,00,000", "₹ 10,00,000", "₹ 15,00,000", "₹ 20,00,000"};
            } else {
                options = new String[]{"₹ 1,00,000", "₹ 2,00,000", "₹ 5,00,000", "₹ 10,00,000"};
            }
            showPicker("Select Loan Amount", options, tvLoanValue);
        });

        cardEmployment.setOnClickListener(v -> {
            String[] options = {"Salaried", "Self-Employed", "Business Owner", "Others"};
            showPicker("Select Employment Type", options, tvEmploymentType);
        });

        btnContinue.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_SEND_SMS);
            } else {
                processLoanApplication();
            }
        });
    }

    private void setupLoanTypeUI() {
        tvLoanTitle.setText("Apply for " + loanType);
        tvLoanPromoTitle.setText("Get a " + loanType + " with SafeBank");
        
        if (loanType.equals("Car Loan")) {
            ivLoanPromotion.setImageResource(android.R.drawable.ic_menu_directions);
            tvLoanValue.setText("₹ 5,00,000");
        } else if (loanType.equals("Personal Loan")) {
            ivLoanPromotion.setImageResource(android.R.drawable.ic_menu_edit);
            tvLoanValue.setText("₹ 1,00,000");
        }
    }

    private void notifyViaSms(String message) {
        if (userPhone != null && !userPhone.isEmpty() && ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                ArrayList<String> parts = smsManager.divideMessage(message);
                smsManager.sendMultipartTextMessage(userPhone, null, parts, null, null);
                Toast.makeText(this, "Loan SMS sent to " + userPhone, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "SMS Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    private void processLoanApplication() {
        String incomeStr = tvIncomeValue.getText().toString();
        String loanStr = tvLoanValue.getText().toString();
        String employment = tvEmploymentType.getText().toString();

        if (employment.contains("Select")) {
            Toast.makeText(this, "Please select employment type", Toast.LENGTH_SHORT).show();
            return;
        }

        long income = Long.parseLong(incomeStr.replaceAll("[^0-9]", ""));
        long loanRequested = Long.parseLong(loanStr.replaceAll("[^0-9]", ""));
        double currentBalance = dbHelper.getBalance(userPhone);

        boolean isApproved = false;
        String reason = "";

        if (dbHelper.hasLoan(userPhone, loanType)) {
            reason = "You already have an active " + loanType;
        } else if (currentBalance < 30000) {
            reason = "Account balance is less than ₹30,000";
        } else if (income <= 30000) {
            reason = "Monthly income must be above ₹30,000";
        } else if (employment.equals("Others")) {
            reason = "Employment type not eligible";
        } else {
            isApproved = true;
        }

        String statusMessage;
        if (isApproved) {
            statusMessage = "Safe Bank : Congratulations " + userName + "! Your " + loanType + " of " + loanStr + " is APPROVED and added to your account.";
            dbHelper.updateBalance(userPhone, (double) loanRequested);
            String date = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date());
            dbHelper.addTransaction(userPhone, loanType + " Disbursed", date, "+ " + loanStr, "CREDIT");
        } else {
            statusMessage = "Safe Bank : Sorry " + userName + ", your " + loanType + " application for " + loanStr + " was REJECTED. Reason: " + reason;
        }

        notifyViaSms(statusMessage);
        
        new android.os.Handler().postDelayed(this::finish, 1000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                processLoanApplication();
            } else {
                Toast.makeText(this, "SMS permission denied. Loan application failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showPicker(String title, String[] options, TextView target) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(options, (dialog, which) -> {
                    target.setText(options[which]);
                    target.setTextColor(getResources().getColor(R.color.primary_blue));
                }).show();
    }
}