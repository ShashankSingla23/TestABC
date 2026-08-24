package pricing.engine;

import pricing.factory.OfferFactory;
import pricing.model.Cart;
import pricing.model.CartEvaluationResult;
import pricing.model.Offer;
import pricing.model.Product;
import pricing.repository.CartRepository;
import pricing.repository.OfferRepository;
import pricing.repository.ProductRepository;
import pricing.service.CartEvaluationService;
import pricing.strategy.BestSingleOfferStrategy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Facade — the single public entry point for all pricing engine operations.
 *
 * Wires together repositories, factory, strategy, and service internally so
 * callers (e.g. ApplicationRun) only interact with this one class.
 *
 * This class is thread-safe: all mutable state is inside the thread-safe
 * repositories, and the service/strategy are stateless.
 */
public class PricingEngine {

    private final ProductRepository productRepository;
    private final OfferRepository offerRepository;
    private final CartRepository cartRepository;
    private final OfferFactory offerFactory;
    private final CartEvaluationService evaluationService;

    public PricingEngine() {
        this.productRepository = new ProductRepository();
        this.offerRepository   = new OfferRepository();
        this.cartRepository    = new CartRepository();
        this.offerFactory      = new OfferFactory();
        // Strategy is stateless — one shared instance is safe
        this.evaluationService = new CartEvaluationService(offerRepository, new BestSingleOfferStrategy());
    }

    // -------------------------------------------------------------------------
    // Product operations
    // -------------------------------------------------------------------------

    /**
     * Onboards a new product. The product name is used as its id (case-insensitive).
     *
     * @throws IllegalStateException if a product with this name already exists
     */
    public Product addProduct(String name, double basePrice) {
        String id = normalise(name);
        Product product = new Product(id, name, basePrice);
        productRepository.save(product);
        return product;
    }

    // -------------------------------------------------------------------------
    // Offer operations
    // -------------------------------------------------------------------------

    /**
     * Creates a percentage-discount offer and maps it to the given products.
     *
     * @param offerId            unique offer identifier
     * @param productNames       names of products this offer applies to
     * @param discountPercentage e.g. 10.0 for 10%
     * @param minQty             minimum quantity in cart to activate
     * @param maxGlobalUses      maximum global redemptions allowed
     * @throws IllegalArgumentException if any product name is not found
     */
    public Offer createPercentageOffer(String offerId,
                                       List<String> productNames,
                                       double discountPercentage,
                                       int minQty,
                                       int maxGlobalUses) {
        Set<String> productIds = resolveProductIds(productNames);
        Offer offer = offerFactory.createPercentageOffer(
                offerId, productIds, discountPercentage, minQty, maxGlobalUses);
        offerRepository.save(offer);
        return offer;
    }

    /**
     * Creates a flat-discount offer and maps it to the given products.
     *
     * @param offerId            unique offer identifier
     * @param productNames       names of products this offer applies to
     * @param flatDiscountAmount fixed monetary discount per unit (e.g. 300.0)
     * @param minQty             minimum quantity in cart to activate
     * @param maxGlobalUses      maximum global redemptions allowed
     * @throws IllegalArgumentException if any product name is not found
     */
    public Offer createFlatOffer(String offerId,
                                 List<String> productNames,
                                 double flatDiscountAmount,
                                 int minQty,
                                 int maxGlobalUses) {
        Set<String> productIds = resolveProductIds(productNames);
        Offer offer = offerFactory.createFlatOffer(
                offerId, productIds, flatDiscountAmount, minQty, maxGlobalUses);
        offerRepository.save(offer);
        return offer;
    }

    // -------------------------------------------------------------------------
    // Cart operations
    // -------------------------------------------------------------------------

    /**
     * Creates a new empty cart with the given id.
     *
     * @throws IllegalStateException if a cart with this id already exists
     */
    public Cart createCart(String cartId) {
        Cart cart = new Cart(cartId);
        cartRepository.save(cart);
        return cart;
    }

    /**
     * Adds the specified quantity of a product to an existing cart.
     * If the product is already in the cart, its quantity is incremented.
     *
     * @throws IllegalArgumentException if the cart or product is not found
     */
    public void addToCart(String cartId, String productName, int quantity) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

        String productId = normalise(productName);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productName));

        cart.addItem(product, quantity);
    }

    /**
     * Evaluates the cart using the Best Single Offer strategy.
     * Redeems (increments global use count for) each applied offer.
     *
     * @throws IllegalArgumentException if the cart is not found
     */
    public CartEvaluationResult evaluateCart(String cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));
        return evaluationService.evaluate(cart);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Converts a product name to the normalised id used as the map key. */
    private static String normalise(String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Name must not be blank");
        return name.trim().toLowerCase();
    }

    /**
     * Resolves a list of product names to their normalised ids, validating that
     * each product exists in the catalog.
     */
    private Set<String> resolveProductIds(List<String> productNames) {
        if (productNames == null || productNames.isEmpty())
            throw new IllegalArgumentException("At least one product name required");

        Set<String> ids = new HashSet<>();
        for (String name : productNames) {
            String id = normalise(name);
            if (!productRepository.exists(id)) {
                throw new IllegalArgumentException("Product not found: '" + name + "'. Add it first.");
            }
            ids.add(id);
        }
        return ids;
    }
}
