package com.zhh.taskmanager.config; // 注意替换成你自己的包名

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 全局跨域配置：优先级高于普通的拦截器，确保 OPTIONS 预检请求顺利通过
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有域名跨域调用（前端 localhost:5173）
        config.addAllowedOriginPattern("*");
        // 允许所有请求头（包括你的 Authorization）
        config.addAllowedHeader("*");
        // 允许所有方法（GET, POST, PUT, DELETE, OPTIONS 等）
        config.addAllowedMethod("*");
        // 允许携带凭证
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有接口生效
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}