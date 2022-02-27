package com.multi.tenant.config;

import com.multi.tenant.multitenancy.TenantHandler;
import com.multi.tenant.multitenancy.TenantInterceptor;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;

@Configuration
public class JPAConfig {

    @Bean
    public JPAQueryFactory jpaQuery(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }

    /**
     * 创建租户维护处理器对象
     *
     * @return 处理后的租户维护处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantHandler tenantHandler() {
        return new TenantHandler();
    }


//    /**
//     * jpa 拦截器配置 多租户支持
//     *
//     * @return MybatisPlusInterceptor
//     */
//    @Bean
//    public void mybatisPlusInterceptor(TenantHandler tenantHandler) {
//        TenantInterceptor interceptor = new TenantInterceptor();
//        interceptor.setTenantLineHandler(tenantHandler);
//
//    }

}
