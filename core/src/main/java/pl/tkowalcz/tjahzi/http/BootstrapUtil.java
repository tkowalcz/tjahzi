package pl.tkowalcz.tjahzi.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.agrona.Strings;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public class BootstrapUtil {

    public static ChannelFuture initConnection(
            EventLoopGroup group,
            ClientConfiguration clientConfiguration,
            MonitoringModule monitoringModule
    ) {
        Bootstrap bootstrap = new Bootstrap();

        SslContext sslContext = null;
        if (clientConfiguration.isUseSSL()) {
            sslContext = createSslContext(clientConfiguration);
        }

        return bootstrap.group(group)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, clientConfiguration.getConnectionTimeoutMillis())
                .channel(NioSocketChannel.class)
                .handler(
                        new HttpClientInitializer(
                                monitoringModule,
                                sslContext,
                                clientConfiguration.getHost(),
                                clientConfiguration.getPort(),
                                clientConfiguration.getRequestTimeoutMillis(),
                                clientConfiguration.getMaxRequestsInFlight()
                        )
                )
                .remoteAddress(
                        clientConfiguration.getHost(),
                        clientConfiguration.getPort()
                ).connect();
    }

    private static SslContext createSslContext(ClientConfiguration configuration) {
        try {
            SslContextBuilder builder = SslContextBuilder.forClient();
            TrustManagerFactory tmf = buildTrustManagerFactory(configuration);
            builder.trustManager(tmf);

            return builder.build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    private static TrustManagerFactory buildTrustManagerFactory(ClientConfiguration configuration) {
        String trustStorePath = configuration.getTrustStorePath();
        String trustStorePassword = configuration.getTrustStorePassword();
        String trustStoreType = configuration.getTrustStoreType();

        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

            if (trustStorePath == null || trustStorePath.isEmpty()) {
                // Use default JVM trust store
                tmf.init((KeyStore) null);
                return tmf;
            }

            // Load custom trust store
            String ksType = !Strings.isEmpty(trustStoreType)
                    ? trustStoreType
                    : guessKeyStoreTypeFromPath(trustStorePath);

            KeyStore keyStore = KeyStore.getInstance(ksType);
            char[] passwordChars = trustStorePassword != null ? trustStorePassword.toCharArray() : null;
            try (FileInputStream fis = new FileInputStream(trustStorePath)) {
                keyStore.load(fis, passwordChars);
            }
            tmf.init(keyStore);
            return tmf;
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to initialize TrustManagerFactory for TLS. " + e.getMessage(), e);
        }
    }

    private static String guessKeyStoreTypeFromPath(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".p12") || lower.endsWith(".pfx")) {
            return "PKCS12";
        }

        // Default to JKS if not specified
        return KeyStore.getDefaultType();
    }
}
