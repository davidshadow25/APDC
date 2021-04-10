/**
 * 
 */
package pt.unl.fct.di.apdc.apdcdavid54920.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author davidpereira
 *
 */
public class RegisterData {
	
	public String username;
	public String email;
	public String password;
	public String confirmation;
	public String role;
	public String state;
	public String creation_data;
	
	private static final DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
	
	public RegisterData() {
		this.creation_data = fmt.format(new Date());
	}

	public RegisterData(String username, String email, String password, String confirmation) {
		this.username = username;
		this.email = email;
		this.password = password;
		this.confirmation = confirmation;
	}

	public boolean validRegistration() {
		// TODO Auto-generated method stub
		return username != null && confirmation.equals(password) && email != null;
	}
	
	

}
