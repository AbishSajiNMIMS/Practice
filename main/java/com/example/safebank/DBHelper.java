package com.example.safebank;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {
        super(context, "BankDB", null, 10);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE account(phone TEXT PRIMARY KEY, name TEXT, balance REAL, card_locked INTEGER DEFAULT 0, account_no TEXT)");
        db.execSQL("CREATE TABLE transactions(id INTEGER PRIMARY KEY AUTOINCREMENT, user_phone TEXT, title TEXT, date TEXT, amount TEXT, type TEXT)");
        db.execSQL("CREATE TABLE fixed_deposits(id INTEGER PRIMARY KEY AUTOINCREMENT, user_phone TEXT, fd_no TEXT, amount TEXT, opening_date TEXT, maturity_date TEXT, tenure TEXT, interest_rate TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS account");
        db.execSQL("DROP TABLE IF EXISTS transactions");
        db.execSQL("DROP TABLE IF EXISTS fixed_deposits");
        onCreate(db);
    }

    public boolean accountExists(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT phone FROM account WHERE phone=?", new String[]{phone});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public void createAccount(String name, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("phone", phone);
        cv.put("balance", 50000.00); // Default starting balance
        cv.put("card_locked", 0);
        
        // Generate random account number: XXXX XXXX XXXX
        Random r = new Random();
        String accNo = String.format(Locale.US, "%04d %04d %04d", r.nextInt(10000), r.nextInt(10000), r.nextInt(10000));
        cv.put("account_no", accNo);
        
        db.insert("account", null, cv);
    }

    public double getBalance(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT balance FROM account WHERE phone=?", new String[]{phone});
        if (cursor.moveToFirst()) {
            double balance = cursor.getDouble(0);
            cursor.close();
            return balance;
        }
        cursor.close();
        return 0;
    }

    public AccountInfo getAccountInfo(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name, phone, account_no FROM account WHERE phone=?", new String[]{phone});
        if (cursor.moveToFirst()) {
            AccountInfo info = new AccountInfo(cursor.getString(0), cursor.getString(1), cursor.getString(2));
            cursor.close();
            return info;
        }
        cursor.close();
        return new AccountInfo("User", phone, "0000 0000 0000");
    }

    public boolean isCardLocked(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT card_locked FROM account WHERE phone=?", new String[]{phone});
        boolean locked = false;
        if (cursor.moveToFirst()) {
            locked = cursor.getInt(0) == 1;
        }
        cursor.close();
        return locked;
    }

    public void setCardLocked(String phone, boolean locked) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("card_locked", locked ? 1 : 0);
        db.update("account", cv, "phone=?", new String[]{phone});
    }

    public void updateBalance(String phone, double amountChange) {
        SQLiteDatabase db = this.getWritableDatabase();
        double currentBalance = getBalance(phone);
        ContentValues cv = new ContentValues();
        cv.put("balance", currentBalance + amountChange);
        db.update("account", cv, "phone=?", new String[]{phone});
    }

    public void addTransaction(String phone, String title, String date, String amount, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("user_phone", phone);
        cv.put("title", title);
        cv.put("date", date);
        cv.put("amount", amount);
        cv.put("type", type);
        db.insert("transactions", null, cv);
    }

    public List<Transaction> getAllTransactions(String phone) {
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT title, date, amount, type FROM transactions WHERE user_phone=? ORDER BY id DESC", new String[]{phone});
        if (cursor.moveToFirst()) {
            do {
                list.add(new Transaction(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public boolean hasLoan(String phone, String loanType) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM transactions WHERE user_phone=? AND title = ?", new String[]{phone, loanType + " Disbursed"});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public void addFixedDeposit(String phone, String fdNo, String amount, String openingDate, String maturityDate, String tenure, String interestRate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("user_phone", phone);
        cv.put("fd_no", fdNo);
        cv.put("amount", amount);
        cv.put("opening_date", openingDate);
        cv.put("maturity_date", maturityDate);
        cv.put("tenure", tenure);
        cv.put("interest_rate", interestRate);
        db.insert("fixed_deposits", null, cv);
    }

    public List<FixedDeposit> getAllFixedDeposits(String phone) {
        List<FixedDeposit> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT fd_no, amount, opening_date, maturity_date, tenure, interest_rate FROM fixed_deposits WHERE user_phone=? ORDER BY id DESC", new String[]{phone});
        if (cursor.moveToFirst()) {
            do {
                list.add(new FixedDeposit(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public int getFDCount(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT count(*) FROM fixed_deposits WHERE user_phone=?", new String[]{phone});
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public void deleteFixedDeposit(String phone, String fdNo) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("fixed_deposits", "user_phone=? AND fd_no=?", new String[]{phone, fdNo});
    }

    public static class Transaction {
        public String title, date, amount, type;
        public Transaction(String title, String date, String amount, String type) {
            this.title = title;
            this.date = date;
            this.amount = amount;
            this.type = type;
        }
    }

    public static class FixedDeposit {
        public String fdNo, amount, openingDate, maturityDate, tenure, interestRate;
        public FixedDeposit(String fdNo, String amount, String openingDate, String maturityDate, String tenure, String interestRate) {
            this.fdNo = fdNo;
            this.amount = amount;
            this.openingDate = openingDate;
            this.maturityDate = maturityDate;
            this.tenure = tenure;
            this.interestRate = interestRate;
        }
    }

    public static class AccountInfo {
        public String name, phone, accountNo;
        public AccountInfo(String name, String phone, String accountNo) {
            this.name = name;
            this.phone = phone;
            this.accountNo = accountNo;
        }
    }
}