package ru.aigul.mts_service.auth;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

public class XmlLoginModule implements LoginModule {
    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, String> users = new HashMap<>();
    private Map<String, List<String>> roles = new HashMap<>();
    private boolean succeeded = false;
    private String loginName;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;

        String usersResource = (options != null && options.containsKey("usersResource")) ? options.get("usersResource").toString() : "users.xml";

        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(usersResource)) {
            if (is == null) {
                return;
            }
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();
            NodeList userNodes = doc.getElementsByTagName("user");
            for (int i = 0; i < userNodes.getLength(); i++) {
                Element u = (Element) userNodes.item(i);
                String username = u.getAttribute("username");
                String password = u.getAttribute("password");
                users.put(username, password);
                NodeList roleNodes = u.getElementsByTagName("role");
                List<String> rlist = new ArrayList<>();
                for (int j = 0; j < roleNodes.getLength(); j++) {
                    String role = roleNodes.item(j).getTextContent();
                    rlist.add(role);
                }
                roles.put(username, rlist);
            }
        } catch (Exception e) {
            // ignore, leave users empty
        }
    }

    @Override
    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("No callback handler available");
        }
        NameCallback nameCb = new NameCallback("username");
        PasswordCallback passCb = new PasswordCallback("password", false);
        try {
            callbackHandler.handle(new Callback[]{nameCb, passCb});
            String name = nameCb.getName();
            char[] pwd = passCb.getPassword();
            passCb.clearPassword();
            if (name == null) throw new LoginException("No username provided");
            String expected = users.get(name);
            if (expected != null && Arrays.equals(expected.toCharArray(), pwd)) {
                succeeded = true;
                loginName = name;
                return true;
            } else {
                throw new LoginException("Authentication failed");
            }
        } catch (Exception e) {
            LoginException le = new LoginException("Login failure: " + e.getMessage());
            le.initCause(e);
            throw le;
        }
    }

    @Override
    public boolean commit() throws LoginException {
        if (!succeeded) return false;
        subject.getPrincipals().add(new UserPrincipal(loginName));
        List<String> r = roles.getOrDefault(loginName, Collections.emptyList());
        for (String role : r) {
            subject.getPrincipals().add(new RolePrincipal(role));
        }
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        succeeded = false;
        loginName = null;
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().removeIf(p -> p instanceof UserPrincipal || p instanceof RolePrincipal);
        succeeded = false;
        loginName = null;
        return true;
    }
}

