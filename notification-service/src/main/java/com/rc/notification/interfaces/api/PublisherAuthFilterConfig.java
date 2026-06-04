package com.rc.notification.interfaces.api;

import com.rc.notification.domain.publisher.PublisherRepository;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublisherAuthFilterConfig {

    @Bean
    public FilterRegistrationBean<PublisherAuthFilter> publisherAuthFilterRegistration(
            PublisherRepository publisherRepository) {
        FilterRegistrationBean<PublisherAuthFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new PublisherAuthFilter(publisherRepository));
        reg.addUrlPatterns("/api/v2/*");
        reg.setOrder(2);
        return reg;
    }
}
