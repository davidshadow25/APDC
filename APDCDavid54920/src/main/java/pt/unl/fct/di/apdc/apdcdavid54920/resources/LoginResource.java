/**
 * 
 */
package pt.unl.fct.di.apdc.apdcdavid54920.resources;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
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
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.apdcdavid54920.util.AuthToken;
import pt.unl.fct.di.apdc.apdcdavid54920.util.LoginData;
import pt.unl.fct.di.apdc.apdcdavid54920.util.UpdatePassword;
import pt.unl.fct.di.apdc.apdcdavid54920.util.UpdateUser;

/**
 * @author davidpereira
 *
 */
@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

	/**
	 * A Logger Object
	 */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();

	public LoginResource() { // Nothing to be done here

	}
	
	@POST
	@Path("/localLogin")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLoginLocal(LoginData data) {
		LOG.fine("Login attempt by user: " + data.username);
		
		if(data.username.equals("Administrador") && DigestUtils.sha512Hex(data.password).equals(DigestUtils.sha512Hex("Davidshadow"))) {
			AuthToken token = new AuthToken(data.username);
			LOG.info("User '" + data.username + "' logged in successfully");
			return Response.ok(g.toJson(token)).build();
		}
		return Response.status(Status.FORBIDDEN).entity("Incorrect username or password.").build();
	}
	

	@POST
	@Path("/loginData")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLogin(LoginData data) {
		LOG.fine("Attempt to login user: " + data.username);

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			if(user == null) {
				// Username does not exist
				LOG.warning("Failed login attempt for username: " + data.username);
				return Response.status(Status.FORBIDDEN).build();
			} else {
				String hashedPWD = user.getString("password");
				if(hashedPWD.equals(DigestUtils.sha512Hex(data.password)) && user.getString("STATE").equals("ENABLED")) {
					// Password is correct
					AuthToken token = new AuthToken(data.username, user.getString("ROLE"));

					Key tokenKey = datastore.newKeyFactory()
							.addAncestors(PathElement.of("User", data.username))
							.setKind("Token").newKey(token.tokenID);

					Entity otherToken = txn.get(tokenKey);
					if(otherToken == null) {
						otherToken = Entity.newBuilder(tokenKey)
								.set("ROLE", token.role)
								.set("creationDate", token.creationDate)
								.set("expirationDate", token.expirationDate)
								.build();
					}
					txn.put(otherToken);
					txn.commit();

					LOG.info("User '" + data.username + "' logged in successfully");
					return Response.ok(g.toJson(token)).build();
				} else {
					// Incorrect password
					txn.commit();
					LOG.warning("Wrong password for username: " + data.username);
					return Response.status(Status.FORBIDDEN).build();
				}
			}
		} catch (Exception e) {
			txn.rollback();
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}

	@PUT
	@Path("/tokenId")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLogout(AuthToken token) {
		LOG.info("Attempt to logout user: " + token.username);

		Key tokenKey = datastore.newKeyFactory()
				.addAncestors(PathElement.of("User", token.username))
				.setKind("Token").newKey(token.tokenID);
		Entity otherToken = datastore.get(tokenKey);
		if(otherToken == null) {
			LOG.info("Token does not exist.");
			return Response.status(Status.FORBIDDEN).build();
		}

		if(token.expirationDate > Timestamp.now().getSeconds() && otherToken.getLong("expirationDate") > Timestamp.now().getSeconds()) {
			Entity newToken = Entity.newBuilder(otherToken)
					.set("expirationDate", Timestamp.now().getSeconds())
					.build();
			Transaction txn = datastore.newTransaction();
			try {
				txn.put(newToken);
				txn.commit();
				return Response.ok().build();
			} catch (Exception e) {
				txn.rollback();
				LOG.severe(e.getMessage());
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			} finally {
				if(txn.isActive()) {
					txn.rollback();
					return Response.status(Status.INTERNAL_SERVER_ERROR).build();
				}
			}

		}

		return Response.status(Status.FORBIDDEN).build();
	}

	@POST
	@Path("/updateUser")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateUser(UpdateUser data) {
		LOG.info("Attempt to update user: " + data.username);
		
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			if(user == null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("User does not exist.").build();
			} else {
				user = Entity.newBuilder(userKey)
						.set("email", data.email)
						.set("password", DigestUtils.sha512Hex(data.password))
						.set("profile", data.profile)
						.set("phoneFix", "+351 "+data.phoneFix)
						.set("cellphone", "+351 "+data.cellphone)
						.set("address", data.address)
						.set("locality", data.locality)
						.set("postalCode", data.postalCode)
						.set("ROLE", user.getString("ROLE"))
						.set("STATE", user.getString("STATE"))
						.set("creation_data", Timestamp.now())
						.build();
				txn.put(user);
				LOG.info("User updated " + data.username);
				txn.commit();
				return Response.ok(g.toJson(data)).build();

			}
		} finally {
			if(txn.isActive())
				txn.rollback();
		}
	}

	@POST
	@Path("/{username}/{role}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateRole(@PathParam("username") String username, @PathParam("role") String role, AuthToken token) {
		LOG.info("Attempt to update role from user: " + username);

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
		Entity user = datastore.get(userKey);
		
		if(Timestamp.now().getSeconds() < token.expirationDate) {
			if((token.role.equals("GA") || token.role.equals("SU")) && role.equals("GBO")) {
				user = Entity.newBuilder(userKey)
						.set("email", user.getString("email"))
						.set("password", user.getString("password"))
						.set("profile", user.getString("profile"))
						.set("phoneFix", user.getString("phoneFix"))
						.set("cellphone", user.getString("cellphone"))
						.set("address", user.getString("address"))
						.set("locality", user.getString("locality"))
						.set("postalCode", user.getString("postalCode"))
						.set("ROLE", role)
						.set("STATE", user.getString("STATE"))
						.set("creation_data", Timestamp.now())
						.build();
				datastore.put(user);
				LOG.info("User role update was successfull.");
				return Response.ok().build();
			} else if(token.role.equals("SU") && (role.equals("GBO") || role.equals("GA"))) {
				user = Entity.newBuilder(userKey)
						.set("email", user.getString("email"))
						.set("password", user.getString("password"))
						.set("profile", user.getString("profile"))
						.set("phoneFix", user.getString("phoneFix"))
						.set("cellphone", user.getString("cellphone"))
						.set("address", user.getString("address"))
						.set("locality", user.getString("locality"))
						.set("postalCode", user.getString("postalCode"))
						.set("ROLE", role)
						.set("STATE", user.getString("STATE"))
						.set("creation_data", Timestamp.now())
						.build();
				datastore.put(user);
				LOG.info("User role update was successfull.");
				return Response.ok().build();
			}
		}
		return Response.status(Status.FORBIDDEN).build();
	}

	@PUT
	@Path("/{username}/{state}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateState(@PathParam("username") String username, @PathParam("state") String state, AuthToken token) {
		LOG.info("Attempt to update state from: " + username);

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
		Entity user = datastore.get(userKey);
		
		if(Timestamp.now().getSeconds() < token.expirationDate) {
			if(token.role.equals("USER")) {
				LOG.info("User with role USER cannot change state.");
				return Response.status(Status.FORBIDDEN).build();
			} else if((token.role.equals("GBO") || token.role.equals("GA") || token.role.equals("SU"))
					&& user.getString("ROLE").equals("USER")) {
				user = Entity.newBuilder(userKey)
						.set("email", user.getString("email"))
						.set("password", user.getString("password"))
						.set("profile", user.getString("profile"))
						.set("phoneFix", user.getString("phoneFix"))
						.set("cellphone", user.getString("cellphone"))
						.set("address", user.getString("address"))
						.set("locality", user.getString("locality"))
						.set("postalCode", user.getString("postalCode"))
						.set("ROLE", user.getString("ROLE"))
						.set("STATE", state)
						.set("creation_data", Timestamp.now())
						.build();
				datastore.put(user);
				LOG.info("User role update was successfull.");
				return Response.ok().build();
			} else if((token.role.equals("GA") || token.role.equals("SU")) && user.getString("ROLE").equals("GBO")) {
				user = Entity.newBuilder(userKey)
						.set("email", user.getString("email"))
						.set("password", user.getString("password"))
						.set("profile", user.getString("profile"))
						.set("phoneFix", user.getString("phoneFix"))
						.set("cellphone", user.getString("cellphone"))
						.set("address", user.getString("address"))
						.set("locality", user.getString("locality"))
						.set("postalCode", user.getString("postalCode"))
						.set("ROLE", user.getString("ROLE"))
						.set("STATE", state)
						.set("creation_data", Timestamp.now())
						.build();
				datastore.put(user);
				LOG.info("User role update was successfull.");
				return Response.ok().build();
			} else if(token.role.equals("SU") && user.getString("ROLE").equals("GA")) {
				user = Entity.newBuilder(userKey)
						.set("email", user.getString("email"))
						.set("password", user.getString("password"))
						.set("profile", user.getString("profile"))
						.set("phoneFix", user.getString("phoneFix"))
						.set("cellphone", user.getString("cellphone"))
						.set("address", user.getString("address"))
						.set("locality", user.getString("locality"))
						.set("postalCode", user.getString("postalCode"))
						.set("ROLE", user.getString("ROLE"))
						.set("STATE", state)
						.set("creation_data", Timestamp.now())
						.build();
				datastore.put(user);
				LOG.info("User role update was successfull.");
				return Response.ok().build();
			}
		}
		return Response.status(Status.FORBIDDEN).build();

	}

	@POST
	@Path("/{username}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updatePassword(@PathParam("username") String username, UpdatePassword data) {
		LOG.info("Attempt to update password from: " + username);
		
		if(!data.checkPassword())
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter or wrong password.").build();

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			if(user == null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("User does not exist.").build();
			} else if(user.getString("ROLE").equals("USER") || user.getString("ROLE").equals("GA")) {
				user = Entity.newBuilder(userKey)
						.set("email", user.getString("email"))
						.set("password", DigestUtils.sha512Hex(data.newPassword))
						.set("profile", user.getString("profile"))
						.set("phoneFix", user.getString("phoneFix"))
						.set("cellphone", user.getString("cellphone"))
						.set("address", user.getString("address"))
						.set("locality", user.getString("locality"))
						.set("postalCode", user.getString("postalCode"))
						.set("ROLE", user.getString("ROLE"))
						.set("STATE", user.getString("STATE"))
						.set("creation_data", Timestamp.now())
						.build();
				txn.put(user);
				LOG.info("Password updated " + username);
				txn.commit();
				return Response.ok(g.toJson(data)).build();
			} else {
				return Response.status(Status.FORBIDDEN).build();
			}
		} finally {
			if(txn.isActive())
				txn.rollback();
		}
	}

	@POST
	@Path("/user")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response checkUsernameAvailable(LoginData data) {
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Entity user = datastore.get(userKey);
		if(user != null && user.getString("password").equals(DigestUtils.sha512Hex(data.password)) && user.getString("ROLE").equals("GBO")) {

			// Get the date of yesterday
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, -15);
			Timestamp last15 = Timestamp.of(cal.getTime());
			Calendar cal2 = Calendar.getInstance();
			cal2.add(Calendar.DATE, 0);
			Timestamp next15 = Timestamp.of(cal2.getTime());

			Query<Entity> query = Query.newEntityQueryBuilder()
					.setKind("User")
					.setFilter(
							CompositeFilter.and(
									PropertyFilter.ge("creation_data", last15),
									PropertyFilter.ge("creation_data", next15)
									)
							)
					.build();

			QueryResults<Entity> logs = datastore.run(query);

			List<Date> users = new ArrayList<>();
			logs.forEachRemaining(userlog -> {
				users.add(userlog.getTimestamp("creation_data").toDate());
			});

			return Response.ok(g.toJson(users)).build();
		} else {
			return Response.ok().entity(g.toJson(false)).build();
		}
	}

}
