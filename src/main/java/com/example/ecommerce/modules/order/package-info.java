/**
 * Checkout, orders and the order lifecycle.
 * <p>
 * Other modules may only use this module's {@code service} interfaces and {@code dto} records (its named
 * interfaces). Entities, repositories and implementations are internal.
 */
@ApplicationModule(displayName = "Orders")
package com.example.ecommerce.modules.order;

import org.springframework.modulith.ApplicationModule;