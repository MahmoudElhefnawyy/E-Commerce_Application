package com.mycompany.e.commerce_application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class EmptyCartException extends Exception {
    public EmptyCartException(String message) {
        super(message);
    }
}

class InsufficientBalanceException extends Exception {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}

class OutOfStockException extends Exception {
    public OutOfStockException(String message) {
        super(message);
    }
}

class ExpiredProductException extends Exception {
    public ExpiredProductException(String message) {
        super(message);
    }
}

interface Shippable {
    String getName();
    double getWeight();
}

abstract class Product {
    protected String name;
    protected double price;
    protected int quantity;

    public Product(String name, double price, int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void reduceQuantity(int amount) throws OutOfStockException {
        if (quantity < amount) {
            throw new OutOfStockException(name + " is out of stock. Available: " + quantity);
        }
        quantity -= amount;
    }

    public abstract void checkAvailability(int requestedQuantity) throws OutOfStockException, ExpiredProductException;
}

class PerishableProduct extends Product implements Shippable {
    private LocalDate expirationDate;
    private double weight;

    public PerishableProduct(String name, double price, int quantity, LocalDate expirationDate, double weight) {
        super(name, price, quantity);
        this.expirationDate = expirationDate;
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }

    public void checkAvailability(int requestedQuantity) throws OutOfStockException, ExpiredProductException {
        if (quantity < requestedQuantity) {
            throw new OutOfStockException(name + " is out of stock. Available: " + quantity);
        }
        if (expirationDate.isBefore(LocalDate.now())) {
            throw new ExpiredProductException(name + " is expired.");
        }
    }
}

class NonPerishableProduct extends Product {
    private double weight;
    private boolean isShippable;

    public NonPerishableProduct(String name, double price, int quantity, boolean isShippable, double weight) {
        super(name, price, quantity);
        this.isShippable = isShippable;
        this.weight = isShippable ? weight : 0;
    }

    public void checkAvailability(int requestedQuantity) throws OutOfStockException {
        if (quantity < requestedQuantity) {
            throw new OutOfStockException(name + " is out of stock. Available: " + quantity);
        }
    }

    public boolean isShippable() {
        return isShippable;
    }

    public double getWeight() {
        return weight;
    }
}

class Customer {
    private double balance;

    public Customer(double balance) {
        this.balance = balance;
    }

    public double getBalance() {
        return balance;
    }

    public void deductBalance(double amount) throws InsufficientBalanceException {
        if (balance < amount) {
            throw new InsufficientBalanceException("Insufficient balance. Available: " + balance);
        }
        balance -= amount;
    }
}

class Cart {
    private Map<Product, Integer> items = new HashMap<>();

    public void add(Product product, int quantity) throws OutOfStockException, ExpiredProductException {
        product.checkAvailability(quantity);
        items.merge(product, quantity, Integer::sum);
    }

    public Map<Product, Integer> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}

class ShippingService {
    public void ship(List<ShippableItem> items) {
        System.out.println("** Shipment notice **");
        double totalWeight = 0;
        for (ShippableItem item : items) {
            System.out.println(item.quantity + "x " + item.shippable.getName() + " " + (item.quantity * item.shippable.getWeight() * 1000) + "g");
            totalWeight += item.quantity * item.shippable.getWeight();
        }
        System.out.println("Total package weight " + totalWeight + "kg");
    }
}

class ShippableItem {
    Shippable shippable;
    int quantity;

    public ShippableItem(Shippable shippable, int quantity) {
        this.shippable = shippable;
        this.quantity = quantity;
    }
}

class CheckoutService {
    private static final double SHIPPING_FEE_PER_KG = 30.0 / 1.1;

    public void checkout(Customer customer, Cart cart) throws EmptyCartException, InsufficientBalanceException, OutOfStockException, ExpiredProductException {
        if (cart.isEmpty()) {
            throw new EmptyCartException("Cart is empty.");
        }

        double subtotal = 0;
        List<ShippableItem> shippableItems = new ArrayList<>();
        for (Map.Entry<Product, Integer> entry : cart.getItems().entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            product.checkAvailability(quantity);
            subtotal += product.getPrice() * quantity;
            if (product instanceof Shippable) {
                shippableItems.add(new ShippableItem((Shippable) product, quantity));
            } else if (product instanceof NonPerishableProduct && ((NonPerishableProduct) product).isShippable()) {
                shippableItems.add(new ShippableItem((Shippable) product, quantity));
            }
        }

        double totalWeight = shippableItems.stream().mapToDouble(item -> item.shippable.getWeight() * item.quantity).sum();
        double shippingFees = totalWeight * SHIPPING_FEE_PER_KG;
        double totalAmount = subtotal + shippingFees;

        customer.deductBalance(totalAmount);

        for (Map.Entry<Product, Integer> entry : cart.getItems().entrySet()) {
            entry.getKey().reduceQuantity(entry.getValue());
        }

        if (!shippableItems.isEmpty()) {
            new ShippingService().ship(shippableItems);
        }

        System.out.println("** Checkout receipt **");
        for (Map.Entry<Product, Integer> entry : cart.getItems().entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            System.out.println(quantity + "x " + product.getName() + " " + (product.getPrice() * quantity));
        }
        System.out.println("----------------------");
        System.out.println("Subtotal " + subtotal);
        System.out.println("Shipping " + shippingFees);
        System.out.println("Amount " + totalAmount);
        System.out.println("Customer balance after payment: " + customer.getBalance());
    }
}

public class ECommerceSystem {
    public static void main(String[] args) {
        CheckoutService checkoutService = new CheckoutService();

        System.out.println("=== Test Case 1: Normal Checkout ===");
        try {
            Product cheese = new PerishableProduct("Cheese", 100, 5, LocalDate.now().plusDays(10), 0.2);
            Product biscuits = new PerishableProduct("Biscuits", 150, 3, LocalDate.now().plusDays(5), 0.7);
            Product scratchCard = new NonPerishableProduct("Mobile Scratch Card", 50, 10, false, 0);
            Customer customer = new Customer(1000);
            Cart cart = new Cart();
            cart.add(cheese, 2);
            cart.add(biscuits, 1);
            cart.add(scratchCard, 1);
            checkoutService.checkout(customer, cart);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println("\n=== Test Case 2: Empty Cart ===");
        try {
            Customer customer = new Customer(1000);
            Cart emptyCart = new Cart();
            checkoutService.checkout(customer, emptyCart);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println("\n=== Test Case 3: Insufficient Balance ===");
        try {
            Product cheese = new PerishableProduct("Cheese", 100, 5, LocalDate.now().plusDays(10), 0.2);
            Customer poorCustomer = new Customer(100);
            Cart cart = new Cart();
            cart.add(cheese, 2);
            checkoutService.checkout(poorCustomer, cart);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println("\n=== Test Case 4: Out of Stock ===");
        try {
            Product tv = new NonPerishableProduct("TV", 1000, 2, true, 5.0);
            Cart cart = new Cart();
            cart.add(tv, 3);
            Customer customer = new Customer(5000);
            checkoutService.checkout(customer, cart);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println("\n=== Test Case 5: Expired Product ===");
        try {
            Product expiredCheese = new PerishableProduct("Cheese", 100, 5, LocalDate.now().minusDays(1), 0.2);
            Cart cart = new Cart();
            cart.add(expiredCheese, 2);
            Customer customer = new Customer(1000);
            checkoutService.checkout(customer, cart);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}