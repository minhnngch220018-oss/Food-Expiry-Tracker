package com.example.foodexpirytracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/*
 * Function: DatabaseHelper
 * Purpose: Provide CRUD operations for food items and handle secure user accounts
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "food_tracker.db";
    private static final int DATABASE_VERSION = 3;

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

    /*
     * Function: onCreate
     * Purpose: Create food and users tables on first database creation
     */
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

    /*
     * Function: onUpgrade
     * Purpose: Incremental migrations; adds users table for versions < 2 without data loss
     */
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

    /*
     * Function: onDowngrade
     * Purpose: Gracefully handle downgrades by recreating schema to avoid crashes
     */
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // If a downgrade happens (e.g., device has DB v3 but app declares v2),
        // default SQLiteOpenHelper throws. We avoid crash by recreating tables.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FOOD);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    /*
     * Function: addFood
     * Purpose: Insert a FoodItem into the database
     * Returns: row id (>0) or -1 on failure
     */
    public long addFood(FoodItem foodItem) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, foodItem.getName());
        values.put(COLUMN_CATEGORY, foodItem.getCategory());
        values.put(COLUMN_PURCHASE_DATE, foodItem.getPurchaseDate());
        values.put(COLUMN_EXPIRY_DATE, foodItem.getExpiryDate());
        values.put(COLUMN_QUANTITY, foodItem.getQuantity());
        values.put(COLUMN_NOTES, foodItem.getNotes());

        long id = db.insert(TABLE_FOOD, null, values);
        db.close();
        return id;
    }

    /*
     * Function: getAllFood
     * Purpose: Retrieve all stored FoodItem records
     * Returns: List<FoodItem>
     */
    public List<FoodItem> getAllFood() {
        List<FoodItem> foodItemList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_FOOD, null);

        if (cursor.moveToFirst()) {
            do {
                FoodItem foodItem = new FoodItem(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getInt(5),
                        cursor.getString(6)
                );
                foodItemList.add(foodItem);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return foodItemList;
    }

    /*
     * Function: deleteFood
     * Purpose: Remove a FoodItem by id
     */
    public void deleteFood(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FOOD, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    /*
     * Function: addUser
     * Purpose: Create a user with PBKDF2 hashed password and random salt
     * Returns: row id (>0) or -1 on failure
     */
    public long addUser(String email, String passwordPlain) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String salt = PasswordHelper.generateSalt();
        String hash = PasswordHelper.hashPassword(passwordPlain.toCharArray(), salt);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD_HASH, hash);
        values.put(COLUMN_SALT, salt);
        values.put(COLUMN_CREATED_AT, System.currentTimeMillis());
        long id = db.insert(TABLE_USERS, null, values);
        db.close();
        return id; // returns -1 on constraint violation (e.g., duplicate email)
    }

    /*
     * Function: userExists
     * Purpose: Determine if a user is present by email
     * Returns: true if exists
     */
    public boolean userExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_ID}, COLUMN_EMAIL + "=?", new String[]{email}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    /*
     * Function: verifyUser
     * Purpose: Validate plaintext password against stored hash for email
     * Returns: true if credentials match
     */
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
            valid = PasswordHelper.verifyPassword(passwordPlain.toCharArray(), salt, hash);
        }
        cursor.close();
        db.close();
        return valid;
    }

    /*
     * Function: updateUserPassword
     * Purpose: Update password (hash+salt) for a given email
     * Returns: true if at least one row updated
     */
    public boolean updateUserPassword(String email, String newPasswordPlain) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String newSalt = PasswordHelper.generateSalt();
        String newHash = PasswordHelper.hashPassword(newPasswordPlain.toCharArray(), newSalt);
        values.put(COLUMN_PASSWORD_HASH, newHash);
        values.put(COLUMN_SALT, newSalt);
        int rows = db.update(TABLE_USERS, values, COLUMN_EMAIL + "=?", new String[]{email});
        db.close();
        return rows > 0;
    }

    /*
     * Function: clearAllData
     * Purpose: Wipe all rows from app tables and reset autoincrement counters
     */
    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete(TABLE_FOOD, null, null);
            db.delete(TABLE_USERS, null, null);
            // Reset autoincrement counters (if present)
            db.execSQL("DELETE FROM sqlite_sequence WHERE name=?", new Object[]{TABLE_FOOD});
            db.execSQL("DELETE FROM sqlite_sequence WHERE name=?", new Object[]{TABLE_USERS});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }
}