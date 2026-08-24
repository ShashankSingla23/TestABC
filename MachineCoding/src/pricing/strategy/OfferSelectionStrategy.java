package pricing.strategy;

import pricing.model.CartItem;
import pricing.model.LineItemResult;
import pricing.model.Offer;

import java.util.List;

/**
 * Strategy interface for offer selection during cart evaluation.
 *
 * Each implementation defines a different policy for choosing which offer(s)
 * to apply to a given cart line item. The system can swap strategies at runtime.
 *
 * Implementations must be stateless so a single instance can be safely shared
 * across concurrent cart evaluations.
 */
public interface OfferSelectionStrategy {

    /**
     * Evaluates a single cart line item against the list of candidate offers and
     * returns the computed result — including which offer was applied and the discount.
     *
     * Implementations are responsible for:
     *   1. Filtering out inapplicable / exhausted offers
     *   2. Selecting the best offer per the strategy's policy
     *   3. Calling tryRedeem() on the chosen offer (atomically consumes one global use)
     *   4. Ensuring the resulting per-unit price remains > 0
     *
     * @param item             the cart line item being evaluated
     * @param candidateOffers  all offers configured for this product (may be empty)
     * @return a LineItemResult describing the outcome (never null)
     */
    LineItemResult evaluate(CartItem item, List<Offer> candidateOffers);
}
