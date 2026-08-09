package com.example.minitask_pacto_mais.security;

import com.example.minitask_pacto_mais.repository.UserRepository;
import com.example.minitask_pacto_mais.util.PhoneNormalizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class BotAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Bot-Api-Key";
    public static final String PHONE_HEADER = "X-WhatsApp-Phone";
    private static final String DEFAULT_COUNTRY = "55";

    private final BotProperties botProperties;
    private final UserRepository userRepository;

    public BotAuthenticationFilter(BotProperties botProperties, UserRepository userRepository) {
        this.botProperties = botProperties;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/bot/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!botProperties.isConfigured()) {
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Bot API key não configurada");
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || !apiKey.equals(botProperties.apiKey())) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "API key inválida");
            return;
        }

        String rawPhone = request.getHeader(PHONE_HEADER);
        String phone = PhoneNormalizer.normalize(rawPhone, DEFAULT_COUNTRY);
        if (phone == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Telefone WhatsApp ausente ou inválido");
            return;
        }

        var userOpt = userRepository.findByPhone(phone);
        if (userOpt.isEmpty()) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Usuário não encontrado para este telefone");
            return;
        }

        var user = userOpt.get();
        if (!user.isPhoneVerified()) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Telefone não verificado");
            return;
        }

        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPasswordHash(),
                user.getRole()
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private static void writeError(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"detail\":\"" + detail.replace("\"", "'") + "\"}");
    }
}
