/**
 * The category tree of the catalog.
 * <p>
 * Other modules may only use this module's {@code service} interfaces and {@code dto} records (its named
 * interfaces). Entities, repositories and implementations are internal.
 */
@ApplicationModule(displayName = "Categories")
package com.example.ecommerce.modules.category;

import org.springframework.modulith.ApplicationModule;