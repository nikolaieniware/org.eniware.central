/* ==================================================================
 *  Eniware Open Source:Nikolai Manchev
 *  Apache License 2.0
 * ==================================================================
 */

package org.eniware.central.user.domain;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.eniware.central.domain.Entity;
import org.eniware.central.domain.EniwareEdge;
import org.eniware.support.CertificateException;
import org.eniware.util.SerializeIgnore;
import org.joda.time.DateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A user Edge certificate. The certificate is expected to be in X.509 format.
 * 
 * @version 1.2
 */
public class UserEdgeCertificate implements Entity<UserEdgePK>, Cloneable, Serializable {

	private static final long serialVersionUID = 3070315335910395052L;

	/** The expected format of the keystore data. */
	public static final String KEYSTORE_TYPE = "pkcs12";

	/** The alias of the Edge certificate in the keystore. */
	public static final String KEYSTORE_Edge_ALIAS = "Edge";

	private UserEdgePK id = new UserEdgePK();
	private DateTime created;
	private byte[] keystoreData;
	private UserEdgeCertificateStatus status;
	private String requestId;

	private User user;
	private EniwareEdge Edge;

	/**
	 * Get the Edge certificate from a keystore. The certificate is expected to
	 * be available on the {@link #KEYSTORE_Edge_ALIAS} alias.
	 * 
	 * @param keyStore
	 *        the keystore
	 * @return the certificate, or <em>null</em> if not available
	 */
	public X509Certificate getEdgeCertificate(KeyStore keyStore) {
		X509Certificate EdgeCert;
		try {
			EdgeCert = (X509Certificate) keyStore.getCertificate(KEYSTORE_Edge_ALIAS);
		} catch ( KeyStoreException e ) {
			throw new CertificateException("Error opening Edge certificate", e);
		}
		return EdgeCert;
	}

	/**
	 * Get the Edge certificate chain from a keystoer. The certificate is
	 * expected to be available on the {@link #KEYSTORE_Edge_ALIAS} alias.
	 * 
	 * @param keyStore
	 *        the keystore
	 * @return the certificate chain, or <em>null</em> if not available
	 */
	public X509Certificate[] getEdgeCertificateChain(KeyStore keyStore) {
		Certificate[] chain;
		try {
			chain = keyStore.getCertificateChain(KEYSTORE_Edge_ALIAS);
		} catch ( KeyStoreException e ) {
			throw new CertificateException("Error opening Edge certificate", e);
		}
		if ( chain == null || chain.length < 1 ) {
			return null;
		}
		X509Certificate[] x509Chain = new X509Certificate[chain.length];
		for ( int i = 0; i < chain.length; i++ ) {
			assert chain[i] instanceof X509Certificate;
			x509Chain[i] = (X509Certificate) chain[i];
		}
		return x509Chain;
	}

	/**
	 * Open the key store from {@link #getKeystoreData()}.
	 * 
	 * @param password
	 *        the password to use to open, or <em>null</em> for no password
	 * @return the KeyStore
	 */
	public KeyStore getKeyStore(String password) {
		KeyStore keyStore = null;
		InputStream in = null;
		if ( keystoreData != null ) {
			in = new ByteArrayInputStream(keystoreData);
		}
		try {
			keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
			keyStore.load(in, (password == null ? null : password.toCharArray()));
			return keyStore;
		} catch ( KeyStoreException e ) {
			throw new CertificateException("Error loading certificate key store", e);
		} catch ( NoSuchAlgorithmException e ) {
			throw new CertificateException("Error loading certificate key store", e);
		} catch ( java.security.cert.CertificateException e ) {
			throw new CertificateException("Error loading certificate key store", e);
		} catch ( IOException e ) {
			String msg;
			if ( e.getCause() instanceof UnrecoverableKeyException ) {
				msg = "Invalid password loading key store";
			} else {
				msg = "Error loading certificate key store";
			}
			throw new CertificateException(msg, e);
		} finally {
			if ( in != null ) {
				try {
					in.close();
				} catch ( IOException e ) {
					// ignore this one
				}
			}
		}
	}

	/**
	 * Convenience getter for {@link UserEdgePK#getEdgeId()}.
	 * 
	 * @return the EdgeId
	 */
	public Long getEdgeId() {
		return (id == null ? null : id.getEdgeId());
	}

	/**
	 * Convenience setter for {@link UserEdgePK#setEdgeId(Long)}.
	 * 
	 * @param EdgeId
	 *        the EdgeId to set
	 */
	public void setEdgeId(Long EdgeId) {
		if ( id == null ) {
			id = new UserEdgePK();
		}
		id.setEdgeId(EdgeId);
	}

	/**
	 * Convenience getter for {@link UserEdgePK#getUserId()}.
	 * 
	 * @return the userId
	 */
	public Long getUserId() {
		return (id == null ? null : id.getUserId());
	}

	/**
	 * Convenience setter for {@link UserEdgePK#setUserId(String)}.
	 * 
	 * @param userId
	 *        the userId to set
	 */
	public void setUserId(Long userId) {
		if ( id == null ) {
			id = new UserEdgePK();
		}
		id.setUserId(userId);
	}

	@JsonIgnore
	@SerializeIgnore
	@Override
	public UserEdgePK getId() {
		return id;
	}

	public void setId(UserEdgePK id) {
		this.id = id;
	}

	@Override
	public int compareTo(UserEdgePK o) {
		return id.compareTo(o);
	}

	@Override
	protected Object clone() {
		try {
			return super.clone();
		} catch ( CloneNotSupportedException e ) {
			// should not get here
			return null;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		UserEdgeCertificate other = (UserEdgeCertificate) obj;
		if ( id == null ) {
			if ( other.id != null ) {
				return false;
			}
		} else if ( !id.equals(other.id) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "UserEdgeCertificate{" + id + "}";
	}

	@JsonIgnore
	@SerializeIgnore
	public byte[] getKeystoreData() {
		return keystoreData;
	}

	public void setKeystoreData(byte[] keystoreData) {
		this.keystoreData = keystoreData;
	}

	public UserEdgeCertificateStatus getStatus() {
		return status;
	}

	public void setStatus(UserEdgeCertificateStatus status) {
		this.status = status;
	}

	@JsonIgnore
	@SerializeIgnore
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	@JsonIgnore
	@SerializeIgnore
	public EniwareEdge getEdge() {
		return Edge;
	}

	public void setEdge(EniwareEdge Edge) {
		this.Edge = Edge;
	}

	/**
	 * Get an external certificate request ID, to be used when a certificate
	 * status is pending.
	 * 
	 * @return the request ID
	 * @since 1.1
	 */
	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestID) {
		this.requestId = requestID;
	}

	@Override
	public DateTime getCreated() {
		return created;
	}

	public void setCreated(DateTime created) {
		this.created = created;
	}

}
