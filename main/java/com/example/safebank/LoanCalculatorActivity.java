package com.example.safebank;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.NumberFormat;
import java.util.Locale;

public class LoanCalculatorActivity extends AppCompatActivity {

    private SeekBar sbAmount, sbRate, sbTenure;
    private EditText etAmountValue;
    private TextView tvRateValue, tvTenureValue;
    private TextView tvEMIResult, tvTotalInterest, tvTotalPayment;
    private Button btnCalculate;
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_calculator);

        // Initialize Views
        ImageView btnBack = findViewById(R.id.btnBack);
        sbAmount = findViewById(R.id.sbAmount);
        sbRate = findViewById(R.id.sbRate);
        sbTenure = findViewById(R.id.sbTenure);
        etAmountValue = findViewById(R.id.etAmountValue);
        tvRateValue = findViewById(R.id.tvRateValue);
        tvTenureValue = findViewById(R.id.tvTenureValue);
        tvEMIResult = findViewById(R.id.tvEMIResult);
        tvTotalInterest = findViewById(R.id.tvTotalInterest);
        tvTotalPayment = findViewById(R.id.tvTotalPayment);
        btnCalculate = findViewById(R.id.btnCalculate);

        btnBack.setOnClickListener(v -> finish());

        // Amount Input (Manual Entry)
        etAmountValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;
                
                try {
                    String val = s.toString();
                    if (val.isEmpty()) return;
                    
                    int amount = Integer.parseInt(val);
                    if (amount > 100000000) { // Cap at 10 Cr
                        amount = 100000000;
                        isUpdating = true;
                        etAmountValue.setText(String.valueOf(amount));
                        etAmountValue.setSelection(etAmountValue.getText().length());
                        isUpdating = false;
                    }
                    
                    isUpdating = true;
                    sbAmount.setProgress(amount);
                    isUpdating = false;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });

        // Amount SeekBar
        sbAmount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    isUpdating = true;
                    etAmountValue.setText(String.valueOf(progress));
                    isUpdating = false;
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Rate SeekBar (Multiplied by 10 for precision, e.g., 95 = 9.5%)
        sbRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvRateValue.setText((progress / 10.0) + " %");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Tenure SeekBar
        sbTenure.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvTenureValue.setText(progress + " years");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnCalculate.setOnClickListener(v -> calculateLoan());

        // Initial Calculation
        calculateLoan();
    }

    private void calculateLoan() {
        double p;
        try {
            p = Double.parseDouble(etAmountValue.getText().toString());
        } catch (Exception e) {
            p = 0;
        }

        double r = (sbRate.getProgress() / 10.0) / 12 / 100;
        int n = sbTenure.getProgress() * 12;

        if (n == 0 || r == 0) {
            tvEMIResult.setText("₹ 0 / month");
            tvTotalInterest.setText("₹ 0");
            tvTotalPayment.setText(formatCurrency((int)p));
            return;
        }

        double emi = (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
        double totalPayment = emi * n;
        double totalInterest = totalPayment - p;

        tvEMIResult.setText(formatCurrency((int)emi) + " / month");
        tvTotalInterest.setText(formatCurrency((int)totalInterest));
        tvTotalPayment.setText(formatCurrency((int)totalPayment));
    }

    private String formatCurrency(int amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return formatter.format(amount).replace(".00", "");
    }
}