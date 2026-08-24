package pricing.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base for all offer types. Implements shared validity/redemption logic
 * via the Template Method pattern. Subclasses only override calculateDiscountPerUnit().
 *
 * Thread-safety: currentUseCount is an AtomicInteger; tryRedeem() uses a CAS loop
 * so concurrent cart evaluations never over-redeem an offer.
 */
public abstract class Offer {

    private final String offerId;
    private final Set<String> productIds;   // immutable view after construction
    private final int minQty;
    private final int maxGlobalUses;
    private final AtomicInteger currentUseCount = new AtomicInteger(0);

    protected Offer(String offerId, Set<String> productIds, int minQty, int maxGlobalUses) {
        if (offerId == null || offerId.trim().isEmpty())
            throw new IllegalArgumentException("Offer id must not be blank");
        if (productIds == null || productIds.isEmpty())
            throw new IllegalArgumentException("Offer must target at least one product");
        if (minQty < 1)
            throw new IllegalArgumentException("minQty must be >= 1, got: " + minQty);
        if (maxGlobalUses < 1)
            throw new IllegalArgumentException("maxGlobalUses must be >= 1, got: " + maxGlobalUses);

        this.offerId = offerId;
        this.productIds = Collections.unmodifiableSet(new HashSet<>(productIds));
        this.minQty = minQty;
        this.maxGlobalUses = maxGlobalUses;
    }

    // -------------------------------------------------------------------------
    // Template method — subclasses implement only this
    // -------------------------------------------------------------------------

    /**
     * Returns the monetary discount for a single unit at the given base price.
     * Must never return a value that would make (basePrice - discount) <= 0;
     * callers validate this before redeeming.
     */
    public abstract double calculateDiscountPerUnit(double basePrice);

    /**
     * Human-readable description of the discount (e.g. "10% off", "Flat 300").
     */
    public abstract String discountDescription();

    // -------------------------------------------------------------------------
    // Shared logic
    // -------------------------------------------------------------------------

    /**
     * Non-destructive check: is this offer applicable for the given product and quantity?
     */
    public boolean isApplicable(String productId, int qty) {
        return productIds.contains(productId) && qty >= minQty;
    }

    /**
     * Non-destructive check: does this offer still have redemption capacity?
     * Used as a fast pre-screen before the CAS in tryRedeem().
     */
    public boolean hasCapacity() {
        return currentUseCount.get() < maxGlobalUses;
    }

    /**
     * Atomically attempts to redeem one use of this offer via a CAS loop.
     * Returns true if successfully claimed, false if the offer was exhausted
     * (either already at limit or lost a concurrent race to the last slot).
     *
     * IMPORTANT: call this only after discount validity has been confirmed.
     * A successful tryRedeem() consumes one global use — never call speculatively.
     */
    public boolean tryRedeem() {
        int current;
        do {
            current = currentUseCount.get();
            if (current >= maxGlobalUses) return false;
        } while (!currentUseCount.compareAndSet(current, current + 1));
        return true;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getOfferId()        { return offerId; }
    public Set<String> getProductIds(){ return productIds; }
    public int getMinQty()            { return minQty; }
    public int getMaxGlobalUses()     { return maxGlobalUses; }
    public int getCurrentUseCount()   { return currentUseCount.get(); }

    @Override
    public String toString() {
        return String.format("Offer{id='%s', type=%s, uses=%d/%d}",
                offerId, discountDescription(), currentUseCount.get(), maxGlobalUses);
    }
}
