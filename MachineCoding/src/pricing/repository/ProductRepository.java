package pricing.repository;

import pricing.model.Product;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store for Products.
 */
public class ProductRepository {

    private final ConcurrentHashMap<String, Product> store = new ConcurrentHashMap<>();

    /**
     * Persists a product. Throws if a product with the same id already exists.
     */
    public void save(Product product) {
        if (product == null) throw new IllegalArgumentException("Product must not be null");
        Product existing = store.putIfAbsent(product.getId(), product);
        if (existing != null) {
            throw new IllegalStateException(
                    "Product with id '" + product.getId() + "' already exists");
        }
    }

    public Optional<Product> findById(String productId) {
        return Optional.ofNullable(store.get(productId));
    }

    public Collection<Product> findAll() {
        return Collections.unmodifiableCollection(store.values());
    }

    public boolean exists(String productId) {
        return store.containsKey(productId);
    }
}
