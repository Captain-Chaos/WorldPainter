/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.browser;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

/**
 *
 * @author pepijn
 */
public class WPTrustManager implements X509TrustManager {
    public WPTrustManager(X509Certificate trustedCert) {
        this.trustedCert = trustedCert;
    }
    
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        for (X509Certificate cert: chain) {
            cert.checkValidity();
            if (cert.equals(trustedCert)) {
//                System.out.println("Trusted certificate found!");
                return;
//            } else {
//                System.out.println("Untrusted certificate found!");
            }
//            System.out.println(cert);
        }
        throw new CertificateException("No certificate provided or certificate not trusted");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[] {trustedCert};
    }
    
    private final X509Certificate trustedCert;
}