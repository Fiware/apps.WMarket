package org.fiware.apps.marketplace.rest;

/*
 * #%L
 * FiwareMarketplace
 * %%
 * Copyright (C) 2012 SAP
 * Copyright (C) 2014-2015 CoNWeT Lab, Universidad Politécnica de Madrid
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of copyright holders nor the names of its contributors
 *    may be used to endorse or promote products derived from this software 
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.fiware.apps.marketplace.bo.OfferingsDescriptionBo;
import org.fiware.apps.marketplace.bo.StoreBo;
import org.fiware.apps.marketplace.exceptions.OfferingDescriptionNotFoundException;
import org.fiware.apps.marketplace.exceptions.StoreNotFoundException;
import org.fiware.apps.marketplace.exceptions.UserNotFoundException;
import org.fiware.apps.marketplace.exceptions.ValidationException;
import org.fiware.apps.marketplace.model.OfferingsDescriptions;
import org.fiware.apps.marketplace.model.User;
import org.fiware.apps.marketplace.model.OfferingsDescription;
import org.fiware.apps.marketplace.model.Store;
import org.fiware.apps.marketplace.model.validators.OfferingsDescriptionValidator;
import org.fiware.apps.marketplace.security.auth.AuthUtils;
import org.fiware.apps.marketplace.security.auth.OfferingsDescriptionRegistrationAuth;
import org.hibernate.HibernateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.shared.JenaException;

@Component
@Path("/store/{storeName}/offerings_description/")	
public class OfferingsDescriptionRegistrationService {

	// OBJECT ATTRIBUTES //
	@Autowired private StoreBo storeBo;
	@Autowired private OfferingsDescriptionBo offeringsDescriptionBo;
	@Autowired private OfferingsDescriptionRegistrationAuth offeringRegistrationAuth;
	@Autowired private OfferingsDescriptionValidator offeringsDescriptionValidator;
	@Autowired private AuthUtils authUtils;

	// CLASS ATTRIBUTES //
	private static final String INVALID_RDF = "Your RDF could not be parsed";
	private static final ErrorUtils ERROR_UTILS = new ErrorUtils(
			LoggerFactory.getLogger(OfferingsDescriptionRegistrationService.class), 
			"There is already an Offering in this Store with that name/URL");

	@POST
	@Consumes({"application/xml", "application/json"})
	@Path("/")	
	public Response createOfferingsDescription(@PathParam("storeName") String storeName, 
			OfferingsDescription offeringsDescription) {	
		Response response;

		try {			
			if (offeringRegistrationAuth.canCreate()) {

				// Validate offerings description (exception is thrown if the description is not valid) 
				offeringsDescriptionValidator.validateOfferingsDescription(offeringsDescription, true);

				User user = authUtils.getLoggedUser();
				Store store = storeBo.findByName(storeName);
				offeringsDescription.setRegistrationDate(new Date());
				offeringsDescription.setStore(store);
				offeringsDescription.setCreator(user);
				offeringsDescription.setLasteditor(user);
				offeringsDescription.setName(Utils.getURLName(offeringsDescription.getDisplayName()));

				offeringsDescriptionBo.save(offeringsDescription);
				response = Response.status(Status.CREATED)
						.contentLocation(new URI(offeringsDescription.getName()))
						.build();
			} else {
				response = ERROR_UTILS.unauthorizedResponse("create offering");
			}
		} catch (ValidationException ex) {
			response = ERROR_UTILS.badRequestResponse(ex.getMessage());
		} catch (JenaException ex) {
			// When a offering description is created, the RDF is parsed to update the index
			// If the RDF is not correct, this exception will be risen
			response = ERROR_UTILS.badRequestResponse(INVALID_RDF);
		} catch (StoreNotFoundException ex) {
			//The Store is an URL... If the Store does not exist a 404
			//should be returned instead of a 400
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} catch (UserNotFoundException ex) {
			response = ERROR_UTILS.internalServerError(
					"There was an error retrieving the user from the database");
		} catch (HibernateException ex) {
			response = ERROR_UTILS.badRequestResponse(ex);
		} catch (Exception ex) {
			response = ERROR_UTILS.internalServerError(ex);
		}

		return response;
	}


	@PUT
	@Consumes({"application/xml", "application/json"})
	@Path("/{offeringsDescriptionName}")
	public Response updateOfferingsDescription(@PathParam("storeName") String storeName, 
			@PathParam("offeringsDescriptionName") String offeringsDescriptionName, 
			OfferingsDescription offeringDescriptionInfo) {

		Response response;

		try {
			@SuppressWarnings("unused")
			Store store = storeBo.findByName(storeName);	//Check that the Store exists
			OfferingsDescription offeringDescription = offeringsDescriptionBo.
					findByNameAndStore(offeringsDescriptionName, storeName);

			if (offeringRegistrationAuth.canUpdate(offeringDescription)) {

				// Validate offerings description (exception is thrown if the description is not valid) 
				offeringsDescriptionValidator.validateOfferingsDescription(offeringDescriptionInfo, false);

				// Name cannot be changed...
				// if (offeringDescriptionInfo.getName() != null) {
				// 	 offeringDescription.setName(offeringDescriptionInfo.getName());
				// }

				if (offeringDescriptionInfo.getDisplayName() != null) {
					offeringDescription.setDisplayName(offeringDescriptionInfo.getDisplayName());
				}

				if (offeringDescriptionInfo.getUrl() != null) {
					offeringDescription.setUrl(offeringDescriptionInfo.getUrl());
				}

				if (offeringDescriptionInfo.getDescription() != null) {
					offeringDescription.setDescription(offeringDescriptionInfo.getDescription());
				}

				offeringDescription.setLasteditor(authUtils.getLoggedUser());

				offeringsDescriptionBo.update(offeringDescription);
				response = Response.status(Status.OK).build();
			} else {
				response = ERROR_UTILS.unauthorizedResponse(
						"update offering " + offeringsDescriptionName);
			}
		} catch (ValidationException ex) {
			response = ERROR_UTILS.badRequestResponse(ex.getMessage());
		} catch (JenaException ex) {
			// When a offering description is created, the RDF is parsed to update the index
			// If the RDF is not correct, this exception will be risen
			response = ERROR_UTILS.badRequestResponse(INVALID_RDF);
		} catch (OfferingDescriptionNotFoundException ex) {
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} catch (StoreNotFoundException ex) {
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} catch (UserNotFoundException ex) {
			response = ERROR_UTILS.internalServerError(
					"There was an error retrieving the user from the database");
		} catch (HibernateException ex) {
			response = ERROR_UTILS.badRequestResponse(ex);
		} catch (Exception ex) {
			response = ERROR_UTILS.internalServerError(ex);
		}

		return response;
	}

	@DELETE
	@Path("/{offeringsDescriptionName}")	
	public Response deleteOfferingsDescription(@PathParam("storeName") String storeName, 
			@PathParam("offeringsDescriptionName") String offeringsDescriptionName) {
		Response response;

		try {
			@SuppressWarnings("unused")
			Store store = storeBo.findByName(storeName);	//Check that the Store exists
			OfferingsDescription offeringsDescription = offeringsDescriptionBo.
					findByNameAndStore(offeringsDescriptionName, storeName);

			if (offeringRegistrationAuth.canDelete(offeringsDescription)) {
				offeringsDescriptionBo.delete(offeringsDescription);
				response = Response.status(Status.NO_CONTENT).build();
			} else {
				response = ERROR_UTILS.unauthorizedResponse("delete offering " + offeringsDescriptionName);
			}
		} catch (OfferingDescriptionNotFoundException ex) {
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} catch (StoreNotFoundException ex) {
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} catch (Exception ex) {
			response = ERROR_UTILS.internalServerError(ex);
		}

		return response;
	}

	@GET
	@Produces({"application/xml", "application/json"})
	@Path("/{offeringsDescriptionName}")	
	public Response getOfferingsDescription(@PathParam("storeName") String storeName, 
			@PathParam("offeringsDescriptionName") String offeringsDescriptionName) {	
		Response response;

		try {
			@SuppressWarnings("unused")
			Store store = storeBo.findByName(storeName);	//Check that the Store exists
			OfferingsDescription offeringDescription = offeringsDescriptionBo.
					findByNameAndStore(offeringsDescriptionName, storeName);

			if (offeringRegistrationAuth.canGet(offeringDescription)) {
				response = Response.status(Status.OK).entity(offeringDescription).build();
			} else {
				response = ERROR_UTILS.unauthorizedResponse("get offering " + offeringsDescriptionName);
			}
		} catch (OfferingDescriptionNotFoundException ex) {
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} catch (StoreNotFoundException ex) {
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} catch (Exception ex) {
			response = ERROR_UTILS.internalServerError(ex);
		}

		return response;
	}

	@GET
	@Produces({"application/xml", "application/json"})
	@Path("/")	
	public Response listOfferingsDescriptionsInStore(@PathParam("storeName") String storeName, 
			@DefaultValue("0") @QueryParam("offset") int offset,
			@DefaultValue("100") @QueryParam("max") int max) {
		Response response;

		if (offset < 0 || max <= 0) {
			// Offset and Max should be checked
			response = ERROR_UTILS.badRequestResponse("offset and/or max are not valid");
		} else {
			try {
				int toIndex;
				Store store = storeBo.findByName(storeName);

				if (offeringRegistrationAuth.canList(store)) {
					OfferingsDescriptions returnedOfferingDescriptions = new OfferingsDescriptions();
					List<OfferingsDescription> allOfferingDescriptions = store.getOfferingsDescriptions();

					// Otherwise (if offset > allServices.size() - 1) an empty list will be returned
					if (offset <= allOfferingDescriptions.size() - 1) {
						if (offset + max > allOfferingDescriptions.size()) {
							toIndex = allOfferingDescriptions.size();
						} else {
							toIndex = offset + max;
						}	
						returnedOfferingDescriptions.setOfferingsDescriptions(
								store.getOfferingsDescriptions().subList(offset, toIndex));
					}

					response = Response.status(Status.OK).entity(returnedOfferingDescriptions).build();
				} else {
					response = ERROR_UTILS.unauthorizedResponse("list offerings");
				}


			} catch (StoreNotFoundException ex) {
				response = ERROR_UTILS.entityNotFoundResponse(ex);
			} catch (Exception ex) {
				response = ERROR_UTILS.internalServerError(ex);
			}
		}

		return response;
	}
}
