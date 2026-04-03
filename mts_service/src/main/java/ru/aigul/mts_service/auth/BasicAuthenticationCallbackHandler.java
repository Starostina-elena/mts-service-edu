package ru.aigul.mts_service.auth;

import java.io.IOException;

public class BasicAuthenticationCallbackHandler
        implements javax.security.auth.callback.CallbackHandler {

    @Override
    public void handle(javax.security.auth.callback.Callback[] callbacks)
            throws IOException, javax.security.auth.callback.UnsupportedCallbackException {

        for (javax.security.auth.callback.Callback callback : callbacks) {
            if (callback instanceof javax.security.auth.callback.NameCallback) {
                javax.security.auth.callback.NameCallback nameCallback =
                        (javax.security.auth.callback.NameCallback) callback;

                org.springframework.security.core.Authentication auth =
                        org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication();

                if (auth != null) {
                    nameCallback.setName(auth.getName());
                }

            } else if (callback instanceof javax.security.auth.callback.PasswordCallback) {
                javax.security.auth.callback.PasswordCallback passwordCallback =
                        (javax.security.auth.callback.PasswordCallback) callback;

                org.springframework.security.core.Authentication auth =
                        org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication();

                if (auth != null && auth.getCredentials() != null) {
                    passwordCallback.setPassword(
                            auth.getCredentials().toString().toCharArray()
                    );
                }
            }
        }
    }
}