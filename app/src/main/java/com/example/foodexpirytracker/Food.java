package com.example.foodexpirytracker;

public class Food {
    private int id;
    private String name;
    private String category;
    private String purchaseDate;
    private String expiryDate;
    private int quantity;
    private String notes;

    public Food(int id, String name, String category, String purchaseDate, String expiryDate, int quantity, String notes) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.purchaseDate = purchaseDate;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
        this.notes = notes;
    }

    // Getters & Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getPurchaseDate() { return purchaseDate; }
    public String getExpiryDate() { return expiryDate; }
    public int getQuantity() { return quantity; }
    public String getNotes() { return notes; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setNotes(String notes) { this.notes = notes; }
}
