package com.zhh.taskmanager.filter;

import com.zhh.taskmanager.utils.JwtUtils;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TokenCompatibleFilter implements Filter {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String token = httpRequest.getHeader("Authorization");

        // 1. 如果是登录或者刷新接口，直接放行
        if (path.contains("/auth/login") || path.contains("/auth/refresh")) {
            chain.doFilter(request, response);
            return;
        }

        // 2. 如果检测到前端传来的是新版加密的 JWT 令牌 (含有 '.')
        if (token != null && token.startsWith("SUCCESS_TOKEN_") && token.contains(".")) {
            try {
                // 解密提取用户真实 ID
                Long userId = jwtUtils.getUserIdFromToken(token);
                // 转换成老控制器的硬编码格式：SUCCESS_TOKEN_FOR_X
                final String compatibleToken = "SUCCESS_TOKEN_FOR_" + userId;

                // 🌟 核心魔法：包装原本的 request，强行把解密后的老 Token 替换进 Header
                HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(httpRequest) {
                    @Override
                    public String getHeader(String name) {
                        if ("Authorization".equalsIgnoreCase(name)) {
                            return compatibleToken;
                        }
                        return super.getHeader(name);
                    }
                };

                // 让带有兼容 Token 的请求继续往下走
                chain.doFilter(requestWrapper, response);
                return;
            } catch (ExpiredJwtException e) {
                // 🚨 关键：如果是 Access Token 过期了，直接向前端精准轰出 401 状态码！
                // 前端的 request.js 截获到 401 后，才能完美启动无感静默刷新逻辑！
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write("{\"message\":\"Token已过期，请进行无感刷新\"}");
                return;
            } catch (Exception e) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write("{\"message\":\"无效的身份凭证\"}");
                return;
            }
        }

        // 3. 其他请求（如老格式Token或没带Token）原样放行，交给老控制器自己去识别
        chain.doFilter(request, response);
    }
}