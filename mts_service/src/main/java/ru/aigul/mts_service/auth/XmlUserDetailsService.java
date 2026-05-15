package ru.aigul.mts_service.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

public class XmlUserDetailsService implements UserDetailsService {
    private final Map<String, String> users = new HashMap<>();
    private final Map<String, List<String>> roles = new HashMap<>();

    public XmlUserDetailsService() {
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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String pwd = users.get(username);
        if (pwd == null) throw new UsernameNotFoundException("User not found: " + username);
        List<GrantedAuthority> auth = new ArrayList<>();
        for (String r : roles.getOrDefault(username, Collections.singletonList("USER"))) {
            auth.add(new SimpleGrantedAuthority(r));
        }
        return new User(username, pwd, auth);
    }
}

