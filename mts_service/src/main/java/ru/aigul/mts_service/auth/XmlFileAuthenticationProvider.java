package ru.aigul.mts_service.auth;

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
import java.util.*;

public class XmlFileAuthenticationProvider implements AuthenticationProvider {
    private final Map<String, String> users = new HashMap<>();
    private final Map<String, List<String>> roles = new HashMap<>();

    public XmlFileAuthenticationProvider() {
        loadFromResource("users.xml");
    }

    private void loadFromResource(String resource) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (is == null) return;
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
                    rlist.add(roleNodes.item(j).getTextContent());
                }
                roles.put(username, rlist);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = (authentication.getPrincipal() == null) ? "" : authentication.getName();
        String password = (authentication.getCredentials() == null) ? "" : authentication.getCredentials().toString();

        String lookup = username;
        if (lookup.contains("@")) {
            lookup = lookup.substring(0, lookup.indexOf('@'));
        }

        String expected = users.get(username);
        if (expected == null) {
            // try normalized lookup
            expected = users.get(lookup);
        }
        if (expected == null || !expected.equals(password)) {
            throw new BadCredentialsException("Invalid credentials");
        }

        Set<GrantedAuthority> authorities = new HashSet<>();
        List<String> r = roles.getOrDefault(username, roles.getOrDefault(lookup, Collections.singletonList("USER")));
        for (String role : r) {
            authorities.add(new SimpleGrantedAuthority(role));
        }

        return new UsernamePasswordAuthenticationToken(lookup, null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
