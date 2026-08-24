package pricing.model;

/**
 * Represents a product in the catalog.
 * Immutable after creation — no price update capability by design.
 */
public class Product {

    private final String id;
    private final String name;
    private final double basePrice;

    public Product(String id, String name, double basePrice) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("Product id must not be blank");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Product name must not be blank");
        if (basePrice <= 0) throw new IllegalArgumentException("Base price must be positive, got: " + basePrice);
        this.id = id;
        this.name = name;
        this.basePrice = basePrice;
    }

    public String getId()        { return id; }
    public String getName()      { return name; }
    public double getBasePrice() { return basePrice; }

    @Override
    public String toString() {
        return String.format("Product{id='%s', name='%s', basePrice=%.2f}", id, name, basePrice);
    }
}
