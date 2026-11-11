/**
 * Registration, sign-in, tokens and password recovery.
 * <p>
 * Other modules may only use this module's {@code service} interfaces and {@code dto} records (its named
 * interfaces). Entities, repositories and implementations are internal.
 */
@ApplicationModule(displayName = "Auth")
package com.example.ecommerce.modules.auth;

import org.springframework.modulith.ApplicationModule;