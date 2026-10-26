package nexussMarket.adapters.useCases;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.BuyerPort;
import nexussMarket.domain.services.AddCartLineService;
import nexussMarket.domain.services.CancelOrderService;
import nexussMarket.domain.services.ChangeUserPasswordService;
import nexussMarket.domain.services.ConfirmOrderService;
import nexussMarket.domain.services.ConsultCartService;
import nexussMarket.domain.services.ConsultOrderService;
import nexussMarket.domain.services.ConsultProductService;
import nexussMarket.domain.services.CreateShoppingCartService;
import nexussMarket.domain.services.ListBuyerOrdersService;
import nexussMarket.domain.services.ListPublishedProductsService;
import nexussMarket.domain.services.LogoutService;
import nexussMarket.domain.services.RemoveCartLineService;
import nexussMarket.domain.services.RequestReturnService;
import nexussMarket.domain.services.UpdateCartLineQuantityService;

/** Implements {@link BuyerPort} by delegating each operation to its domain service. */
@Service
@RequiredArgsConstructor
public class BuyerUseCaseImpl implements BuyerPort {

    private final CreateShoppingCartService createShoppingCartService;
    private final AddCartLineService addCartLineService;
    private final UpdateCartLineQuantityService updateCartLineQuantityService;
    private final RemoveCartLineService removeCartLineService;
    private final ConsultCartService consultCartService;
    private final ConfirmOrderService confirmOrderService;
    private final CancelOrderService cancelOrderService;
    private final ConsultOrderService consultOrderService;
    private final ListBuyerOrdersService listBuyerOrdersService;
    private final ListPublishedProductsService listPublishedProductsService;
    private final ConsultProductService consultProductService;
    private final RequestReturnService requestReturnService;
    private final LogoutService logoutService;
    private final ChangeUserPasswordService changeUserPasswordService;

    @Override
    public ShoppingCart createShoppingCart(User user, ShoppingCart cart) {
        return createShoppingCartService.execute(user, cart);
    }

    @Override
    public ShoppingCart addCartLine(User user, ShoppingCart cart, Product product, int quantity) {
        return addCartLineService.execute(user, cart, product, quantity);
    }

    @Override
    public ShoppingCart updateCartLineQuantity(User user, ShoppingCart cart, Product product, int quantity) {
        return updateCartLineQuantityService.execute(user, cart, product, quantity);
    }

    @Override
    public ShoppingCart removeCartLine(User user, ShoppingCart cart, Product product) {
        return removeCartLineService.execute(user, cart, product);
    }

    @Override
    public ShoppingCart consultCart(User user, ShoppingCart cart) {
        return consultCartService.execute(user, cart);
    }

    @Override
    public Order confirmOrder(User user, ShoppingCart cart, Order newOrder) {
        return confirmOrderService.execute(user, cart, newOrder);
    }

    @Override
    public Order cancelOrder(User user, Order order) {
        return cancelOrderService.execute(user, order);
    }

    @Override
    public Order consultOrder(User user, Order order) {
        return consultOrderService.execute(user, order);
    }

    @Override
    public List<Order> listBuyerOrders(User user, Buyer buyer) {
        return listBuyerOrdersService.execute(user, buyer);
    }

    @Override
    public List<Product> listPublishedProducts(User user) {
        return listPublishedProductsService.execute(user);
    }

    @Override
    public Product consultProduct(User user, Product product) {
        return consultProductService.execute(user, product);
    }

    @Override
    public ReturnRequest requestReturn(User user, ReturnRequest returnRequest) {
        return requestReturnService.execute(user, returnRequest);
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
