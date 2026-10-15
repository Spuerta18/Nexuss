package nexussMarket.adapters.in.rest.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import nexussMarket.adapters.in.rest.exception.InvalidRequestException;
import nexussMarket.adapters.in.rest.requests.CreateShipmentRequest;
import nexussMarket.adapters.in.rest.requests.MoneyRequest;
import nexussMarket.adapters.in.rest.requests.PublishProductRequest;
import nexussMarket.adapters.in.rest.requests.RegisterBuyerRequest;
import nexussMarket.adapters.in.rest.requests.RegisterWarehouseRequest;
import nexussMarket.adapters.in.rest.requests.RequestReturnRequest;
import nexussMarket.adapters.in.rest.requests.VariantRequest;
import nexussMarket.adapters.in.rest.responses.OrderResponse;
import nexussMarket.adapters.in.rest.responses.UserResponse;
import nexussMarket.domain.enums.VariantAttributeType;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

class RestMappersTest {

    @Test
    void userReferenceSubtypeFollowsTheRole() {
        assertInstanceOf(Buyer.class, DomainReferences.user("U-1", SystemRole.BUYER));
        assertInstanceOf(Seller.class, DomainReferences.user("U-1", SystemRole.SELLER));
        assertInstanceOf(LogisticsOperator.class, DomainReferences.user("U-1", SystemRole.LOGISTICS_OPERATOR));
        assertInstanceOf(Administrator.class, DomainReferences.user("U-1", SystemRole.ADMINISTRATOR));
        assertInstanceOf(Supervisor.class, DomainReferences.user("U-1", SystemRole.SUPERVISOR));
        assertEquals("U-1", DomainReferences.user("U-1", SystemRole.SELLER).getIdentifier());
        assertThrows(InvalidRequestException.class, () -> DomainReferences.user("U-1", null));
    }

    @Test
    void unknownRoleCodeIsAClientError() {
        InvalidRequestException error = assertThrows(InvalidRequestException.class, () -> UserRestMapper.role("KING"));
        assertEquals("Unknown role 'KING'", error.getMessage());
    }

    @Test
    void userResponseNeverCarriesThePasswordHash() {
        Buyer buyer = new Buyer("B-1", "Ana Buyer", "ana@example.com", UserStatus.ACTIVE, CustomerStatus.ENABLED);
        buyer.setPrimaryAddress("1 Main Street");
        buyer.setPasswordHash("$2a$10$secret");

        UserResponse response = UserRestMapper.toResponse(buyer);

        assertEquals("BUYER", response.role());
        assertEquals("ENABLED", response.commercialStatus());
        assertEquals("1 Main Street", response.primaryAddress());
        assertNull(response.sellerStatus());
        boolean exposesHash = Arrays.stream(UserResponse.class.getRecordComponents())
                .anyMatch(component -> component.getName().toLowerCase().contains("password"));
        assertTrue(!exposesHash, "UserResponse must not expose any password field");
        assertTrue(!response.toString().contains("$2a$10$secret"));
    }

    @Test
    void sellerResponseCarriesTheSellerStatus() {
        Seller seller = new Seller("S-1", "Sam Seller", "sam@example.com", UserStatus.ACTIVE, SellerStatus.SUSPENDED);

        UserResponse response = UserRestMapper.toResponse(seller);

        assertEquals("SUSPENDED", response.sellerStatus());
        assertNull(response.commercialStatus());
    }

    @Test
    void newBuyerTakesTheRequestData() {
        Buyer buyer = UserRestMapper.newBuyer(
                new RegisterBuyerRequest("B-1", "Ana Buyer", "ana@example.com", "1 Main Street", "Strong-Pass-1"));

        assertEquals("B-1", buyer.getIdentifier());
        assertEquals("ana@example.com", buyer.getEmail());
        assertEquals("1 Main Street", buyer.getPrimaryAddress());
        assertNull(buyer.getPasswordHash());
    }

