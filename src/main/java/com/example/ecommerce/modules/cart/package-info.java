/**
 * The shopping cart of a signed-in customer.
 * <p>
 * Other modules may only use this module's {@code service} interfaces and {@code dto} records (its named
 * interfaces). Entities, repositories and implementations are internal.
 */
@ApplicationModule(displayName = "Cart")
package com.example.ecommerce.modules.cart;

import org.springframework.modulith.ApplicationModule;