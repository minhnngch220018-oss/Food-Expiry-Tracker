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
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private FoodAdapter foodAdapter;
    private DatabaseHelper dbHelper;
    private List<Food> foodList;

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

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load food items from database
        loadFoodItems();

        // Set up FAB for adding new food items
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(view -> showAddFoodDialog());
    }

    private void loadFoodItems() {
        foodList = dbHelper.getAllFood();
        foodAdapter = new FoodAdapter(this, foodList, dbHelper, recyclerView, findViewById(R.id.emptyStateView));
        recyclerView.setAdapter(foodAdapter);
        
        // Show empty state view if no items
        updateEmptyState();
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
}