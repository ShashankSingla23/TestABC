package pricing.model;

import java.util.Collections;
import java.util.List;

/**
 * Immutable result of evaluating an entire cart.
 */
public class CartEvaluationResult {

    private final String cartId;
    private final List<LineItemResult> lineItems;
    private final double totalBasePrice;
    private final double totalDiscount;
    private final double finalAmount;

    public CartEvaluationResult(String cartId, List<LineItemResult> lineItems) {
        this.cartId = cartId;
        this.lineItems = Collections.unmodifiableList(lineItems);
        this.totalBasePrice = lineItems.stream()
                .mapToDouble(LineItemResult::getTotalBasePrice).sum();
        this.totalDiscount = lineItems.stream()
                .mapToDouble(LineItemResult::getTotalDiscount).sum();
        this.finalAmount = totalBasePrice - totalDiscount;
    }

    public String getCartId()                   { return cartId; }
    public List<LineItemResult> getLineItems()   { return lineItems; }
    public double getTotalBasePrice()            { return totalBasePrice; }
    public double getTotalDiscount()             { return totalDiscount; }
    public double getFinalAmount()               { return finalAmount; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (LineItemResult item : lineItems) {
            sb.append(item).append("\n");
        }
        sb.append(String.format("Cart Total: Base = %s. Total Discount = %s. Final Amount To Pay = %s.",
                formatAmount(totalBasePrice),
                formatAmount(totalDiscount),
                formatAmount(finalAmount)));
        return sb.toString();
    }

    private static String formatAmount(double amount) {
        if (amount == Math.floor(amount)) return String.valueOf((long) amount);
        return String.format("%.2f", amount);
    }
}
