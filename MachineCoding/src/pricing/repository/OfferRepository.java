package pricing.repository;

import pricing.model.Offer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory store for Offers.
 *
 * Maintains two structures:
 *   1. primary  — offerId → Offer (for O(1) lookup by id)
 *   2. index    — productId → [Offer...] (for O(1) lookup by product during evaluation)
 *
 * CopyOnWriteArrayList is used for the index values because offers are registered
 * once at startup (rare writes) and read very frequently during cart evaluation.
 */
public class OfferRepository {

    private final ConcurrentHashMap<String, Offer> primary = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Offer>> index = new ConcurrentHashMap<>();

    /**
     * Saves an offer and registers it in the product index.
     * Throws if an offer with the same id already exists.
     */
    public void save(Offer offer) {
        if (offer == null) throw new IllegalArgumentException("Offer must not be null");

        Offer existing = primary.putIfAbsent(offer.getOfferId(), offer);
        if (existing != null) {
            throw new IllegalStateException(
                    "Offer with id '" + offer.getOfferId() + "' already exists");
        }

        // Register in the product → offers index for each targeted product
        for (String productId : offer.getProductIds()) {
            index.computeIfAbsent(productId, k -> new CopyOnWriteArrayList<>()).add(offer);
        }
    }

    public Optional<Offer> findById(String offerId) {
        return Optional.ofNullable(primary.get(offerId));
    }

    /**
     * Returns all offers associated with the given product.
     * Returns an empty list if no offers are configured for that product.
     */
    public List<Offer> findByProductId(String productId) {
        CopyOnWriteArrayList<Offer> offers = index.get(productId);
        if (offers == null) return Collections.emptyList();
        return new ArrayList<>(offers); // snapshot; safe for per-evaluation processing
    }

    public boolean exists(String offerId) {
        return primary.containsKey(offerId);
    }
}
