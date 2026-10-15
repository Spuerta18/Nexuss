/**
 * Input ports exposed by the domain, one per role: {@code PublicAccessPort},
 * {@code BuyerPort}, {@code SellerPort}, {@code AdministratorPort},
 * {@code LogisticsOperatorPort} and {@code SupervisorPort}. Each port lists the
 * operations its role can execute; they are implemented in
 * {@code nexussMarket.adapters.useCases} and invoked by input adapters.
 */
package nexussMarket.domain.ports.in;
