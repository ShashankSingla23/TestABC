package pricing.model;

/**
 * A single line item in a cart: one product + the quantity added.
 * Quantity is mutable to allow add_to_cart to accumulate units for the same product.
 */
public class CartItem {

    private final Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        if (product == null) throw new IllegalArgumentException("Product must not be null");
        if (quantity < 1)    throw new IllegalArgumentException("Quantity must be >= 1, got: " + quantity);
        this.product = product;
        this.quantity = quantity;
    }

    /** Adds more units to this line item. */
    public void addQuantity(int extra) {
        if (extra < 1) throw new IllegalArgumentException("Extra quantity must be >= 1, got: " + extra);
        this.quantity += extra;
    }

    public Product getProduct() { return product; }
    public int getQuantity()    { return quantity; }

    @Override
    public String toString() {
        return String.format("CartItem{product='%s', qty=%d}", product.getId(), quantity);
    }
}
