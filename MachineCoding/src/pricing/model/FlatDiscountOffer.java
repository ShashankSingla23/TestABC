package pricing.model;

import java.util.Set;

/**
 * An offer that applies a fixed monetary discount per unit.
 * e.g. Flat ₹300 off Shoes (base ₹2000) → ₹300 discount per unit.
 *
 * The strategy layer validates that (basePrice - flatAmount) > 0 before redeeming,
 * so we never produce a negative price.
 */
public class FlatDiscountOffer extends Offer {

    private final double flatDiscountAmount; // e.g. 300.0

    public FlatDiscountOffer(String offerId, Set<String> productIds,
                             double flatDiscountAmount, int minQty, int maxGlobalUses) {
        super(offerId, productIds, minQty, maxGlobalUses);
        if (flatDiscountAmount <= 0)
            throw new IllegalArgumentException(
                    "Flat discount amount must be positive, got: " + flatDiscountAmount);
        this.flatDiscountAmount = flatDiscountAmount;
    }

    @Override
    public double calculateDiscountPerUnit(double basePrice) {
        // Cap at basePrice so callers always get a meaningful value to compare
        return Math.min(flatDiscountAmount, basePrice);
    }

    @Override
    public String discountDescription() {
        return String.format("Flat %.0f/unit", flatDiscountAmount);
    }

    public double getFlatDiscountAmount() { return flatDiscountAmount; }
}
