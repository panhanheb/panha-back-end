/**
 * Shared kernel: configuration, security, error handling and the response envelope. Every module may use
 * it; it must not depend on any module.
 */
@ApplicationModule(displayName = "Common", type = ApplicationModule.Type.OPEN)
package com.example.ecommerce.common;

import org.springframework.modulith.ApplicationModule;