/**
 * Order module. Depends on {@code inventory} module's public API only.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Order",
    allowedDependencies = {"inventory"}
)
package com.application.modulith.order;
