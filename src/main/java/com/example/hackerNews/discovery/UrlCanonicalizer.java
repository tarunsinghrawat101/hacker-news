package com.example.hackerNews.discovery;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class UrlCanonicalizer {
    private static final Set<String> TRACKING_KEYS = new HashSet<>(Arrays.asList(
            "fbclid", "gclid", "mc_cid", "mc_eid", "ref", "source"));

    private UrlCanonicalizer() { }

    public static String canonicalize(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty())
            return "";

        String candidate = rawUrl.trim();
        if (!candidate.matches("(?i)^https?://.*"))
            candidate = "https://" + candidate;

        try {
            URI uri = new URI(candidate).normalize();
            String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost();
            if (host == null)
                return candidate.toLowerCase(Locale.ROOT);

            host = host.toLowerCase(Locale.ROOT);
            if (host.startsWith("www."))
                host = host.substring(4);

            int port = uri.getPort();
            String path = uri.getRawPath();
            if (path == null || path.isEmpty())
                path = "/";

            if (path.length() > 1 && path.endsWith("/"))
                path = path.substring(0, path.length() - 1);

            List<String> query = new ArrayList<>();
            if (uri.getRawQuery() != null) {
                for (String pair : uri.getRawQuery().split("&")) {
                    String key = pair.split("=", 2)[0];
                    String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);
                    String lower = decodedKey.toLowerCase(Locale.ROOT);
                    if (lower.startsWith("utm_") || TRACKING_KEYS.contains(lower))
                        continue;

                    query.add(pair);
                }
            }
            Collections.sort(query);
            String authority = host + ((port == -1 || (port == 80 && scheme.equals("http"))
                    || (port == 443 && scheme.equals("https"))) ? "" : ":" + port);

            return scheme + "://" + authority + path + (query.isEmpty() ? "" : "?" + String.join("&", query));
        } catch (URISyntaxException | IllegalArgumentException ex) {
            return candidate.toLowerCase(Locale.ROOT);
        }
    }
}
