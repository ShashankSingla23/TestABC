package pricing.strategy;

import pricing.model.CartItem;
import pricing.model.LineItemResult;
import pricing.model.Offer;
import pricing.model.Product;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Offer selection strategy: apply the single offer that yields the highest
 * monetary discount for a line item.
 *
 * Algorithm:
 *   1. Filter: keep only offers that pass isApplicable() AND hasCapacity() (read-only)
 *   2. Sort:   descending by calculateDiscountPerUnit() — highest saving first
 *   3. Iterate sorted list:
 *        a. Skip any offer where (basePrice - discountPerUnit) <= 0   (price floor guard)
 *        b. Call tryRedeem() — atomic CAS; succeeds only if a slot is available
 *        c. First successful redemption wins; stop iterating
 *
 * Ordering ensures we always try the best offer first. The CAS in tryRedeem()
 * guarantees no over-redemption under concurrent evaluation.
 *
 * This class is stateless and thread-safe.
 */
public class BestSingleOfferStrategy implements OfferSelectionStrategy {

    @Override
    public LineItemResult evaluate(CartItem item, List<Offer> candidateOffers) {
        Product product = item.getProduct();
        String productId = product.getId();
        double basePrice = product.getBasePrice();
        int qty = item.getQuantity();

        // Step 1: filter to applicable + capacity-available offers
        List<Offer> validOffers = candidateOffers.stream()
                .filter(o -> o.isApplicable(productId, qty) && o.hasCapacity())
                .collect(Collectors.toList());

        // Collect valid offer ids for the result (for display purposes)
        List<String> validOfferIds = validOffers.stream()
                .map(Offer::getOfferId)
                .collect(Collectors.toList());

        // Step 2: sort by discount per unit descending
        validOffers.sort(Comparator.comparingDouble(
                (Offer o) -> o.calculateDiscountPerUnit(basePrice)).reversed());

        // Step 3: try to redeem the best available offer
        for (Offer offer : validOffers) {
            double discountPerUnit = offer.calculateDiscountPerUnit(basePrice);

            // Price floor guard — discounted price per unit must be > 0
            if (basePrice - discountPerUnit <= 0) continue;

            // Atomically claim one redemption slot
            if (offer.tryRedeem()) {
                double totalDiscount = discountPerUnit * qty;
                return new LineItemResult(
                        productId,
                        product.getName(),
                        qty,
                        basePrice,
                        validOfferIds,
                        offer.getOfferId(),
                        offer.discountDescription(),
                        totalDiscount
                );
            }
            // CAS failed — another thread took the last slot; try the next-best offer
        }

        // No offer could be applied
        return new LineItemResult(
                productId,
                product.getName(),
                qty,
                basePrice,
                validOfferIds,
                null,
                null,
                0.0
        );
    }
}
