package pricing.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A shopping cart identified by a unique cartId.
 *
 * Thread-safety: mutations (addItem) are synchronized on the instance so concurrent
 * add_to_cart calls for the same cart are safe. Reads (getItems) return an unmodifiable
 * snapshot to prevent external mutation.
 */
public class Cart {

    private final String cartId;

    // productId → CartItem; LinkedHashMap preserves insertion order for deterministic output
    private final Map<String, CartItem> items = new LinkedHashMap<>();

    public Cart(String cartId) {
        if (cartId == null || cartId.trim().isEmpty())
            throw new IllegalArgumentException("Cart id must not be blank");
        this.cartId = cartId;
    }

    /**
     * Adds the given quantity of the product to the cart.
     * If the product is already in the cart, its quantity is incremented.
     */
    public synchronized void addItem(Product product, int quantity) {
        if (product == null) throw new IllegalArgumentException("Product must not be null");
        if (quantity < 1)    throw new IllegalArgumentException("Quantity must be >= 1, got: " + quantity);

        items.merge(
                product.getId(),
                new CartItem(product, quantity),
                (existing, newItem) -> { existing.addQuantity(quantity); return existing; }
        );
    }

    /** Returns an unmodifiable view of the current line items. */
    public synchronized Collection<CartItem> getItems() {
        return Collections.unmodifiableCollection(items.values());
    }

    public String getCartId() { return cartId; }

    @Override
    public String toString() {
        return String.format("Cart{id='%s', items=%d}", cartId, items.size());
    }
}
