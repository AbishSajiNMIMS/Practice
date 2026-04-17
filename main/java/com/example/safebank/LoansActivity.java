package com.example.safebank;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class LoansActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loans);

        ImageView btnBack = findViewById(R.id.btnBack);
        MaterialCardView cardLoanCalculator = findViewById(R.id.cardLoanCalculator);
        MaterialCardView cardHomeLoan = findViewById(R.id.cardHomeLoan);
        MaterialCardView cardPersonalLoan = findViewById(R.id.cardPersonalLoan);
        MaterialCardView cardCarLoan = findViewById(R.id.cardCarLoan);

        btnBack.setOnClickListener(v -> finish());

        cardLoanCalculator.setOnClickListener(v -> {
            Intent intent = new Intent(LoansActivity.this, LoanCalculatorActivity.class);
            startActivity(intent);
        });

        cardHomeLoan.setOnClickListener(v -> {
            Intent intent = new Intent(LoansActivity.this, ApplyHomeLoanActivity.class);
            intent.putExtra("LOAN_TYPE", "Home Loan");
            startActivity(intent);
        });

        cardPersonalLoan.setOnClickListener(v -> {
            Intent intent = new Intent(LoansActivity.this, ApplyHomeLoanActivity.class);
            intent.putExtra("LOAN_TYPE", "Personal Loan");
            startActivity(intent);
        });

        cardCarLoan.setOnClickListener(v -> {
            Intent intent = new Intent(LoansActivity.this, ApplyHomeLoanActivity.class);
            intent.putExtra("LOAN_TYPE", "Car Loan");
            startActivity(intent);
        });
    }
}