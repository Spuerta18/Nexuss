package nexussMarket.adapters.in.rest.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nexussMarket.adapters.in.rest.mappers.UserRestMapper;
import nexussMarket.adapters.in.rest.requests.LoginRequest;
import nexussMarket.adapters.in.rest.requests.RegisterBuyerRequest;
import nexussMarket.adapters.in.rest.responses.AuthResponse;
import nexussMarket.adapters.in.rest.responses.UserResponse;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.PublicAccessPort;

/**
 * Public endpoints ({@link PublicAccessPort}): login and buyer
 * self-registration. {@code GET /api/me} returns the authenticated user's own
 * profile straight from the principal, without a use case.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthRestController {

    private final PublicAccessPort publicAccessPort;

    /** A wrong email and a wrong password both answer {@code 401} with the same generic message. */
    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return UserRestMapper.toResponse(publicAccessPort.authenticate(request.email(), request.password()));
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerBuyer(@Valid @RequestBody RegisterBuyerRequest request) {
        return UserRestMapper.toResponse(
                publicAccessPort.registerBuyer(UserRestMapper.newBuyer(request), request.password()));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal(expression = "user") User actor) {
        return UserRestMapper.toResponse(actor);
    }
}
