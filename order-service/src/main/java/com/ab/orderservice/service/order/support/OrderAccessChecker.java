package com.ab.orderservice.service.order.support;

import com.ab.orderservice.exception.ForbiddenException;
import com.ab.orderservice.exception.enums.ErrorCode;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.security.SecurityUser;
import org.springframework.stereotype.Component;

@Component
public class OrderAccessChecker {

    // Requires verified user and token userId == requested userId.
    public void requireVerifiedSameUser(Long requestedUserId) {
        if (!SecurityUser.verified()) {
            throw new ForbiddenException(ErrorCode.USER_NOT_VERIFIED);
        }

        Long tokenUserId = SecurityUser.userId();
        if (tokenUserId == null || !tokenUserId.equals(requestedUserId)) {
            throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
        }
    }

    // Requires order ownership unless user is ADMIN.
    public void requireOwnerOrAdmin(Order order, Long currentUserId, boolean isAdmin) {
        if (isAdmin) return;

        Long ownerId = order.getUserId();
        if (ownerId == null || !ownerId.equals(currentUserId)) {
            throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
        }
    }
}
