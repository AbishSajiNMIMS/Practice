package com.example.safebank;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class FixedDepositActivity extends AppCompatActivity {

    private TextView tvFdAmount, tvFdTenure, tvMyFdsTitle;
    private LinearLayout fdsContainer;
    private DBHelper dbHelper;
    private String userName, userPhone;
    private static final int PERMISSION_SEND_SMS = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fixed_deposit);

        dbHelper = new DBHelper(this);
        
        SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
        userPhone = sp.getString("current_user_phone", "");

        DBHelper.AccountInfo info = dbHelper.getAccountInfo(userPhone);
        userName = info.name;
        userPhone = info.phone;

        tvFdAmount = findViewById(R.id.tvFdAmount);
        tvFdTenure = findViewById(R.id.tvFdTenure);
        tvMyFdsTitle = findViewById(R.id.tvMyFdsTitle);
        fdsContainer = findViewById(R.id.fdsContainer);

        MaterialCardView cardAmount = findViewById(R.id.cardFdAmount);
        MaterialCardView cardTenure = findViewById(R.id.cardFdTenure);
        
        ImageView btnBack = findViewById(R.id.btnBack);
        Button btnCreateFd = findViewById(R.id.btnCreateFd);

        btnBack.setOnClickListener(v -> finish());

        cardAmount.setOnClickListener(v -> {
            String[] options = {"₹ 10,000", "₹ 25,000", "₹ 50,000", "₹ 1,00,000"};
            showPicker("Select FD Amount", options, tvFdAmount);
        });

        cardTenure.setOnClickListener(v -> {
            String[] options = {"1 Year (6.5%)", "2 Years (7.0%)", "3 Years (7.25%)", "5 Years (7.5%)"};
            showPicker("Select Tenure", options, tvFdTenure);
        });

        btnCreateFd.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_SEND_SMS);
            } else {
                createFD();
            }
        });

        updateFDList();
    }

    private void updateFDList() {
        List<DBHelper.FixedDeposit> list = dbHelper.getAllFixedDeposits(userPhone);
        fdsContainer.removeAllViews();
        
        if (list.isEmpty()) {
            tvMyFdsTitle.setVisibility(View.GONE);
        } else {
            tvMyFdsTitle.setVisibility(View.VISIBLE);
            for (DBHelper.FixedDeposit fd : list) {
                View itemView = LayoutInflater.from(this).inflate(R.layout.item_fd, fdsContainer, false);
                ((TextView) itemView.findViewById(R.id.tvFdNo)).setText("FD No: " + fd.fdNo);
                ((TextView) itemView.findViewById(R.id.tvFdAmountVal)).setText(fd.amount);
                ((TextView) itemView.findViewById(R.id.tvOpeningDate)).setText("Opening Date: " + fd.openingDate);
                ((TextView) itemView.findViewById(R.id.tvMaturityDate)).setText("Maturity Date: " + fd.maturityDate);
                ((TextView) itemView.findViewById(R.id.tvTenureVal)).setText("Tenure: " + fd.tenure);
                ((TextView) itemView.findViewById(R.id.tvRoiVal)).setText("ROI: " + fd.interestRate);
                
                itemView.findViewById(R.id.btnCloseFd).setOnClickListener(v -> showCloseFdDialog(fd));
                
                fdsContainer.addView(itemView);
            }
        }
    }

    private void showCloseFdDialog(DBHelper.FixedDeposit fd) {
        new AlertDialog.Builder(this)
                .setTitle("Close Deposit")
                .setMessage("Are you sure you want to close FD " + fd.fdNo + "? The amount " + fd.amount + " will be credited back to your account.")
                .setPositiveButton("Close FD", (dialog, which) -> {
                    double amount = Double.parseDouble(fd.amount.replaceAll("[^0-9]", ""));
                    dbHelper.updateBalance(userPhone, amount);
                    dbHelper.deleteFixedDeposit(userPhone, fd.fdNo);
                    
                    String date = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date());
                    dbHelper.addTransaction(userPhone, "FD Closed (" + fd.fdNo + ")", date, "+ " + fd.amount, "CREDIT");
                    
                    String statusMessage = "Safe Bank : " + userName + ", your Fixed Deposit " + fd.fdNo + " of " + fd.amount + " is closed and the amount is credited to your account.";
                    notifyViaSms(statusMessage);
                    
                    updateFDList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createFD() {
        if (dbHelper.getFDCount(userPhone) >= 5) {
            Toast.makeText(this, "Maximum 5 FDs allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = tvFdAmount.getText().toString();
        String tenureStr = tvFdTenure.getText().toString();
        
        if (tenureStr.contains("Select")) {
            Toast.makeText(this, "Please select tenure", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr.replaceAll("[^0-9]", ""));
        double balance = dbHelper.getBalance(userPhone);

        if (balance < amount) {
            Toast.makeText(this, "Insufficient balance to create FD", Toast.LENGTH_SHORT).show();
            return;
        }

        // Genrate FD No
        String fdNo = "FD" + (100000 + new Random().nextInt(900000));
        
        // Dates
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String openingDate = sdf.format(new Date());
        
        int years = Integer.parseInt(tenureStr.substring(0, 1));
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, years);
        String maturityDate = sdf.format(cal.getTime());
        
        String roi = tenureStr.substring(tenureStr.indexOf("(") + 1, tenureStr.indexOf(")"));

        dbHelper.updateBalance(userPhone, -amount);
        dbHelper.addTransaction(userPhone, "FD Created (" + fdNo + ")", openingDate, "- " + amountStr, "DEBIT");
        dbHelper.addFixedDeposit(userPhone, fdNo, amountStr, openingDate, maturityDate, tenureStr.split(" ")[0] + " " + tenureStr.split(" ")[1], roi);

        String statusMessage = "Safe Bank : Congratulations " + userName + "! Your Fixed Deposit of " + amountStr + " (FD No: " + fdNo + ") is created successfully.";
        notifyViaSms(statusMessage);
        
        updateFDList();
    }

    private void notifyViaSms(String message) {
        if (userPhone != null && !userPhone.isEmpty() && ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager smsManager;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    smsManager = this.getSystemService(SmsManager.class);
                } else {
                    smsManager = SmsManager.getDefault();
                }
                
                if (smsManager != null) {
                    ArrayList<String> parts = smsManager.divideMessage(message);
                    smsManager.sendMultipartTextMessage(userPhone, null, parts, null, null);
                    Toast.makeText(this, "SMS notification sent", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "SMS Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createFD();
            } else {
                Toast.makeText(this, "SMS Permission denied", Toast.LENGTH_SHORT).show();
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
