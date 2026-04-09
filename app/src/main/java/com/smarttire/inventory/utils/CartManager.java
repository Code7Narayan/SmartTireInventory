package com.smarttire.inventory.utils;

import com.smarttire.inventory.models.Product;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartManager {
    private static CartManager instance;
    private final Map<Integer, Product> cartItems = new HashMap<>();

    private CartManager() {}

    public static synchronized CartManager getInstance() {
        if (instance == null) instance = new CartManager();
        return instance;
    }

    public void addItem(Product product) {
        if (cartItems.containsKey(product.getId())) {
            Product p = cartItems.get(product.getId());
            p.setCartQuantity(p.getCartQuantity() + 1);
        } else {
            product.setCartQuantity(1);
            cartItems.put(product.getId(), product);
        }
    }

    public void removeItem(int productId) {
        cartItems.remove(productId);
    }

    public List<Product> getItems() {
        return new ArrayList<>(cartItems.values());
    }

    public void clear() {
        cartItems.clear();
    }

    public int getCount() {
        int count = 0;
        for (Product p : cartItems.values()) {
            count += p.getCartQuantity();
        }
        return count;
    }
    
    public double getTotal() {
        double total = 0;
        for (Product p : cartItems.values()) {
            total += p.getPrice() * p.getCartQuantity();
        }
        return total;
    }
}