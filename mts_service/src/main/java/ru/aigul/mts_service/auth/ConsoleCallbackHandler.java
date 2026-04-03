package ru.aigul.mts_service.auth;

import javax.security.auth.callback.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleCallbackHandler implements CallbackHandler {
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        for (Callback callback : callbacks) {

            if (callback instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback) callback;
                System.out.print(nameCallback.getPrompt());
                String username = reader.readLine();
                nameCallback.setName(username);

            } else if (callback instanceof PasswordCallback) {
                PasswordCallback passwordCallback = (PasswordCallback) callback;
                System.out.print(passwordCallback.getPrompt());
                String password = reader.readLine();
                passwordCallback.setPassword(password.toCharArray());

            } else {
                throw new UnsupportedCallbackException(callback, "Unknown callback");
            }
        }
    }
}
