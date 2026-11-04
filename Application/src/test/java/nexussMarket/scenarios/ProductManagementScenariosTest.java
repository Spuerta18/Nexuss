package nexussMarket.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import nexussMarket.domain.exceptions.InvalidProductPriceException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.exceptions.ProductDiscontinuedException;
import nexussMarket.domain.exceptions.ProductInOpenOrderException;
import nexussMarket.domain.exceptions.SellerNotAuthorizedException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.ports.out.OperationRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.SellerStatus;

/** Editing a product (name and price) and deleting it logically ({@code DISCONTINUED}). */
class ProductManagementScenariosTest extends MemoryScenarioSupport {

    @Autowired
    private ProductRepositoryPort products;
    @Autowired
    private OperationRepositoryPort operations;

    @Test
    void theOwnerChangesNameAndPriceAndConfirmedOrdersKeepTheirPrice() {
        Seller seller = seller();
        Product product = stockedProduct(seller, warehouseOf(seller), 5);
        Order order = pendingOrder(buyer(), product, 1);

        Product updated = sellerPort.updateProduct(seller, changes(product, "Silent Keyboard", 1500));

        assertEquals("Silent Keyboard", updated.getName());
        assertEquals(0, updated.getPrice().getAmount().compareTo(cop(1500).getAmount()));
        assertEquals(0, orders.findById(order.getIdentifier()).orElseThrow().getLines().get(0).getUnitPrice()
                .getAmount().compareTo(cop(1000).getAmount()), "the order keeps the price it was confirmed with");
        assertEquals(1, count(OperationType.PRODUCT_UPDATE, product));
    }

    @Test
    void editingFollowsThePublicationRules() {
        Seller owner = seller();
        Product product = stockedProduct(owner, warehouseOf(owner), 0);

        assertThrows(OperationNotAllowedException.class, () -> sellerPort.updateProduct(seller(),
                changes(product, "Hijacked", 1)));
        assertThrows(InvalidProductPriceException.class, () -> sellerPort.updateProduct(owner,
                changes(product, "Free", 0)));
        owner.setSellerStatus(SellerStatus.SUSPENDED);
        users.save(owner);
        assertThrows(SellerNotAuthorizedException.class, () -> sellerPort.updateProduct(
                (Seller) users.findById(owner.getIdentifier()).orElseThrow(), changes(product, "Late", 10)));
        assertEquals("Keyboard", products.findById(product.getIdentifier()).orElseThrow().getName());
    }

    @Test
    void deletingIsLogicalIdempotentAndHidesTheProductFromBuyers() {
        Seller seller = seller();
        Product product = stockedProduct(seller, warehouseOf(seller), 5);
        Buyer buyer = buyer();

        Product deleted = sellerPort.discontinueProduct(seller, reference(product));
        Product again = sellerPort.discontinueProduct(seller, reference(product));

        assertEquals(ProductStatus.DISCONTINUED, deleted.getStatus());
        assertEquals(ProductStatus.DISCONTINUED, again.getStatus());
        assertEquals(1, count(OperationType.PRODUCT_DISCONTINUATION, product), "the second call records nothing");
        assertFalse(buyerPort.listPublishedProducts(buyer).stream()
                .anyMatch(listed -> listed.getIdentifier().equals(product.getIdentifier())));
        assertThrows(ProductDiscontinuedException.class, () -> sellerPort.updateProduct(seller,
                changes(product, "Back", 10)));
    }

    @Test
    void aProductInAnOpenOrderCannotBeDeletedUntilTheOrderCloses() {
        Seller seller = seller();
        Product product = stockedProduct(seller, warehouseOf(seller), 5);
        Buyer buyer = buyer();
        Order order = pendingOrder(buyer, product, 1);

        assertThrows(ProductInOpenOrderException.class, () -> sellerPort.discontinueProduct(seller, reference(product)));
        assertEquals(ProductStatus.PUBLISHED, products.findById(product.getIdentifier()).orElseThrow().getStatus());

        buyerPort.cancelOrder(buyer, orderReference(order));
        assertEquals(ProductStatus.DISCONTINUED, sellerPort.discontinueProduct(seller, reference(product)).getStatus());
    }

    @Test
    void anotherSellerCannotDeleteTheProduct() {
        Seller owner = seller();
        Product product = stockedProduct(owner, warehouseOf(owner), 0);

        assertThrows(OperationNotAllowedException.class, () -> sellerPort.discontinueProduct(seller(),
                reference(product)));
        assertEquals(ProductStatus.PUBLISHED, products.findById(product.getIdentifier()).orElseThrow().getStatus());
    }

    private static Product changes(Product product, String name, long price) {
        return new Product(product.getIdentifier(), name, null, null, null, cop(price), List.of());
    }

    private long count(OperationType type, Product product) {
        return operations.findAll().stream().filter(operation -> operation.getOperationType() == type
                && product.getIdentifier().equals(operation.getAffectedEntityId())).count();
    }
}
