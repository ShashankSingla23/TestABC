package pricing.factory;

import pricing.model.FlatDiscountOffer;
import pricing.model.Offer;
import pricing.model.PercentageDiscountOffer;

import java.util.Set;

/**
 * Factory for creating Offer instances.
 * Centralises construction so callers never reference concrete subclasses directly.
 * New offer types only require adding a method here — no changes elsewhere.
 */
public class OfferFactory {

    /**
     * Creates a percentage-discount offer.
     *
     * @param offerId            unique identifier
     * @param productIds         set of product ids this offer applies to
     * @param discountPercentage percentage to discount (must be in (0, 100))
     * @param minQty             minimum cart quantity for activation
     * @param maxGlobalUses      maximum times this offer can be redeemed globally
     */
    public Offer createPercentageOffer(String offerId,
                                       Set<String> productIds,
                                       double discountPercentage,
                                       int minQty,
                                       int maxGlobalUses) {
        return new PercentageDiscountOffer(offerId, productIds, discountPercentage, minQty, maxGlobalUses);
    }

    /**
     * Creates a flat-discount offer.
     *
     * @param offerId            unique identifier
     * @param productIds         set of product ids this offer applies to
     * @param flatDiscountAmount fixed monetary discount per unit
     * @param minQty             minimum cart quantity for activation
     * @param maxGlobalUses      maximum times this offer can be redeemed globally
     */
    public Offer createFlatOffer(String offerId,
                                 Set<String> productIds,
                                 double flatDiscountAmount,
                                 int minQty,
                                 int maxGlobalUses) {
        return new FlatDiscountOffer(offerId, productIds, flatDiscountAmount, minQty, maxGlobalUses);
    }
}
