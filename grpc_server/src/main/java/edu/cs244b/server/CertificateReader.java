package edu.cs244b.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.provider.X509Factory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

class CertificateReader {
    private static final Logger logger = LoggerFactory.getLogger(CertificateReader.class);

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

    public static X509Certificate getClientCertificateAuthorities(final String certBaseDirectory) {
        X509Certificate certificate = null;
        try {
            certificate = parseCertificate(certBaseDirectory + File.separator + CLIENT + File.separator + CERTIFICATE_FILENAME);
        } catch (Exception ex) {
            logger.info("Error while parsing certificate for client", ex);
        }
        return certificate;
    }

    public static X509Certificate getServerCertificateAuthorities(final String certBaseDirectory,
                                                                  final String peerName) {
        final String peerCertFilePath = certBaseDirectory + File.separator + TRUSTED_CONTACTS + File.separator + peerName + File.separator + CERTIFICATE_FILENAME;
        if (new File(peerCertFilePath).exists()) {
            try {
                return parseCertificate(peerCertFilePath);
            } catch (Exception ex) {
                logger.info("Error while parsing certificate for trusted contact {}", peerName, ex);
            }
        }

        return null;
    }

    private static X509Certificate parseCertificate(final String certFile) throws Exception {
        logger.info("Parsing certificate: {}", certFile);

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

        logger.info("Certificate: {}", cert);
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(decoded));
    }
}
