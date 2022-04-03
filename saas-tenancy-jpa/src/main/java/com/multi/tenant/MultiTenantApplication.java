package com.multi.tenant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "com.multi.tenant") // JPA
@SpringBootApplication
@EnableDiscoveryClient
public class MultiTenantApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiTenantApplication.class, args);
    }

}
