package com.notifyengine.config;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateMultiTenancyConfig {

    private final TenantIdentifierResolver tenantIdentifierResolver;
    private final SchemaMultiTenantConnectionProvider connectionProvider;

    public HibernateMultiTenancyConfig(TenantIdentifierResolver tenantIdentifierResolver,
                                       SchemaMultiTenantConnectionProvider connectionProvider) {
        this.tenantIdentifierResolver = tenantIdentifierResolver;
        this.connectionProvider = connectionProvider;
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return properties -> {
            properties.put("hibernate.multiTenancy", "SCHEMA");
            properties.put("hibernate.tenant_identifier_resolver", tenantIdentifierResolver);
            properties.put("hibernate.multi_tenant_connection_provider", connectionProvider);
        };
    }
}
