package com.example.foodexpirytracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private FoodAdapter foodAdapter;
    private DatabaseHelper dbHelper;
    private List<Food> foodList;
    private boolean sortAscendingByTimeLeft = true;

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
        dbHelper = new DatabaseHelper(this);

        // Request notification permission on Android 13+
        ensureNotificationPermission();

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load food items from database
        loadFoodItems();

        // Set up FAB for adding new food items
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(view -> showAddFoodDialog());

        FloatingActionButton fabSort = findViewById(R.id.fabSort);
        fabSort.setOnClickListener(v -> toggleSort());
    }

    private void loadFoodItems() {
        foodList = dbHelper.getAllFood();
        foodAdapter = new FoodAdapter(this, foodList, dbHelper, recyclerView, findViewById(R.id.emptyStateView));
        recyclerView.setAdapter(foodAdapter);
        
        // Show empty state view if no items
        updateEmptyState();
        
        // Schedule reminders for existing items
        for (Food f : foodList) {
            scheduleExpiryReminder(f);
            scheduleExpiredAlert(f);
        }
    }
    
    public void updateEmptyState() {
        View emptyStateView = findViewById(R.id.emptyStateView);
        if (foodList.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddFoodDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_food, null);
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

            // Create food object
            Food food = new Food(0, name, category, purchaseDate, expiryDate, quantity, notes);

            // Save to database
            long id = dbHelper.addFood(food);
            if (id > 0) {
                food.setId((int) id);
                foodList.add(food);
                foodAdapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, R.string.food_added_success, Toast.LENGTH_SHORT).show();
                
                // Schedule one-day-before reminder
                scheduleExpiryReminder(food);
                
                // Schedule on-expiry alert
                scheduleExpiredAlert(food);
                
                dialog.dismiss();
            } else {
                Toast.makeText(MainActivity.this, R.string.food_add_failed, Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

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
    private void scheduleExpiryReminder(Food food) {
        try {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat(getString(R.string.date_format), java.util.Locale.getDefault());
            java.util.Date expiry = dateFormat.parse(food.getExpiryDate());
            if (expiry == null) return;
            long triggerTime = expiry.getTime() - java.util.concurrent.TimeUnit.DAYS.toMillis(1);
            long delay = triggerTime - System.currentTimeMillis();
            if (delay <= 0) {
                // If already within 1 day or past, optionally notify immediately
                com.example.foodexpirytracker.notifications.NotificationHelper.sendNotification(this,
                        "Food expiring soon",
                        food.getName() + " expires tomorrow (" + food.getExpiryDate() + ")");
                return;
            }
            Data input = new Data.Builder()
                    .putInt(ExpiryNotifierWorker.KEY_FOOD_ID, food.getId())
                    .putString(ExpiryNotifierWorker.KEY_FOOD_NAME, food.getName())
                    .putString(ExpiryNotifierWorker.KEY_EXPIRY_DATE, food.getExpiryDate())
                    .build();
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ExpiryNotifierWorker.class)
                    .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .setInputData(input)
                    .build();
            WorkManager.getInstance(getApplicationContext())
                    .enqueueUniqueWork("expiry_reminder_" + food.getId(), ExistingWorkPolicy.KEEP, request);
        } catch (java.text.ParseException e) {
            // ignore parse errors
        }
    }
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
    private void scheduleExpiredAlert(Food food) {
        try {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat(getString(R.string.date_format), java.util.Locale.getDefault());
            java.util.Date expiry = dateFormat.parse(food.getExpiryDate());
            if (expiry == null) return;
            long triggerTime = expiry.getTime();
            long delay = triggerTime - System.currentTimeMillis();
            if (delay <= 0) {
                // Already expired, notify immediately
                com.example.foodexpirytracker.notifications.NotificationHelper.sendNotification(this,
                        "Food expired",
                        food.getName() + " has expired (" + food.getExpiryDate() + ")");
                return;
            }
            Data input = new Data.Builder()
                    .putInt(ExpiredNotifierWorker.KEY_FOOD_ID, food.getId())
                    .putString(ExpiredNotifierWorker.KEY_FOOD_NAME, food.getName())
                    .putString(ExpiredNotifierWorker.KEY_EXPIRY_DATE, food.getExpiryDate())
                    .build();
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ExpiredNotifierWorker.class)
                    .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .setInputData(input)
                    .build();
            WorkManager.getInstance(getApplicationContext())
                    .enqueueUniqueWork("expired_alert_" + food.getId(), ExistingWorkPolicy.REPLACE, request);
        } catch (java.text.ParseException e) {
            // ignore parse errors
        }
    }
    private long timeLeftMillis(Food f, SimpleDateFormat dateFormat, long now) {
        try {
            java.util.Date expiry = dateFormat.parse(f.getExpiryDate());
            if (expiry == null) return Long.MAX_VALUE;
            return expiry.getTime() - now;
        } catch (java.text.ParseException e) {
            return Long.MAX_VALUE;
        }
    }

    private void applySortByTimeLeft() {
        long now = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.date_format), Locale.getDefault());
        Comparator<Food> comparator = (a, b) -> {
            long ta = timeLeftMillis(a, dateFormat, now);
            long tb = timeLeftMillis(b, dateFormat, now);
            return sortAscendingByTimeLeft ? Long.compare(ta, tb) : Long.compare(tb, ta);
        };
        Collections.sort(foodList, comparator);
        foodAdapter.notifyDataSetChanged();
    }

    private void toggleSort() {
        sortAscendingByTimeLeft = !sortAscendingByTimeLeft;
        applySortByTimeLeft();
        Toast.makeText(this,
                sortAscendingByTimeLeft ? R.string.sort_soonest_first : R.string.sort_furthest_first,
                Toast.LENGTH_SHORT).show();
    }
}