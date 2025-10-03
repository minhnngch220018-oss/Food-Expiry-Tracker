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
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_FOOD = "food";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_PURCHASE_DATE = "purchase_date";
    private static final String COLUMN_EXPIRY_DATE = "expiry_date";
    private static final String COLUMN_QUANTITY = "quantity";
    private static final String COLUMN_NOTES = "notes";

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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FOOD);
        onCreate(db);
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
}