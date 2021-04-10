/**
 * 
 */
package pt.unl.fct.di.apdc.apdcdavid54920.util;

/**
 * @author davidpereira
 *
 */
public class UpdatePassword {
	
	public String oldPassword;
	public String newPassword;
	public String confirmation;
	
	public UpdatePassword() {
		
	}
	
	public UpdatePassword(String oldPassword, String newPassword, String confirmation) {
		this.oldPassword = oldPassword;
		this.newPassword = newPassword;
		this.confirmation = confirmation;
	}
	
	public boolean checkPassword() {
		return oldPassword != null && confirmation.equals(newPassword);
	}

}
