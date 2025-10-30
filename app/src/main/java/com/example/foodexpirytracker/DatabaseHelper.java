package com.example.foodexpirytracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "food_tracker.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_FOOD = "food";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_PURCHASE_DATE = "purchase_date";
    private static final String COLUMN_EXPIRY_DATE = "expiry_date";
    private static final String COLUMN_QUANTITY = "quantity";
    private static final String COLUMN_NOTES = "notes";

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD_HASH = "password_hash";
    private static final String COLUMN_SALT = "salt";
    private static final String COLUMN_CREATED_AT = "created_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_FOOD + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_CATEGORY + " TEXT,"
                + COLUMN_PURCHASE_DATE + " TEXT,"
                + COLUMN_EXPIRY_DATE + " TEXT,"
                + COLUMN_QUANTITY + " INTEGER,"
                + COLUMN_NOTES + " TEXT"
                + ")";
        db.execSQL(CREATE_TABLE);

        String CREATE_USERS = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_EMAIL + " TEXT UNIQUE NOT NULL,"
                + COLUMN_PASSWORD_HASH + " TEXT NOT NULL,"
                + COLUMN_SALT + " TEXT NOT NULL,"
                + COLUMN_CREATED_AT + " INTEGER"
                + ")";
        db.execSQL(CREATE_USERS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Migrate without dropping existing data
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_EMAIL + " TEXT UNIQUE NOT NULL,"
                    + COLUMN_PASSWORD_HASH + " TEXT NOT NULL,"
                    + COLUMN_SALT + " TEXT NOT NULL,"
                    + COLUMN_CREATED_AT + " INTEGER"
                    + ")");
        }
    }

    // Insert
    public long addFood(Food food) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, food.getName());
        values.put(COLUMN_CATEGORY, food.getCategory());
        values.put(COLUMN_PURCHASE_DATE, food.getPurchaseDate());
        values.put(COLUMN_EXPIRY_DATE, food.getExpiryDate());
        values.put(COLUMN_QUANTITY, food.getQuantity());
        values.put(COLUMN_NOTES, food.getNotes());

        long id = db.insert(TABLE_FOOD, null, values);
        db.close();
        return id;
    }

    // Get All
    public List<Food> getAllFood() {
        List<Food> foodList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_FOOD, null);

        if (cursor.moveToFirst()) {
            do {
                Food food = new Food(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getInt(5),
                        cursor.getString(6)
                );
                foodList.add(food);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return foodList;
    }

    // Delete
    public void deleteFood(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FOOD, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Users: add user with secure password hashing
    public long addUser(String email, String passwordPlain) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hashPassword(passwordPlain.toCharArray(), salt);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD_HASH, hash);
        values.put(COLUMN_SALT, salt);
        values.put(COLUMN_CREATED_AT, System.currentTimeMillis());
        long id = db.insert(TABLE_USERS, null, values);
        db.close();
        return id; // returns -1 on constraint violation (e.g., duplicate email)
    }

    // Users: check if a user exists by email
    public boolean userExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_ID}, COLUMN_EMAIL + "=?", new String[]{email}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    // Users: verify email + password
    public boolean verifyUser(String email, String passwordPlain) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_PASSWORD_HASH, COLUMN_SALT},
                COLUMN_EMAIL + "=?",
                new String[]{email},
                null, null, null);
        boolean valid = false;
        if (cursor.moveToFirst()) {
            String hash = cursor.getString(0);
            String salt = cursor.getString(1);
            valid = PasswordUtils.verifyPassword(passwordPlain.toCharArray(), salt, hash);
        }
        cursor.close();
        db.close();
        return valid;
    }
}