    @Test
    void requestsNeverPrintPasswords() {
        RegisterBuyerRequest request =
                new RegisterBuyerRequest("B-1", "Ana Buyer", "ana@example.com", "1 Main Street", "Strong-Pass-1");

        assertTrue(!request.toString().contains("Strong-Pass-1"));
    }

    @Test
    void newProductResolvesCodesMoneyAndVariants() {
        PublishProductRequest request = new PublishProductRequest("P-1", "T-shirt", "PHYSICAL",
                new MoneyRequest(new BigDecimal("49900.00"), "COP"), List.of(new VariantRequest("COLOR", "red")));

        Product product = CatalogRestMapper.newProduct(request);

        assertEquals(ProductType.PHYSICAL, product.getProductType());
        assertEquals(Money.of(new BigDecimal("49900"), Currency.COP), product.getPrice());
        assertEquals(VariantAttributeType.COLOR, product.getVariants().get(0).attribute());
        assertNull(product.getSeller());
    }

    @Test
    void unknownCatalogCodesAreClientErrors() {
        assertThrows(InvalidRequestException.class, () -> CatalogRestMapper.newProduct(new PublishProductRequest(
                "P-1", "T-shirt", "FROZEN", new MoneyRequest(BigDecimal.ONE, "COP"), null)));
        assertThrows(InvalidRequestException.class,
                () -> CatalogRestMapper.toMoney(new MoneyRequest(BigDecimal.ONE, "EUR")));
        assertThrows(InvalidRequestException.class,
                () -> CatalogRestMapper.toVariants(List.of(new VariantRequest("WEIGHT", "1kg"))));
        assertThrows(InvalidRequestException.class, () -> CatalogRestMapper.productStatus("GONE"));
        assertThrows(InvalidRequestException.class, () -> OrderRestMapper.orderStatus("LOST"));
    }

    @Test
    void missingVariantsMapToAnEmptyList() {
        assertTrue(CatalogRestMapper.toVariants(null).isEmpty());
    }

    @Test
    void warehouseOwnershipComesFromTheEndpoint() {
        Warehouse warehouse = WarehouseRestMapper.newWarehouse(
                new RegisterWarehouseRequest("WH-1", "North Warehouse", "1 Main Street"), WarehouseOwnerType.SELLER);

        assertEquals(WarehouseOwnerType.SELLER, warehouse.getOwnerType());
        assertNull(warehouse.getOwner());
        assertTrue(warehouse.isActive());
    }

    @Test
    void newShipmentAndReturnRequestReferenceTheirOrder() {
        Shipment shipment = ShipmentRestMapper.newShipment(new CreateShipmentRequest("SH-1", "O-1"));
        ReturnRequest returnRequest =
                ReturnRestMapper.newReturnRequest("O-1", new RequestReturnRequest("RR-1", "Wrong size"));

        assertEquals("O-1", shipment.getOrder().getIdentifier());
        assertEquals("O-1", returnRequest.getOrder().getIdentifier());
        assertEquals("Wrong size", returnRequest.getReason());
    }

    @Test
    void orderResponseExposesIdentifiersNotEntities() {
        Buyer buyer = new Buyer("B-1", "Ana Buyer", "ana@example.com", UserStatus.ACTIVE, CustomerStatus.ENABLED);
        Product product = new Product("P-1", "T-shirt", ProductType.PHYSICAL, null);
        Warehouse warehouse = new Warehouse("WH-1", "North Warehouse", "1 Main Street", WarehouseOwnerType.MARKETPLACE);
        OrderLine line = new OrderLine(product, "T-shirt", 2, Money.of(new BigDecimal("10"), Currency.USD), warehouse);
        Order order = new Order("O-1", buyer, OrderStatus.PAID, List.of(line), LocalDateTime.of(2026, 1, 1, 12, 0));

        OrderResponse response = OrderRestMapper.toResponse(order);

        assertEquals("B-1", response.buyerId());
        assertEquals("PAID", response.status());
        assertEquals("P-1", response.lines().get(0).productId());
        assertEquals("WH-1", response.lines().get(0).warehouseId());
        assertEquals("USD", response.lines().get(0).unitPrice().currency());
    }
}
