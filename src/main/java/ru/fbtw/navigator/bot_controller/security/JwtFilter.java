package ru.fbtw.navigator.bot_controller.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import ru.fbtw.navigator.bot_controller.domain.Role;
import ru.fbtw.navigator.bot_controller.domain.User;
import ru.fbtw.navigator.bot_controller.service.UserService;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static ru.fbtw.navigator.bot_controller.security.JwtUtil.getTokenFromRequest;


@Component
@Slf4j
public class JwtFilter extends GenericFilterBean {


    private final JwtProvider jwtProvider;
    private final UserService userService;

    public JwtFilter(JwtProvider jwtProvider, UserService userService) {
        this.jwtProvider = jwtProvider;
        this.userService = userService;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        log.info("Starting filter...");
        String token = getTokenFromRequest((HttpServletRequest) servletRequest);

        if (token != null && jwtProvider.validateToken(token)) {
            String login = jwtProvider.getLoginFromToken(token);

            User user = userService.loadUserByUsername(login);
            if(user.getRoles().contains(Role.SERVICE)) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } else {
            log.info("Filter failed: wrong token");
        }
        filterChain.doFilter(servletRequest, servletResponse);

    }
}
