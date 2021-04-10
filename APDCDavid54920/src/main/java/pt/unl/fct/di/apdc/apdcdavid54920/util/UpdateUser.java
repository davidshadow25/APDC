/**
 * 
 */
package pt.unl.fct.di.apdc.apdcdavid54920.util;

/**
 * @author davidpereira
 *
 */
public class UpdateUser {
	
	public String username;
	public String email;
	public String password;
	public String confirmation;
	public String role;
	public String state;
	public String profile;
	public String phoneFix;
	public String cellphone;
	public String address;
	public String locality;
	public String postalCode;
	
	public UpdateUser() {
		
	}
	
	public UpdateUser(String username, String email, String password, String confirmation, 
			String profile, String phoneFix, String cellphone, String address, String locality, String postalCode) {
		this.username = username;
		this.email = email;
		this.password = password;
		this.confirmation = confirmation;
		this.profile = profile;
		this.phoneFix = phoneFix;
		this.cellphone = cellphone;
		this.address = address;
		this.locality = locality;
		this.postalCode = postalCode;
		this.role = "USER";
		this.state = "ENABLED";
	}
	
	public void setRole(String role) {
		this.role = role;
	}
	
	public void setState(String state) {
		this.state = state;
	}

}
