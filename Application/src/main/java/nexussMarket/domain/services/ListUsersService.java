package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.PageQuery;
import nexussMarket.domain.valueobjects.SystemRole;

/** A page of users, optionally of one role. Read-only; administrators and supervisors only. */
@Service
public class ListUsersService {

    private final UserRepositoryPort userRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ListUsersService(UserRepositoryPort userRepositoryPort,
                            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.userRepositoryPort = userRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    /** {@code role} may be {@code null} for every role. */
    public ResultPage<User> execute(User actor, SystemRole role, PageQuery page) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR);
        return userRepositoryPort.findPage(role, page);
    }
}
