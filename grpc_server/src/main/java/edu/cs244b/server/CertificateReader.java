package edu.cs244b.server;

import com.google.common.collect.Lists;
import sun.security.provider.X509Factory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

class CertificateReader {

    /*
     * openssl req -newkey rsa:2048 -new -nodes -x509 -days 3650 -keyout key.pem -out cert.pem
     * generate the private key and ssl certificate
     * server certificate: /var/cs244b.p2p.dns/ssl_cert/server
     * client certificate: /var/cs244b.p2p.dns/ssl_cert/client
     * trusted contacts certificate: /var/cs244b.p2p.dns/ssl_cert/trusted_contacts/<contact_id>
     */
    static final String CLIENT = "client";
    static final String SERVER = "server";
    static final String TRUSTED_CONTACTS = "trusted_contacts";
    static final String CERTIFICATE_FILENAME = "cert.pem";
    static final String KEY = "key.pem";

    public static File getCertificateChain(final String certBaseDirectory) {
        return new File(certBaseDirectory + File.separator + SERVER + File.separator + CERTIFICATE_FILENAME);
    }

    public static File getKey(final String certBaseDirectory) {
        return new File(certBaseDirectory + File.separator + SERVER + File.separator + KEY);
    }

    public static X509Certificate getClientCertificateAuthorities(final String certBaseDirectory) throws Exception {
        return parseCertificate(certBaseDirectory + File.separator + CLIENT + File.separator + CERTIFICATE_FILENAME);
    }

    public static List<X509Certificate> getServerCertificateAuthorities(final String certBaseDirectory) throws Exception {
        final List<X509Certificate> certificates = Lists.newArrayList();
        final String baseDirTrustedContacts = certBaseDirectory + File.separator + TRUSTED_CONTACTS;
        final File[] trustedContacts = new File(baseDirTrustedContacts).listFiles();
        if (trustedContacts != null) {
            for (final File trustedContact : trustedContacts) {
                if (trustedContact.isDirectory()) {
                    certificates.add(parseCertificate(trustedContact.getAbsolutePath() + File.separator + CERTIFICATE_FILENAME));
                }
            }
        }

        return certificates;
    }

    private static X509Certificate parseCertificate(final String certFile) throws Exception {
        // Read key from file
        final StringBuilder certStr = new StringBuilder();
        final BufferedReader br = new BufferedReader(new FileReader(certFile));
        String line;
        while ((line = br.readLine()) != null) {
            certStr.append(line).append("\n");
        }
        br.close();

        //before decoding we need to get rod off the prefix and suffix
        final String cert = certStr.toString()
                .replaceAll(X509Factory.BEGIN_CERT, "")
                .replaceAll(X509Factory.END_CERT, "");
        //byte [] decoded = Base64.getDecoder().decode(cert);
        byte [] decoded = Base64.getMimeDecoder().decode(cert);

        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(decoded));
    }
}
