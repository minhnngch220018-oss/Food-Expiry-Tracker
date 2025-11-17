package com.example.foodexpirytracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.os.Build;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingWorkPolicy;
import com.example.foodexpirytracker.notifications.ExpiryNotifierWorker;
import com.example.foodexpirytracker.notifications.ExpiredNotifierWorker;
import androidx.appcompat.widget.SearchView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/*
 * Function: MainActivity
 * Purpose: Manage list of food items, add/edit, search, sort, and schedule notifications
 */
public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private FoodListAdapter foodListAdapter;
    private DatabaseHelper dbHelper;
    private List<FoodItem> foodItemList;
    private boolean sortAscendingByTimeLeft = true;
    private String currentQuery = "";

    /*
     * Function: onCreate
     * Purpose: Initialize UI, DB, listeners; request permissions; load data
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize database helper
        try {
            dbHelper = new DatabaseHelper(this);
        } catch (Exception e) {
            Log.e("MainActivity", "DatabaseHelper initialization failed", e);
            Toast.makeText(this, "DB init failed: " + (e.getMessage() == null ? e.toString() : e.getMessage()), Toast.LENGTH_LONG).show();
            return;
        }

        // Request notification permission on Android 13+
        try {
            ensureNotificationPermission();
        } catch (Exception e) {
            Log.e("MainActivity", "ensureNotificationPermission crashed", e);
            Toast.makeText(this, "Notification permission error: " + (e.getMessage() == null ? e.toString() : e.getMessage()), Toast.LENGTH_SHORT).show();
        }

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        if (recyclerView == null) {
            Log.e("MainActivity", "RecyclerView not found in layout (R.id.recyclerView)");
            Toast.makeText(this, "UI error: RecyclerView missing in layout", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        } catch (Exception e) {
            Log.e("MainActivity", "RecyclerView setup failed", e);
            Toast.makeText(this, "RecyclerView setup failed: " + (e.getMessage() == null ? e.toString() : e.getMessage()), Toast.LENGTH_LONG).show();
            return;
        }

        // Load food items from database
        try {
            loadFoodItems();
        } catch (Exception e) {
            Log.e("MainActivity", "loadFoodItems crashed", e);
            Toast.makeText(this, "Failed to load items: " + (e.getMessage() == null ? e.toString() : e.getMessage()), Toast.LENGTH_LONG).show();
        }

        // Optional: handle intent extra to clear database for maintenance
        try {
            boolean clearDb = getIntent() != null && getIntent().getBooleanExtra("clearDb", false);
            if (clearDb) {
                dbHelper.clearAllData();
                Toast.makeText(this, "Database cleared", Toast.LENGTH_SHORT).show();
                // Refresh UI state
                applyFilter("");
            }
        } catch (Exception e) {
            Log.e("MainActivity", "clearDb extra handling failed", e);
            Toast.makeText(this, "Failed to clear DB: " + (e.getMessage() == null ? e.toString() : e.getMessage()), Toast.LENGTH_LONG).show();
        }

        // Set up FAB for adding new food items
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(view -> showAddFoodDialog());

        FloatingActionButton fabSort = findViewById(R.id.fabSort);
        fabSort.setOnClickListener(v -> toggleSort());

        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                applyFilter(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                applyFilter(newText);
                return true;
            }
        });
    }

    /*
     * Function: loadFoodItems
     * Purpose: Fetch items from DB, set adapter, update empty state, and schedule reminders
     */
    private void loadFoodItems() {
        foodItemList = dbHelper.getAllFood();
        foodListAdapter = new FoodListAdapter(this, foodItemList, dbHelper);
        recyclerView.setAdapter(foodListAdapter);
        
        // Show empty state view if no items
        updateEmptyState();
        
        // Schedule reminders for existing items
        for (FoodItem f : foodItemList) {
            scheduleExpiryReminder(f);
            scheduleExpiredAlert(f);
        }
    }
    
    /*
     * Function: updateEmptyState
     * Purpose: Toggle empty-state visibility based on current list content
     */
    public void updateEmptyState() {
        View emptyStateView = findViewById(R.id.emptyStateView);
        if (foodItemList.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /*
     * Function: showAddFoodDialog
     * Purpose: Collect new item details, validate, persist, and schedule notifications
     */
    private void showAddFoodDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_food_add, null);
        builder.setView(dialogView);

        final EditText etFoodName = dialogView.findViewById(R.id.etFoodName);
        final EditText etCategory = dialogView.findViewById(R.id.etCategory);
        final EditText etPurchaseDate = dialogView.findViewById(R.id.etPurchaseDate);
        final EditText etExpiryDate = dialogView.findViewById(R.id.etExpiryDate);
        final EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        final EditText etNotes = dialogView.findViewById(R.id.etNotes);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        // Set up date pickers
        setupDatePicker(etPurchaseDate);
        setupDatePicker(etExpiryDate);

        final AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            // Validate input
            if (etFoodName.getText().toString().trim().isEmpty()) {
                Toast.makeText(MainActivity.this, R.string.error_empty_food_name, Toast.LENGTH_SHORT).show();
                return;
            }

            // Get values
            String name = etFoodName.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String purchaseDate = etPurchaseDate.getText().toString().trim();
            String expiryDate = etExpiryDate.getText().toString().trim();
            int quantity = 1;
            try {
                quantity = Integer.parseInt(etQuantity.getText().toString().trim());
            } catch (NumberFormatException e) {
                // Use default value 1
            }
            String notes = etNotes.getText().toString().trim();

            // Create foodItem object
            FoodItem foodItem = new FoodItem(0, name, category, purchaseDate, expiryDate, quantity, notes);

            // Save to database
            long id = dbHelper.addFood(foodItem);
            if (id > 0) {
                foodItem.setId((int) id);
                applyFilter(currentQuery);
                Toast.makeText(MainActivity.this, R.string.food_added_success, Toast.LENGTH_SHORT).show();
                
                // Schedule one-day-before reminder
                scheduleExpiryReminder(foodItem);
                
                // Schedule on-expiry alert
                scheduleExpiredAlert(foodItem);
                
                dialog.dismiss();
            } else {
                Toast.makeText(MainActivity.this, R.string.food_add_failed, Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    /*
     * Function: setupDatePicker
     * Purpose: Attach date picker to an EditText and format selected date
     */
    private void setupDatePicker(final EditText editText) {
        editText.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        calendar.set(Calendar.YEAR, selectedYear);
                        calendar.set(Calendar.MONTH, selectedMonth);
                        calendar.set(Calendar.DAY_OF_MONTH, selectedDay);

                        SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.date_format), Locale.getDefault());
                        editText.setText(dateFormat.format(calendar.getTime()));
                    }, year, month, day);
            datePickerDialog.show();
        });
    }
    /*
     * Function: scheduleExpiryReminder
     * Purpose: Schedule a notification one day before item expiry
     */
    private void scheduleExpiryReminder(FoodItem foodItem) {
        try {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat(getString(R.string.date_format), java.util.Locale.getDefault());
            java.util.Date expiry = dateFormat.parse(foodItem.getExpiryDate());
            if (expiry == null) return;
            long triggerTime = expiry.getTime() - java.util.concurrent.TimeUnit.DAYS.toMillis(1);
            long delay = triggerTime - System.currentTimeMillis();
            if (delay <= 0) {
                // If already within 1 day or past, optionally notify immediately
                com.example.foodexpirytracker.notifications.NotificationHelper.sendNotification(this,
                        "FoodItem expiring soon",
                        foodItem.getName() + " expires tomorrow (" + foodItem.getExpiryDate() + ")");
                return;
            }
            Data input = new Data.Builder()
                    .putInt(ExpiryNotifierWorker.KEY_FOOD_ID, foodItem.getId())
                    .putString(ExpiryNotifierWorker.KEY_FOOD_NAME, foodItem.getName())
                    .putString(ExpiryNotifierWorker.KEY_EXPIRY_DATE, foodItem.getExpiryDate())
                    .build();
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ExpiryNotifierWorker.class)
                    .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .setInputData(input)
                    .build();
            WorkManager.getInstance(getApplicationContext())
                    .enqueueUniqueWork("expiry_reminder_" + foodItem.getId(), ExistingWorkPolicy.KEEP, request);
        } catch (java.text.ParseException e) {
            // ignore parse errors
        }
    }
    /*
     * Function: ensureNotificationPermission
     * Purpose: Request POST_NOTIFICATIONS permission on Android 13+
     */
    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001);
            }
        }
    }
    /*
     * Function: scheduleExpiredAlert
     * Purpose: Schedule a notification at the exact expiry time
     */
    private void scheduleExpiredAlert(FoodItem foodItem) {
        try {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat(getString(R.string.date_format), java.util.Locale.getDefault());
            java.util.Date expiry = dateFormat.parse(foodItem.getExpiryDate());
            if (expiry == null) return;
            long triggerTime = expiry.getTime();
            long delay = triggerTime - System.currentTimeMillis();
            if (delay <= 0) {
                // Already expired, notify immediately
                com.example.foodexpirytracker.notifications.NotificationHelper.sendNotification(this,
                        "FoodItem expired",
                        foodItem.getName() + " has expired (" + foodItem.getExpiryDate() + ")");
                return;
            }
            Data input = new Data.Builder()
                    .putInt(ExpiredNotifierWorker.KEY_FOOD_ID, foodItem.getId())
                    .putString(ExpiredNotifierWorker.KEY_FOOD_NAME, foodItem.getName())
                    .putString(ExpiredNotifierWorker.KEY_EXPIRY_DATE, foodItem.getExpiryDate())
                    .build();
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ExpiredNotifierWorker.class)
                    .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .setInputData(input)
                    .build();
            WorkManager.getInstance(getApplicationContext())
                    .enqueueUniqueWork("expired_alert_" + foodItem.getId(), ExistingWorkPolicy.REPLACE, request);
        } catch (java.text.ParseException e) {
            // ignore parse errors
        }
    }
    /*
     * Function: timeLeftMillis
     * Purpose: Compute milliseconds remaining until expiry; Long.MAX_VALUE on parse errors
     */
    private long timeLeftMillis(FoodItem f, SimpleDateFormat dateFormat, long now) {
        try {
            java.util.Date expiry = dateFormat.parse(f.getExpiryDate());
            if (expiry == null) return Long.MAX_VALUE;
            return expiry.getTime() - now;
        } catch (java.text.ParseException e) {
            return Long.MAX_VALUE;
        }
    }

    /*
     * Function: applySortByTimeLeft
     * Purpose: Sort list by time left until expiry in chosen order
     */
    private void applySortByTimeLeft() {
        long now = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.date_format), Locale.getDefault());
        Comparator<FoodItem> comparator = (a, b) -> {
            String ca = a.getCategory() == null ? "" : a.getCategory().toLowerCase(Locale.getDefault());
            String cb = b.getCategory() == null ? "" : b.getCategory().toLowerCase(Locale.getDefault());
            int catCmp = ca.compareTo(cb);
            if (catCmp != 0) return catCmp;

            long ta = timeLeftMillis(a, dateFormat, now);
            long tb = timeLeftMillis(b, dateFormat, now);
            return sortAscendingByTimeLeft ? Long.compare(ta, tb) : Long.compare(tb, ta);
        };
        Collections.sort(foodItemList, comparator);
        foodListAdapter.notifyDataSetChanged();
    }

    /*
     * Function: toggleSort
     * Purpose: Flip sort order and reapply sorting; show status toast
     */
    private void toggleSort() {
        sortAscendingByTimeLeft = !sortAscendingByTimeLeft;
        applySortByTimeLeft();
        Toast.makeText(this,
                sortAscendingByTimeLeft ? R.string.sort_soonest_first : R.string.sort_furthest_first,
                Toast.LENGTH_SHORT).show();
    }

    /*
     * Function: applyFilter
     * Purpose: Filter items by name substring, resort, and refresh empty state
     */
    private void applyFilter(String query) {
        currentQuery = (query == null) ? "" : query;
        String q = currentQuery.trim().toLowerCase(Locale.getDefault());
        List<FoodItem> allItems = dbHelper.getAllFood();
        foodItemList.clear();
        if (q.isEmpty()) {
            foodItemList.addAll(allItems);
        } else {
            for (FoodItem item : allItems) {
                String name = item.getName() == null ? "" : item.getName().toLowerCase(Locale.getDefault());
                if (name.contains(q)) {
                    foodItemList.add(item);
                }
            }
        }
        applySortByTimeLeft();
        updateEmptyState();
    }

}