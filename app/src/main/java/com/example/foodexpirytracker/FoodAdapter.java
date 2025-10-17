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

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {
    private Context context;
    private List<Food> foodList;
    private DatabaseHelper dbHelper;

    public FoodAdapter(Context context, List<Food> foodList, DatabaseHelper dbHelper, RecyclerView recyclerView, View emptyStateView) {
        this.context = context;
        this.foodList = foodList;
        this.dbHelper = dbHelper;
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_food, parent, false);
        return new FoodViewHolder(view);
    }

    @SuppressLint("StringFormatMatches")
    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        Food food = foodList.get(position);
        holder.tvFoodName.setText(food.getName());
        holder.tvExpiryDate.setText(context.getString(R.string.expiry_date_value, food.getExpiryDate()));
        holder.tvQuantity.setText(context.getString(R.string.quantity_value, food.getQuantity()));
        
        // Set food category icon
        setFoodCategoryIcon(holder.ivFoodIcon, food.getCategory());
        
        // Set expiry indicator color
        setExpiryIndicator(holder.expiryIndicator, food.getExpiryDate());
        
        // Set long click listener for deleting items
        holder.itemView.setOnLongClickListener(v -> {
            showDeleteDialog(food, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    private void showDeleteDialog(Food food, int position) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setTitle(R.string.delete_food_item);
        builder.setMessage(context.getString(R.string.delete_confirmation, food.getName()));
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            // Cancel any scheduled one-day-before reminder for this item
            WorkManager.getInstance(context.getApplicationContext())
                    .cancelUniqueWork("expiry_reminder_" + food.getId());
            // Cancel any scheduled expiry-day alert for this item
            WorkManager.getInstance(context.getApplicationContext())
                    .cancelUniqueWork("expired_alert_" + food.getId());
            
            dbHelper.deleteFood(food.getId());
            foodList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, foodList.size());
            android.widget.Toast.makeText(context, R.string.item_deleted, android.widget.Toast.LENGTH_SHORT).show();
            
            // Update empty state view
            if (context instanceof MainActivity) {
                ((MainActivity) context).updateEmptyState();
            }
        });
        builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

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
    
    public static class FoodViewHolder extends RecyclerView.ViewHolder {
        TextView tvFoodName, tvExpiryDate, tvQuantity;
        ImageView ivFoodIcon;
        View expiryIndicator;

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

