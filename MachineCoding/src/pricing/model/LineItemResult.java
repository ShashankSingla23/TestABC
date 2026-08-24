package pricing.model;

import java.util.Collections;
import java.util.List;

/**
 * Immutable result for a single cart line item after offer evaluation.
 */
public class LineItemResult {

    private final String productId;
    private final String productName;
    private final int quantity;
    private final double unitBasePrice;
    private final double totalBasePrice;
    private final List<String> validOfferIds;   // all offers that were eligible
    private final String appliedOfferId;         // null if no offer applied
    private final String appliedOfferDescription;
    private final double totalDiscount;
    private final double finalPrice;

    public LineItemResult(String productId,
                          String productName,
                          int quantity,
                          double unitBasePrice,
                          List<String> validOfferIds,
                          String appliedOfferId,
                          String appliedOfferDescription,
                          double totalDiscount) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitBasePrice = unitBasePrice;
        this.totalBasePrice = unitBasePrice * quantity;
        this.validOfferIds = Collections.unmodifiableList(validOfferIds);
        this.appliedOfferId = appliedOfferId;
        this.appliedOfferDescription = appliedOfferDescription;
        this.totalDiscount = totalDiscount;
        this.finalPrice = this.totalBasePrice - totalDiscount;
    }

    public String getProductId()              { return productId; }
    public String getProductName()            { return productName; }
    public int getQuantity()                  { return quantity; }
    public double getUnitBasePrice()          { return unitBasePrice; }
    public double getTotalBasePrice()         { return totalBasePrice; }
    public List<String> getValidOfferIds()    { return validOfferIds; }
    public String getAppliedOfferId()         { return appliedOfferId; }
    public String getAppliedOfferDescription(){ return appliedOfferDescription; }
    public double getTotalDiscount()          { return totalDiscount; }
    public double getFinalPrice()             { return finalPrice; }

    /**
     * Formats the line item in the expected output style:
     *   Shirt: Qty 2. Base = 2000. Valid Offers: OFFER1. Discount = 200. Final = 1800.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(productName)
          .append(": Qty ").append(quantity)
          .append(". Base = ").append(formatAmount(totalBasePrice))
          .append(". Valid Offers: ");

        if (validOfferIds.isEmpty()) {
            sb.append("None");
        } else {
            // Build per-offer breakdown for multi-offer products
            sb.append(String.join(", ", validOfferIds));
        }

        if (appliedOfferId != null) {
            sb.append(". Best Offer Applied: ").append(appliedOfferId);
        }

        sb.append(". Discount = ").append(formatAmount(totalDiscount))
          .append(". Final = ").append(formatAmount(finalPrice))
          .append(".");

        return sb.toString();
    }

    private static String formatAmount(double amount) {
        // Show as integer when value is whole, otherwise 2 decimals
        if (amount == Math.floor(amount)) return String.valueOf((long) amount);
        return String.format("%.2f", amount);
    }
}
