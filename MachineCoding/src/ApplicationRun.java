import pricing.engine.PricingEngine;
import pricing.model.CartEvaluationResult;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Driver class — demonstrates and validates all core features of the Pricing Engine.
 *
 * Sections:
 *   A. Core test cases (Cart1, Cart2, Cart3) matching the problem spec
 *   B. Concurrency test — two threads race to redeem a single-use offer simultaneously
 *   C. Edge case tests — invalid inputs, exhausted offers, price floor guard
 */
public class ApplicationRun {

    public static void main(String[] args) throws InterruptedException {
        runCoreTestCases();
        runConcurrencyTest();
        runEdgeCaseTests();
    }

    // =========================================================================
    // A. Core test cases
    // =========================================================================

    private static void runCoreTestCases() {
        printSectionHeader("CORE TEST CASES");

        PricingEngine engine = new PricingEngine();

        // --- Setup: products ---
        engine.addProduct("Shirt", 1000);
        engine.addProduct("Shoes", 2000);
        engine.addProduct("Socks", 500);

        // --- Setup: offers ---
        // OFFER1: 10% off Shirt & Socks, min qty 2, max 2 global uses
        engine.createPercentageOffer("OFFER1", list("Shirt", "Socks"), 10, 2, 2);

        // OFFER2: Flat ₹300 off Shoes, min qty 1, max 5 global uses
        engine.createFlatOffer("OFFER2", list("Shoes"), 300, 1, 5);

        // OFFER3: 20% off Shoes, min qty 2, max 1 global use
        engine.createPercentageOffer("OFFER3", list("Shoes"), 20, 2, 1);

        // --- Cart 1 ---
        printSubHeader("Cart1 — Shirt×2, Shoes×2");
        engine.createCart("Cart1");
        engine.addToCart("Cart1", "Shirt", 2);
        engine.addToCart("Cart1", "Shoes", 2);
        CartEvaluationResult r1 = engine.evaluateCart("Cart1");
        System.out.println(r1);

        // Expected:
        //   Shirt: Qty 2. Base = 2000. Valid Offers: OFFER1. Discount = 200. Final = 1800.
        //   Shoes: Qty 2. Base = 4000. Valid Offers: OFFER2, OFFER3. Best Offer Applied: OFFER3. Discount = 800. Final = 3200.
        //   Cart Total: Base = 6000. Total Discount = 1000. Final Amount To Pay = 5000.

        // --- Cart 2 (OFFER3 now exhausted) ---
        printSubHeader("Cart2 — Shirt×2, Shoes×2 (OFFER3 exhausted)");
        engine.createCart("Cart2");
        engine.addToCart("Cart2", "Shirt", 2);
        engine.addToCart("Cart2", "Shoes", 2);
        CartEvaluationResult r2 = engine.evaluateCart("Cart2");
        System.out.println(r2);

        // Expected:
        //   Shirt: Qty 2. Base = 2000. Valid Offers: OFFER1. Discount = 200. Final = 1800.
        //   Shoes: Qty 2. Base = 4000. Valid Offers: OFFER2. Best Offer Applied: OFFER2. Discount = 600. Final = 3400.
        //   Cart Total: Base = 6000. Total Discount = 800. Final Amount To Pay = 5200.

        // --- Cart 3 (OFFER1 also exhausted now) ---
        printSubHeader("Cart3 — Socks×3 (OFFER1 exhausted)");
        engine.createCart("Cart3");
        engine.addToCart("Cart3", "Socks", 3);
        CartEvaluationResult r3 = engine.evaluateCart("Cart3");
        System.out.println(r3);

        // Expected:
        //   Socks: Qty 3. Base = 1500. Valid Offers: None. Discount = 0. Final = 1500.
        //   Cart Total: Base = 1500. Total Discount = 0. Final Amount To Pay = 1500.
    }

    // =========================================================================
    // B. Concurrency test — race to redeem a single-use offer
    // =========================================================================

