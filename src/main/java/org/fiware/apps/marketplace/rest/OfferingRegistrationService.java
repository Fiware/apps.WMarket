package org.fiware.apps.marketplace.rest;

import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.fiware.apps.marketplace.bo.ServiceBo;
import org.fiware.apps.marketplace.bo.StoreBo;
import org.fiware.apps.marketplace.exceptions.ServiceNotFoundException;
import org.fiware.apps.marketplace.exceptions.StoreNotFoundException;
import org.fiware.apps.marketplace.exceptions.UserNotFoundException;
import org.fiware.apps.marketplace.model.User;
import org.fiware.apps.marketplace.model.Service;
import org.fiware.apps.marketplace.model.Services;
import org.fiware.apps.marketplace.model.Store;
import org.fiware.apps.marketplace.security.auth.AuthUtils;
import org.fiware.apps.marketplace.security.auth.OfferingRegistrationAuth;
import org.fiware.apps.marketplace.utils.ApplicationContextProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;

@Path("/store/{storeName}/offering")
public class OfferingRegistrationService {

	// OBJECT ATTRIBUTES //
	private ApplicationContext context = ApplicationContextProvider.getApplicationContext();	
	private StoreBo storeBo = (StoreBo) context.getBean("storeBo");
	private ServiceBo serviceBo = (ServiceBo) context.getBean("serviceBo");
	private OfferingRegistrationAuth offeringRegistrationAuth = (OfferingRegistrationAuth) 
			context.getBean("offeringRegistrationAuth");

	// CLASS ATTRIBUTES //
	private static final AuthUtils AUTH_UTILS = AuthUtils.getAuthUtils();
	private static final ErrorUtils ERROR_UTILS = new ErrorUtils(
			"There is already an Offering in this Store with that name/URL");	

	@POST
	@Consumes({"application/xml", "application/json"})
	@Path("/")	
	public Response saveService(@PathParam("storeName") String storeName, Service service) {	
		Response response;

		if (offeringRegistrationAuth.canCreate()) {
			try {
				User user = AUTH_UTILS.getLoggedUser();
				Store store = storeBo.findByName(storeName);
				service.setRegistrationDate(new Date());
				service.setStore(store);
				service.setCreator(user);
				service.setLasteditor(user);

				serviceBo.save(service);
				response = Response.status(Status.CREATED).build();
			} catch (StoreNotFoundException ex) {
				//The Store is an URL... If the Store does not exist a 404
				//should be returned instead of a 400
				response = ERROR_UTILS.entityNotFoundResponse(ex);
			} catch (UserNotFoundException ex) {
				response = ERROR_UTILS.internalServerError("There was an error retrieving the user from the database");
			} catch (DataAccessException ex) {
				response = ERROR_UTILS.badRequestResponse(ex);
			} catch (Exception ex) {
				response = ERROR_UTILS.internalServerError(ex.getCause().getMessage());
			}
		} else {
			response = ERROR_UTILS.unauthorizedResponse("create offering");
		}

		return response;
	}


	@PUT
	@Consumes({"application/xml", "application/json"})
	@Path("/{serviceName}")	
	public Response updateService(@PathParam("storeName") String storeName, 
			@PathParam("serviceName") String serviceName, Service serviceInfo) {

		Response response;

		try {
			@SuppressWarnings("unused")
			Store store = storeBo.findByName(storeName);	//Check that the Store exists
			Service service = serviceBo.findByNameAndStore(serviceName, storeName);

			if (offeringRegistrationAuth.canUpdate(service)) {
				service.setName(serviceInfo.getName());
				service.setUrl(serviceInfo.getUrl());
				service.setLasteditor(AUTH_UTILS.getLoggedUser());
				//TODO: merger in model

				serviceBo.update(service);
				response = Response.status(Status.OK).build();
			} else {
				response = ERROR_UTILS.unauthorizedResponse("update offering " + serviceName);
			}
		} catch (ServiceNotFoundException ex) {
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} catch (StoreNotFoundException ex) {
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} catch (UserNotFoundException ex) {
			response = ERROR_UTILS.internalServerError("There was an error retrieving the user from the database");
		} catch (DataAccessException ex) {
			response = ERROR_UTILS.badRequestResponse(ex);
		} catch (Exception ex) {
			response = ERROR_UTILS.internalServerError(ex.getCause().getMessage());
		}

		return response;
	}

	@DELETE
	@Path("/{serviceName}")	
	public Response deleteService(@PathParam("storeName") String storeName, @PathParam("serviceName") String serviceName) {
		Response response;

		try {
			@SuppressWarnings("unused")
			Store store = storeBo.findByName(storeName);	//Check that the Store exists
			Service service = serviceBo.findByNameAndStore(serviceName, storeName);

			if (offeringRegistrationAuth.canUpdate(service)) {
				serviceBo.delete(service);
				response = Response.status(Status.OK).build();
			} else {
				response = ERROR_UTILS.unauthorizedResponse("update offering " + serviceName);
			}
		} catch (ServiceNotFoundException ex) {
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} catch (StoreNotFoundException ex) {
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} catch (Exception ex) {
			response = ERROR_UTILS.internalServerError(ex.getCause().getMessage());
		}

		return response;
	}

	@GET
	@Produces({"application/xml", "application/json"})
	@Path("/{serviceName}")	
	public Response getService(@PathParam("storeName") String storeName, 
			@PathParam("serviceName") String serviceName) {	
		Response response;

		try {
			@SuppressWarnings("unused")
			Store store = storeBo.findByName(storeName);	//Check that the Store exists
			Service service = serviceBo.findByNameAndStore(serviceName, storeName);

			if (offeringRegistrationAuth.canGet(service)) {
				response = Response.status(Status.OK).entity(service).build();
			} else {
				response = ERROR_UTILS.unauthorizedResponse("get offering " + serviceName);
			}
		} catch (ServiceNotFoundException ex) {
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} catch (StoreNotFoundException ex) {
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} catch (Exception ex) {
			response = ERROR_UTILS.internalServerError(ex.getCause().getMessage());
		}

		return response;
	}

	@GET
	@Produces({"application/xml", "application/json"})
	@Path("/")	
	public Response listServices(@PathParam("storeName") String storeName) {
		Response response;
		
		if (offeringRegistrationAuth.canList()) {
			try {
				Store store = storeBo.findByName(storeName);
				response = Response.status(Status.OK).entity(new Services(store.getServices())).build();
			} catch (StoreNotFoundException ex) {
				response = ERROR_UTILS.entityNotFoundResponse(ex);
			} catch (Exception ex) {
				response = ERROR_UTILS.internalServerError(ex.getCause().getMessage());
			}
		} else {
			response = ERROR_UTILS.unauthorizedResponse("list offerings");
		}
		
		return response;

	}


}
