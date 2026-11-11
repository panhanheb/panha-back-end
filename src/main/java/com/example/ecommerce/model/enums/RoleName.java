package com.example.ecommerce.model.enums;

/**
 * Roles (RBAC). {@code ROLE_SUPER_ADMIN} implies {@code ROLE_ADMIN} (see the role hierarchy in
 * {@link SecurityConfig}). Permissions per role are listed in docs/DESIGN.md, section 4.2.
 */
public enum RoleName {
    /** Manages administrators and roles; everything an admin can do. */
    ROLE_SUPER_ADMIN,
    /** Runs the store: catalog, orders, customers, coupons, reports. */
    ROLE_ADMIN,
    /** Marketplace seller: own products, own order lines, own sales. */
    ROLE_SELLER,
    /** Delivery rider: assigned shipments only. */
    ROLE_DELIVERY,
    /** Shops: cart, checkout, own orders, reviews. */
    ROLE_CUSTOMER
}
