package com.example.atlasgeopre.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").   //批量要拦截哪些接口  拦截所有的请求
                allowedOrigins("*"). //允许跨域的域名，可以用*表示允许任何域名使用  带有cookie时必须是具体的值，如http://localhost:8080
                allowedMethods("*"). //允许任何方法（GET,POST,PUT,DELETE,PATCH,OPTIONS）
                allowedHeaders("*"). //允许任何请求头
                allowCredentials(false). //允许带上cookie信息
                exposedHeaders(HttpHeaders.SET_COOKIE).maxAge(3600L); //maxAge(3600)表明在3600秒内，不需要再发送预检验请求，可以缓存该结果
    }
}
