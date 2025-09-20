package pl.tkowalcz.tjahzi.reload4j.infra;

import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public final class TruststoreUtil {

    public static void setupTruststoreSysProps(String certClasspathResource, String password) {
        try {
            File truststoreFile = createPkcs12TruststoreFromPem(certClasspathResource, password);
            System.setProperty("loki.truststore.path", truststoreFile.getAbsolutePath());
            System.setProperty("loki.truststore.password", password);

            // Leave type empty to exercise auto-detection based on .p12 extension
            System.setProperty("loki.truststore.type", "");
        } catch (Exception e) {
            Assertions.fail("Failed to prepare truststore: " + e.getMessage(), e);
        }
    }

    public static File createPkcs12TruststoreFromPem(String certClasspathResource, String password) throws Exception {
        try (InputStream is = TruststoreUtil.class.getClassLoader().getResourceAsStream(certClasspathResource)) {
            if (is == null) {
                throw new IllegalStateException("Certificate resource not found: " + certClasspathResource);
            }
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(is);

            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, null);
            ks.setCertificateEntry("server-cert", cert);

            File temp = Files.createTempFile("tjahzi-truststore", ".p12").toFile();
            try (FileOutputStream fos = new FileOutputStream(temp)) {
                ks.store(fos, password != null ? password.toCharArray() : new char[0]);
            }
            temp.deleteOnExit();
            return temp;
        }
    }

    public static File createJksTruststoreFromPem(String certClasspathResource, String password) throws Exception {
        try (InputStream is = TruststoreUtil.class.getClassLoader().getResourceAsStream(certClasspathResource)) {
            if (is == null) {
                throw new IllegalStateException("Certificate resource not found: " + certClasspathResource);
            }
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(is);

            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(null, null);
            ks.setCertificateEntry("server-cert", cert);

            File temp = Files.createTempFile("tjahzi-truststore", ".jks").toFile();
            try (FileOutputStream fos = new FileOutputStream(temp)) {
                ks.store(fos, password != null ? password.toCharArray() : new char[0]);
            }
            temp.deleteOnExit();
            return temp;
        }
    }
}
