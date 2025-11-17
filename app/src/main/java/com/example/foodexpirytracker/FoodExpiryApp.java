package com.example.foodexpirytracker;

import android.app.Application;

import com.example.foodexpirytracker.notifications.NotificationHelper;

/*
 * Function: FoodExpiryApp
 * Purpose: Initialize global app state such as notification channels
 */
public class FoodExpiryApp extends Application {
    /*
     * Function: onCreate
     * Purpose: Create notification channel used by expiry alerts
     */
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannel(this);
    }
}