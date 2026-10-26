package nexussMarket.adapters.useCases;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.in.SellerPort;
import nexussMarket.domain.services.AdjustInventoryService;
import nexussMarket.domain.services.ChangeUserPasswordService;
import nexussMarket.domain.services.ConsultInventoryService;
import nexussMarket.domain.services.ConsultProductService;
import nexussMarket.domain.services.ListSellerProductsService;
import nexussMarket.domain.services.LogoutService;
import nexussMarket.domain.services.PublishProductService;
import nexussMarket.domain.services.RegisterInventoryInboundService;
import nexussMarket.domain.services.RegisterWarehouseService;
import nexussMarket.domain.services.UpdateProductStatusService;
import nexussMarket.domain.services.UpdateProductVariantsService;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductVariant;

/** Implements {@link SellerPort} by delegating each operation to its domain service. */
@Service
@RequiredArgsConstructor
public class SellerUseCaseImpl implements SellerPort {

    private final PublishProductService publishProductService;
    private final UpdateProductStatusService updateProductStatusService;
    private final UpdateProductVariantsService updateProductVariantsService;
    private final ListSellerProductsService listSellerProductsService;
    private final ConsultProductService consultProductService;
    private final RegisterWarehouseService registerWarehouseService;
    private final RegisterInventoryInboundService registerInventoryInboundService;
    private final AdjustInventoryService adjustInventoryService;
    private final ConsultInventoryService consultInventoryService;
    private final LogoutService logoutService;
    private final ChangeUserPasswordService changeUserPasswordService;

    @Override
    public Product publishProduct(User user, Product product) {
        return publishProductService.execute(user, product);
    }

    @Override
    public Product updateProductStatus(User user, Product product, ProductStatus status) {
        return updateProductStatusService.execute(user, product, status);
    }

    @Override
    public Product updateProductVariants(User user, Product product, List<ProductVariant> variants) {
        return updateProductVariantsService.execute(user, product, variants);
    }

    @Override
    public List<Product> listSellerProducts(User user, Seller seller) {
        return listSellerProductsService.execute(user, seller);
    }

    @Override
    public Product consultProduct(User user, Product product) {
        return consultProductService.execute(user, product);
    }

    @Override
    public Warehouse registerWarehouse(User user, Warehouse warehouse) {
        return registerWarehouseService.execute(user, warehouse);
    }

    @Override
    public InventoryItem registerInventoryInbound(User user, Product product, Warehouse warehouse, int quantity) {
        return registerInventoryInboundService.execute(user, product, warehouse, quantity);
    }

    @Override
    public InventoryItem adjustInventory(User user, Product product, Warehouse warehouse, int quantityDelta) {
        return adjustInventoryService.execute(user, product, warehouse, quantityDelta);
    }

    @Override
    public List<InventoryItem> consultInventory(User user, Product product) {
        return consultInventoryService.execute(user, product);
    }

    @Override
    public void logout(User user) {
        logoutService.execute(user);
    }

    @Override
    public void changePassword(User user, String currentPassword, String newPassword) {
        changeUserPasswordService.execute(user, currentPassword, newPassword);
    }
}
