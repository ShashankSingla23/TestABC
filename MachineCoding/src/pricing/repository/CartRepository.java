package pricing.repository;

import pricing.model.Cart;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store for Carts.
 */
public class CartRepository {

    private final ConcurrentHashMap<String, Cart> store = new ConcurrentHashMap<>();

    /**
     * Saves a new cart. Throws if a cart with the same id already exists.
     */
    public void save(Cart cart) {
        if (cart == null) throw new IllegalArgumentException("Cart must not be null");
        Cart existing = store.putIfAbsent(cart.getCartId(), cart);
        if (existing != null) {
            throw new IllegalStateException(
                    "Cart with id '" + cart.getCartId() + "' already exists");
        }
    }

    public Optional<Cart> findById(String cartId) {
        return Optional.ofNullable(store.get(cartId));
    }

    public boolean exists(String cartId) {
        return store.containsKey(cartId);
    }
}
