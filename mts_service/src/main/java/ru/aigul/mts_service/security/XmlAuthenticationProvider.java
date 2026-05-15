package ru.aigul.mts_service.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class XmlAuthenticationProvider implements AuthenticationProvider {

    private final List<UserRecord> users = new ArrayList<>();

    public XmlAuthenticationProvider() {
        loadUsers();
    }

    private void loadUsers() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("users.xml")) {
            if (is == null) return;
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(is);
            NodeList nodes = doc.getElementsByTagName("user");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                String username = el.getAttribute("username");
                String password = el.getAttribute("password");
                String role = null;
                NodeList roles = el.getElementsByTagName("role");
                if (roles.getLength() > 0) {
                    role = roles.item(0).getTextContent();
                }
                if (username != null && !username.isBlank()) {
                    users.add(new UserRecord(username, password, role));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        if (username == null) username = "";
        String lookup = username;
        if (lookup.contains("@")) lookup = lookup.substring(0, lookup.indexOf('@'));
        Object cred = authentication.getCredentials();
        String password = cred == null ? null : cred.toString();
        UserRecord found = null;
        for (UserRecord u : users) {
            if (Objects.equals(u.username, username) || Objects.equals(u.username, lookup)) {
                found = u; break;
            }
        }
        if (found == null) throw new BadCredentialsException("Invalid credentials");
        if (found.password == null || !found.password.equals(password)) throw new BadCredentialsException("Invalid credentials");

        List<GrantedAuthority> auths = new ArrayList<>();
        if (found.role != null && !found.role.isBlank()) {
            String r = found.role.startsWith("ROLE_") ? found.role : "ROLE_" + found.role;
            auths.add(new SimpleGrantedAuthority(r));
            auths.add(new SimpleGrantedAuthority(found.role));
        } else {
            auths.add(new SimpleGrantedAuthority("ROLE_USER"));
            auths.add(new SimpleGrantedAuthority("USER"));
        }
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, null, auths);
        return token;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static class UserRecord {
        String username;
        String password;
        String role;
        UserRecord(String u, String p, String r) { username = u; password = p; role = r; }
    }
}