    private static void runConcurrencyTest() throws InterruptedException {
        printSectionHeader("CONCURRENCY TEST — two threads race for a single-use offer");

        PricingEngine engine = new PricingEngine();
        engine.addProduct("Laptop", 50000);
        // FLASH_DEAL: 30% off Laptop, min qty 1, max 1 global use — only one thread should win
        engine.createPercentageOffer("FLASH_DEAL", list("Laptop"), 30, 1, 1);

        engine.createCart("ConcCart1");
        engine.addToCart("ConcCart1", "Laptop", 1);

        engine.createCart("ConcCart2");
        engine.addToCart("ConcCart2", "Laptop", 1);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        CartEvaluationResult[] results = new CartEvaluationResult[2];

        ExecutorService pool = Executors.newFixedThreadPool(2);

        pool.submit(() -> {
            try {
                startGate.await();                          // wait for simultaneous start
                results[0] = engine.evaluateCart("ConcCart1");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        pool.submit(() -> {
            try {
                startGate.await();
                results[1] = engine.evaluateCart("ConcCart2");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        startGate.countDown();                             // release both threads simultaneously
        doneLatch.await(5, TimeUnit.SECONDS);
        pool.shutdown();

        System.out.println("ConcCart1 result:\n" + results[0]);
        System.out.println("ConcCart2 result:\n" + results[1]);

        double disc0 = results[0].getTotalDiscount();
        double disc1 = results[1].getTotalDiscount();
        double total = disc0 + disc1;

        // Exactly one cart should have received the 30% discount (₹15000), the other ₹0
        System.out.printf("Discounts — ConcCart1: %.0f, ConcCart2: %.0f, Combined: %.0f%n",
                disc0, disc1, total);
        System.out.println("Concurrency test: " +
                (total == 15000 ? "PASS ✓ — exactly one cart received the offer" :
                        "FAIL ✗ — expected combined discount of 15000"));
    }

    // =========================================================================
    // C. Edge case tests
    // =========================================================================

    private static void runEdgeCaseTests() {
        printSectionHeader("EDGE CASE TESTS");

        PricingEngine engine = new PricingEngine();
        engine.addProduct("Widget", 100);
        engine.createFlatOffer("ALMOST_FREE", list("Widget"), 99, 1, 10); // leaves ₹1 per unit

        // Test 1: qty below minQty — offer should not apply
        engine.createCart("EdgeCart1");
        engine.addToCart("EdgeCart1", "Widget", 1);
        engine.createPercentageOffer("HIGH_MIN", list("Widget"), 50, 5, 10); // needs qty >= 5
        CartEvaluationResult r1 = engine.evaluateCart("EdgeCart1");
        System.out.println("Test 1 — qty(1) below minQty(5) for HIGH_MIN:");
        System.out.println(r1);
        System.out.println("Result: " + (r1.getTotalDiscount() == 99 ? "PASS ✓" : "FAIL ✗") + "\n");

        // Test 2: duplicate addToCart calls merge quantities correctly.
        // Uses a fresh engine with a product that has ONLY HIGH_MIN (needs qty>=5, 50% off).
        // Adding qty 2 then 3 must merge to 5, activating the offer.
        PricingEngine engine2b = new PricingEngine();
        engine2b.addProduct("Gadget", 100);
        engine2b.createPercentageOffer("HIGH_MIN2", list("Gadget"), 50, 5, 10);
        engine2b.createCart("EdgeCart2");
        engine2b.addToCart("EdgeCart2", "Gadget", 2);
        engine2b.addToCart("EdgeCart2", "Gadget", 3); // merges → qty 5
        CartEvaluationResult r2 = engine2b.evaluateCart("EdgeCart2");
        System.out.println("Test 2 — merged qty (2+3=5) triggers HIGH_MIN2 offer (50% × 5 × 100 = 250):");
        System.out.println(r2);
        System.out.println("Result: " + (r2.getTotalDiscount() == 250 ? "PASS ✓" : "FAIL ✗") + "\n");

        // Test 3: duplicate product registration throws
        System.out.println("Test 3 — duplicate product registration:");
        try {
            engine.addProduct("Widget", 200);
            System.out.println("FAIL ✗ — expected exception");
        } catch (IllegalStateException e) {
            System.out.println("PASS ✓ — caught: " + e.getMessage());
        }
        System.out.println();

        // Test 4: cart for unknown product throws
        System.out.println("Test 4 — add unknown product to cart:");
        engine.createCart("EdgeCart3");
        try {
            engine.addToCart("EdgeCart3", "Gadget", 1);
            System.out.println("FAIL ✗ — expected exception");
        } catch (IllegalArgumentException e) {
            System.out.println("PASS ✓ — caught: " + e.getMessage());
        }
        System.out.println();

        // Test 5: evaluate unknown cart throws
        System.out.println("Test 5 — evaluate non-existent cart:");
        try {
            engine.evaluateCart("Ghost");
            System.out.println("FAIL ✗ — expected exception");
        } catch (IllegalArgumentException e) {
            System.out.println("PASS ✓ — caught: " + e.getMessage());
        }
        System.out.println();

        // Test 6: no offers configured for product — zero discount
        PricingEngine engine2 = new PricingEngine();
        engine2.addProduct("NoOfferItem", 500);
        engine2.createCart("EdgeCart4");
        engine2.addToCart("EdgeCart4", "NoOfferItem", 3);
        CartEvaluationResult r4 = engine2.evaluateCart("EdgeCart4");
        System.out.println("Test 6 — product with no offers:");
        System.out.println(r4);
        System.out.println("Result: " + (r4.getTotalDiscount() == 0 ? "PASS ✓" : "FAIL ✗") + "\n");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static List<String> list(String... names) {
        return Arrays.asList(names);
    }

    private static void printSectionHeader(String title) {
        String line = new String(new char[70]).replace('\0', '=');
        System.out.println("\n" + line);
        System.out.println("  " + title);
        System.out.println(line);
    }

    private static void printSubHeader(String title) {
        System.out.println("\n--- " + title + " ---");
    }
}
