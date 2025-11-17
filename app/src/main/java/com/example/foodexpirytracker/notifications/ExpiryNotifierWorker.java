package com.example.foodexpirytracker.notifications;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/*
 * Function: ExpiryNotifierWorker
 * Purpose: Deliver a notification one day before a food item expires
 */
public class ExpiryNotifierWorker extends Worker {
    public static final String KEY_FOOD_ID = "food_id";
    public static final String KEY_FOOD_NAME = "food_name";
    public static final String KEY_EXPIRY_DATE = "expiry_date";

    /*
     * Function: constructor
     * Purpose: Initialize worker with app context and parameters
     */
    public ExpiryNotifierWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    /*
     * Function: doWork
     * Purpose: Build and send the "expiring tomorrow" notification
     * Returns: Result.success or failure on missing input
     */
    public Result doWork() {
        String foodName = getInputData().getString(KEY_FOOD_NAME);
        String expiryDate = getInputData().getString(KEY_EXPIRY_DATE);

        if (foodName == null) {
            return Result.failure(new Data.Builder()
                    .putString("error", "Missing food_name")
                    .build());
        }

        String title = "FoodItem expiring tomorrow";
        String message = foodName + " expires tomorrow (" + (expiryDate != null ? expiryDate : "") + ")";
        NotificationHelper.sendNotification(getApplicationContext(), title, message);

        return Result.success();
    }
}