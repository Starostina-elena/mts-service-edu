package ru.aigul.mts_service.auth;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.*;

public class XmlJaasAuthenticationProvider implements AuthenticationProvider {
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = (authentication.getPrincipal() == null) ? "" : authentication.getName();
        String password = (authentication.getCredentials() == null) ? "" : authentication.getCredentials().toString();

        CallbackHandler handler = callbacks -> {
            for (Callback cb : callbacks) {
                if (cb instanceof NameCallback) {
                    ((NameCallback) cb).setName(username);
                } else if (cb instanceof PasswordCallback) {
                    ((PasswordCallback) cb).setPassword(password.toCharArray());
                }
            }
        };

        Configuration config = new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                Map<String, Object> options = new HashMap<>();
                options.put("usersResource", "users.xml");
                AppConfigurationEntry entry = new AppConfigurationEntry(
                        XmlLoginModule.class.getName(),
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        options
                );
                return new AppConfigurationEntry[]{entry};
            }
        };

        try {
            LoginContext lc = new LoginContext("XmlLogin", null, handler, config);
            lc.login();
            Subject subject = lc.getSubject();
            Set<GrantedAuthority> authorities = new HashSet<>();
            subject.getPrincipals().forEach(p -> {
                if (p instanceof ru.aigul.mts_service.auth.RolePrincipal) {
                    String role = ((ru.aigul.mts_service.auth.RolePrincipal) p).getName();
                    if (role != null && !role.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role));
                        authorities.add(new SimpleGrantedAuthority(role));
                    }
                }
            });
            if (authorities.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                authorities.add(new SimpleGrantedAuthority("USER"));
            }
            return new UsernamePasswordAuthenticationToken(username, null, authorities);
        } catch (LoginException e) {
            throw new BadCredentialsException("Authentication failed", e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
