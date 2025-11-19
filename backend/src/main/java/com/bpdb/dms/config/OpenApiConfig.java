package com.bpdb.dms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dmsOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("DMS API")
                .description("Document Management System API - including DMC, Finance, and Search")
                .version("v1")
                .license(new License().name("Apache 2.0")));
    }
}


