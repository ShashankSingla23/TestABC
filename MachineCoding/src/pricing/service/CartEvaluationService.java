package pricing.service;

import pricing.model.Cart;
import pricing.model.CartEvaluationResult;
import pricing.model.CartItem;
import pricing.model.LineItemResult;
import pricing.model.Offer;
import pricing.repository.OfferRepository;
import pricing.strategy.OfferSelectionStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates cart evaluation:
 *   1. For each line item, fetches the candidate offers from the repository.
 *   2. Delegates to the injected OfferSelectionStrategy to pick and redeem the best offer.
 *   3. Aggregates per-item results into a CartEvaluationResult.
 *
 * The service is stateless — it holds only immutable collaborators — so a single
 * instance is safe to share across threads.
 */
public class CartEvaluationService {

    private final OfferRepository offerRepository;
    private final OfferSelectionStrategy strategy;

    public CartEvaluationService(OfferRepository offerRepository, OfferSelectionStrategy strategy) {
        if (offerRepository == null) throw new IllegalArgumentException("OfferRepository must not be null");
        if (strategy == null)        throw new IllegalArgumentException("OfferSelectionStrategy must not be null");
        this.offerRepository = offerRepository;
        this.strategy = strategy;
    }

    /**
     * Evaluates the given cart and returns a complete breakdown.
     * Offer redemption (global use count increment) happens inside this call.
     */
    public CartEvaluationResult evaluate(Cart cart) {
        if (cart == null) throw new IllegalArgumentException("Cart must not be null");

        List<LineItemResult> lineItems = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            List<Offer> candidates = offerRepository.findByProductId(item.getProduct().getId());
            LineItemResult result = strategy.evaluate(item, candidates);
            lineItems.add(result);
        }

        return new CartEvaluationResult(cart.getCartId(), lineItems);
    }
}
