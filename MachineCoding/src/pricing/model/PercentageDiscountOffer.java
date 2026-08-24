package pricing.model;

import java.util.Set;

/**
 * An offer that discounts a product by a fixed percentage of its base price.
 * e.g. 10% off Shirt (base ₹1000) → ₹100 discount per unit.
 */
public class PercentageDiscountOffer extends Offer {

    private final double discountPercentage; // e.g. 10.0 for 10%

    public PercentageDiscountOffer(String offerId, Set<String> productIds,
                                   double discountPercentage, int minQty, int maxGlobalUses) {
        super(offerId, productIds, minQty, maxGlobalUses);
        if (discountPercentage <= 0 || discountPercentage >= 100)
            throw new IllegalArgumentException(
                    "Discount percentage must be in (0, 100), got: " + discountPercentage);
        this.discountPercentage = discountPercentage;
    }

    @Override
    public double calculateDiscountPerUnit(double basePrice) {
        return basePrice * discountPercentage / 100.0;
    }

    @Override
    public String discountDescription() {
        return String.format("%.0f%% off", discountPercentage);
    }

    public double getDiscountPercentage() { return discountPercentage; }
}
