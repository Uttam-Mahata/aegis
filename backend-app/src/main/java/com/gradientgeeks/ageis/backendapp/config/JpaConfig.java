package com.gradientgeeks.ageis.backendapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // This configuration enables JPA Auditing for @CreatedDate and @LastModifiedDate annotations
}