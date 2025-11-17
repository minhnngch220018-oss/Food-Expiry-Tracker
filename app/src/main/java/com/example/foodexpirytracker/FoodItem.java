package com.example.foodexpirytracker;

/*
 * Function: FoodItem
 * Purpose: Data model representing a tracked food item
 */
public class FoodItem {
    private int id;
    private String name;
    private String category;
    private String purchaseDate;
    private String expiryDate;
    private int quantity;
    private String notes;

    /*
     * Function: FoodItem constructor
     * Purpose: Initialize all fields for a food item
     */
    public FoodItem(int id, String name, String category, String purchaseDate, String expiryDate, int quantity, String notes) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.purchaseDate = purchaseDate;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
        this.notes = notes;
    }

    // Getters & Setters
    /* Function: getId | Purpose: Return item id */
    public int getId() { return id; }
    /* Function: getName | Purpose: Return item name */
    public String getName() { return name; }
    /* Function: getCategory | Purpose: Return item category */
    public String getCategory() { return category; }
    /* Function: getPurchaseDate | Purpose: Return purchase date */
    public String getPurchaseDate() { return purchaseDate; }
    /* Function: getExpiryDate | Purpose: Return expiry date */
    public String getExpiryDate() { return expiryDate; }
    /* Function: getQuantity | Purpose: Return quantity */
    public int getQuantity() { return quantity; }
    /* Function: getNotes | Purpose: Return notes */
    public String getNotes() { return notes; }

    /* Function: setId | Purpose: Set item id */
    public void setId(int id) { this.id = id; }
    /* Function: setName | Purpose: Set item name */
    public void setName(String name) { this.name = name; }
    /* Function: setCategory | Purpose: Set item category */
    public void setCategory(String category) { this.category = category; }
    /* Function: setPurchaseDate | Purpose: Set purchase date */
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }
    /* Function: setExpiryDate | Purpose: Set expiry date */
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    /* Function: setQuantity | Purpose: Set quantity */
    public void setQuantity(int quantity) { this.quantity = quantity; }
    /* Function: setNotes | Purpose: Set notes */
    public void setNotes(String notes) { this.notes = notes; }
}
