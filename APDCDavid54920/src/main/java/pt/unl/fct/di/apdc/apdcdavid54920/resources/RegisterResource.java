/**
 * 
 */
package pt.unl.fct.di.apdc.apdcdavid54920.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.apdcdavid54920.util.AuthToken;
import pt.unl.fct.di.apdc.apdcdavid54920.util.RegisterData;

/**
 * @author davidpereira
 *
 */
@Path("/register")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResource {

	/**
	 * A Logger Object
	 */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();

	public RegisterResource() { //Nothing to be done here

	}

	@POST
	@Path("/registerData")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doRegister(RegisterData data) {
		LOG.fine("Attempt to register user: " + data.username);

		if(!data.validRegistration())
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			if(user != null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("User already exists.").build();
			} else {
				user = Entity.newBuilder(userKey)
						.set("email", data.email)
						.set("password", DigestUtils.sha512Hex(data.password))
						.set("ROLE", "USER")
						.set("STATE", "ENABLED")
						.set("creation_data", Timestamp.now())
						.build();
				txn.put(user);
				LOG.info("User registered " + data.username);
				txn.commit();
				return Response.ok(g.toJson(data)).build();
			}
		} finally {
			if(txn.isActive())
				txn.rollback();
		}
	}

	@PUT
	@Path("/{username}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response deleteUser(@PathParam("username") String username, AuthToken token) {
		LOG.fine("Attempt to delete user: " + username);

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
		Entity user = datastore.get(userKey);
		Key tokenKey = datastore.newKeyFactory()
				.addAncestors(PathElement.of("User", username))
				.setKind("Token").newKey(token.tokenID);
		Entity tkn = datastore.get(tokenKey);
		
		if(Timestamp.now().getSeconds() < token.expirationDate && tkn != null && user.getString("ROLE").equals("USER")) {
			if(token.role.equals("USER") && username.equals(userKey.getNameOrId())) {
				datastore.delete(userKey, tokenKey);
				LOG.info("User " + username + " was deleted with success");
				return Response.ok().build();
			} else if(token.role.equals("GA") || token.role.equals("GBO")) {
				datastore.delete(userKey, tokenKey);
				LOG.info("User " + username + " was deleted with success");
				return Response.ok().build();
			}
		}
		return Response.status(Status.FORBIDDEN).build();
	}

}
