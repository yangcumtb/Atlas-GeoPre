package com.example.atlasgeopre.common.config;

import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Swagger2的接口配置
 *
 * @author ruoyi
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public Docket createDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(getApiInfo())
                .host("localhost")
                .enable(true)//为true可以访问 false不能访问
                .select()//通过.select()方法，去配置扫描接口
                .apis(RequestHandlerSelectors.basePackage("com.example.atlasgeopre.controller"))//RequestHandlerSelectors配置如何扫描接口
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
               // .paths(PathSelectors.ant("/Atlas-GeoPre-1.0/imagePreprocessing/**"))//扫描以  /api开头的请求
                .build();
    }

    private ApiInfo getApiInfo() {
        return new ApiInfoBuilder()
                .title("接口文档")
                .description("接口文档，里面包含了所有的请求及参数信息")
                .contact(new Contact("atlas", "http://localhost", "1@qq.com"))
                .version("v1.0")
                .build();
    }

}
