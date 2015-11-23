package org.pepsoft.worldpainter.tools;

import org.pepsoft.worldpainter.Version;
import org.pepsoft.worldpainter.browser.WPTrustManager;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

/**
 * Created by pepijn on 10-10-15.
 */
public class TestPing {
    public static void main(String[] args) throws CertificateException, NoSuchAlgorithmException, KeyManagementException, IOException {
        // Load and install trusted WorldPainter root certificate
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate trustedCert = (X509Certificate) certificateFactory.generateCertificate(TestPing.class.getResourceAsStream("/wproot.pem"));

        WPTrustManager trustManager = new WPTrustManager(trustedCert);
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TrustManager[] {trustManager}, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

        String httpAgent = "WorldPainter " + Version.VERSION + "; " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch") + ";";
        System.setProperty("http.agent", httpAgent);

        URL url = new URL("https://bo.worldpainter.net:1443/ping");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setAllowUserInteraction(false);
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        try (OutputStream out = new GZIPOutputStream(connection.getOutputStream())) {
            out.write("Test".getBytes(Charset.forName("US-ASCII")));
        }
        int responseCode = connection.getResponseCode();
        System.out.println("Response code: " + responseCode);
        System.out.println("Response body:");
        if (responseCode >= 400) {
            try (InputStreamReader in = new InputStreamReader(connection.getErrorStream(), "US-ASCII")) {
                char[] buffer = new char[32786];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    System.out.print(Arrays.copyOf(buffer, read));
                }
            }
        } else {
            try (InputStreamReader in = new InputStreamReader(connection.getInputStream(), "US-ASCII")) {
                char[] buffer = new char[32786];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    System.out.print(Arrays.copyOf(buffer, read));
                }
            }
        }
    }
}