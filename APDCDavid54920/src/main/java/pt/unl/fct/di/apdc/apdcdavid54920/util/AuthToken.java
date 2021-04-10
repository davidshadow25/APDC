/**
 * 
 */
package pt.unl.fct.di.apdc.apdcdavid54920.util;

import java.util.UUID;

/**
 * @author davidpereira
 *
 */
public class AuthToken {
	
	public String username;
	public String role;
	public String tokenID;
	public long creationDate;
	public long expirationDate;
	
	public static final long EXPIRATION_LIFE = 1000*60*60*2;
	
	public AuthToken() {
		
	}
	
	public AuthToken(String username) {
		this.username = username;
		this.tokenID = UUID.randomUUID().toString();
		this.creationDate = System.currentTimeMillis();
		this.expirationDate = this.creationDate + AuthToken.EXPIRATION_LIFE;
	}
	
	public AuthToken(String username, String role) {
		this.username = username;
		this.role = role;
		this.tokenID = UUID.randomUUID().toString();
		this.creationDate = System.currentTimeMillis();
		this.expirationDate = this.creationDate + AuthToken.EXPIRATION_LIFE;
	}
	
	public void setExpirationDate(long expirationDate) {
		this.expirationDate = expirationDate;
	}

}
