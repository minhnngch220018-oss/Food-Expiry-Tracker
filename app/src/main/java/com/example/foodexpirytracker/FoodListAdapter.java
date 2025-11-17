package com.example.foodexpirytracker;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/*
 * Function: FoodListAdapter
 * Purpose: Bind FoodItem data to RecyclerView list cards and handle item interactions
 */
public class FoodListAdapter extends RecyclerView.Adapter<FoodListAdapter.FoodViewHolder> {
    private Context context;
    private List<FoodItem> foodItemList;
    private DatabaseHelper dbHelper;

    /*
     * Function: FoodListAdapter constructor
     * Purpose: Initialize adapter with context, data list, and database helper
     * Params: context - activity context; foodItemList - items to display; dbHelper - DB operations
     */
    public FoodListAdapter(Context context, List<FoodItem> foodItemList, DatabaseHelper dbHelper) {
        this.context = context;
        this.foodItemList = foodItemList;
        this.dbHelper = dbHelper;
    }

    /*
     * Function: onCreateViewHolder
     * Purpose: Inflate item view and create a ViewHolder instance
     * Returns: FoodViewHolder
     */
    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_food, parent, false);
        return new FoodViewHolder(view);
    }

    /*
     * Function: onBindViewHolder
     * Purpose: Bind FoodItem values to views and wire interactions
     * Params: holder - view holder; position - item index
     */
    @SuppressLint("StringFormatMatches")
    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        FoodItem foodItem = foodItemList.get(position);
        holder.tvFoodName.setText(foodItem.getName());
        holder.tvExpiryDate.setText(context.getString(R.string.expiry_date_value, foodItem.getExpiryDate()));
        holder.tvQuantity.setText(context.getString(R.string.quantity_value, foodItem.getQuantity()));
        
        // Set foodItem category icon
        setFoodCategoryIcon(holder.ivFoodIcon, foodItem.getCategory());
        
        // Set expiry indicator color
        setExpiryIndicator(holder.expiryIndicator, foodItem.getExpiryDate());
        
        // Set long click listener for deleting items
        holder.itemView.setOnLongClickListener(v -> {
            showDeleteDialog(foodItem, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return foodItemList.size();
    }

    /*
     * Function: showDeleteDialog
     * Purpose: Confirm and perform deletion of a food item, cancel related reminders
     */
    private void showDeleteDialog(FoodItem foodItem, int position) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setTitle(R.string.delete_food_item);
        builder.setMessage(context.getString(R.string.delete_confirmation, foodItem.getName()));
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            // Cancel any scheduled one-day-before reminder for this item
            WorkManager.getInstance(context.getApplicationContext())
                    .cancelUniqueWork("expiry_reminder_" + foodItem.getId());
            // Cancel any scheduled expiry-day alert for this item
            WorkManager.getInstance(context.getApplicationContext())
                    .cancelUniqueWork("expired_alert_" + foodItem.getId());
            
            dbHelper.deleteFood(foodItem.getId());
            foodItemList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, foodItemList.size());
            android.widget.Toast.makeText(context, R.string.item_deleted, android.widget.Toast.LENGTH_SHORT).show();
            
            // Update empty state view
            if (context instanceof MainActivity) {
                ((MainActivity) context).updateEmptyState();
            }
        });
        builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    /*
     * Function: setFoodCategoryIcon
     * Purpose: Choose an icon based on category keywords
     */
    private void setFoodCategoryIcon(ImageView imageView, String category) {
        // Set icon based on category
        if (category == null || category.isEmpty()) {
            return;
        }
        
        category = category.toLowerCase();
        
        if (category.contains("fruit") || category.contains("apple") || 
            category.contains("banana") || category.contains("orange")) {
            imageView.setImageResource(android.R.drawable.ic_menu_compass);
        } else if (category.contains("vegetable") || category.contains("veg")) {
            imageView.setImageResource(android.R.drawable.ic_menu_crop);
        } else if (category.contains("meat") || category.contains("chicken") || 
                 category.contains("beef") || category.contains("pork")) {
            imageView.setImageResource(android.R.drawable.ic_menu_view);
        } else if (category.contains("dairy") || category.contains("milk") || 
                 category.contains("cheese") || category.contains("yogurt")) {
            imageView.setImageResource(android.R.drawable.ic_menu_slideshow);
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }
    
    /*
     * Function: setExpiryIndicator
     * Purpose: Color indicator based on days until expiry
     */
    private void setExpiryIndicator(View indicator, String expiryDateStr) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault());
            Date expiryDate = dateFormat.parse(expiryDateStr);
            Date currentDate = Calendar.getInstance().getTime();
            
            if (expiryDate != null) {
                long diffInMillies = expiryDate.getTime() - currentDate.getTime();
                long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
                
                GradientDrawable shape = (GradientDrawable) indicator.getBackground();
                
                if (diffInDays < 0) {
                    // Expired
                    shape.setColor(Color.RED);
                } else if (diffInDays <= 3) {
                    // Expiring soon (3 days or less)
                    shape.setColor(ContextCompat.getColor(context, R.color.orange_warning));
                } else if (diffInDays <= 7) {
                    // Expiring within a week
                    shape.setColor(Color.YELLOW);
                } else {
                    // Not expiring soon
                    shape.setColor(Color.GREEN);
                }
            }
        } catch (ParseException e) {
            // Default to green if date parsing fails
            GradientDrawable shape = (GradientDrawable) indicator.getBackground();
            shape.setColor(Color.GRAY);
        }
    }
    
    /*
     * Function: FoodViewHolder
     * Purpose: Cache view references for item_food layout
     */
    public static class FoodViewHolder extends RecyclerView.ViewHolder {
        TextView tvFoodName, tvExpiryDate, tvQuantity;
        ImageView ivFoodIcon;
        View expiryIndicator;

        /*
         * Function: FoodViewHolder constructor
         * Purpose: Bind child views from itemView
         */
        public FoodViewHolder(View itemView) {
            super(itemView);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvExpiryDate = itemView.findViewById(R.id.tvExpiryDate);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            ivFoodIcon = itemView.findViewById(R.id.ivFoodIcon);
            expiryIndicator = itemView.findViewById(R.id.expiryIndicator);
        }
    }
}

