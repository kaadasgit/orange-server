package cn.orangeiot.mqtt.security;

import io.vertx.core.http.ServerWebSocket;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.vertx.core.net.NetSocket;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by giova_000 on 15/10/2015.
 */
public class CertInfo {

    private static Logger logger = LogManager.getLogger(CertInfo.class);

    private X509Certificate[] certs;

    public CertInfo(X509Certificate[] certs) {
        this.certs = certs;
    }

    public CertInfo(ServerWebSocket webSocket) {
        try {
            this.certs = webSocket.peerCertificateChain();
        } catch(SSLPeerUnverifiedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public CertInfo(NetSocket netSocket) {
        try {
            this.certs = netSocket.peerCertificateChain();
        } catch(SSLPeerUnverifiedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public CertInfo(String certPath) {
        try {
            FileInputStream file = new FileInputStream(certPath);
            X509Certificate cert = X509Certificate.getInstance(file);
            this.certs = new X509Certificate[]{cert};
        } catch(FileNotFoundException|CertificateException e) {
            logger.error(e.getMessage(), e);
        }
    }


    public String getTenant() {
        String tenant = null;
        if(certs!=null) {
            for (X509Certificate c : certs) {
                String dn = c.getSubjectDN().getName();// info del DEVICE/TENANT
                tenant = getTenantFromDN(dn);
                logger.debug("Cert Info - " + c.getSerialNumber() + " " + dn);
            }
        }
        logger.debug("Cert Info - tenant found: "+ tenant);
        return tenant;
    }

    private String getTenantFromDN(String dn) {
        String tenant = selectFromDN(dn, "CN");
        return tenant;
    }
    private String selectFromDN(String dn, String rdnType) {
        String value = null;
        try {
            LdapName ldapDN = new LdapName(dn);
            for (Rdn rdn : ldapDN.getRdns()) {
//                logger.info(rdn.getType() + " -> " + rdn.getValue());
                if(rdn.getType().equals(rdnType)) {
                    value = rdn.getValue().toString();
                }
            }
        } catch (InvalidNameException in) {
            in.printStackTrace();
        }
        return value;
    }
}
