package pl.tkowalcz.tjahzi.http;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.ReadOnlyHttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

public class HttpHeadersFactory {

    public static HttpHeaders createHeaders(ClientConfiguration clientConfiguration, String[] additionalHeaders) {
        ArrayList<String> newHeaders = new ArrayList<>(Arrays.asList(additionalHeaders));

        Optional<String> maybeAuthString = createAuthString(
                clientConfiguration.getUsername(),
                clientConfiguration.getPassword()
        );

        maybeAuthString.ifPresent(
                authString -> {
                    newHeaders.add(HttpHeaderNames.AUTHORIZATION.toString());
                    newHeaders.add(authString);
                }
        );

        return new ReadOnlyHttpHeaders(
                true,
                newHeaders.toArray(new String[0])
        );
    }

    public static Optional<String> createAuthString(String username, String password) {
        if (username == null && password == null) {
            return Optional.empty();
        }

        String userPassword = username + ":" + password;
        String authString = "Basic " + Base64
                .getEncoder()
                .encodeToString(userPassword.getBytes(StandardCharsets.UTF_8));

        return Optional.of(authString);
    }
}
