package org.fiware.apps.marketplace.rest;

/*
 * #%L
 * FiwareMarketplace
 * %%
 * Copyright (C) 2015 CoNWeT Lab, Universidad Politécnica de Madrid
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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.fiware.apps.marketplace.bo.DescriptionBo;
import org.fiware.apps.marketplace.bo.OfferingBo;
import org.fiware.apps.marketplace.bo.StoreBo;
import org.fiware.apps.marketplace.exceptions.DescriptionNotFoundException;
import org.fiware.apps.marketplace.exceptions.OfferingNotFoundException;
import org.fiware.apps.marketplace.exceptions.StoreNotFoundException;
import org.fiware.apps.marketplace.model.Description;
import org.fiware.apps.marketplace.model.Offering;
import org.fiware.apps.marketplace.model.Offerings;
import org.fiware.apps.marketplace.security.auth.OfferingAuth;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Path("/store/{storeName}/description/{descriptionName}/offering/")
public class OfferingService {
	
	// OBJECT ATTRIBUTES //
	@Autowired private OfferingBo offeringBo;
	@Autowired private OfferingAuth offeringAuth;
	@Autowired private DescriptionBo descriptionBo;
	@Autowired private StoreBo storeBo;
	
	// CLASS ATTRIBUTES //
	private static final ErrorUtils ERROR_UTILS = new ErrorUtils(
			LoggerFactory.getLogger(DescriptionService.class));
	
	@GET
	@Produces({"application/xml", "application/json"})
	@Path("/{offeringName}")
	public Response getOffering(
			@PathParam("storeName") String storeName, 
			@PathParam("descriptionName") String descriptionName,
			@PathParam("offeringName") String offeringName) {
		
		Response response;
		
		try {
			Offering offering = offeringBo.findDescriptionByNameStoreAndDescription(
					storeName, descriptionName, offeringName);
			
			if (offeringAuth.canGet(offering)) {
				response = Response.status(Status.OK).entity(offering).build();
			} else {
				response = ERROR_UTILS.unauthorizedResponse("get offering"); 
			}
		} catch (OfferingNotFoundException | StoreNotFoundException | 
				DescriptionNotFoundException e) {
			response = ERROR_UTILS.entityNotFoundResponse(e);
		} catch (Exception e) {
			response = ERROR_UTILS.internalServerError(e);
		}
		
		return response;
	}
	
	@GET
	@Produces({"application/xml", "application/json"})
	@Path("/")
	public Response listOfferingsInDescription(
			@PathParam("storeName") String storeName, 
			@PathParam("descriptionName") String descriptionName,
			@DefaultValue("0") @QueryParam("offset") int offset,
			@DefaultValue("100") @QueryParam("max") int max) {
		
		Response response;
		
		try {
			Description description = descriptionBo.findByNameAndStore(storeName, descriptionName);
			
			if (offeringAuth.canList(description)) {
				Offerings offerings = new Offerings(offeringBo.getDescriptionOfferingsPage(
						storeName, descriptionName, offset, max));
				response = Response.status(Status.OK).entity(offerings).build();
			} else {
				response = ERROR_UTILS.unauthorizedResponse(String.format(
						"list offerings in description %s (Store: %s)", descriptionName, storeName));
			}
		} catch (DescriptionNotFoundException | StoreNotFoundException e) {
			response = ERROR_UTILS.entityNotFoundResponse(e);
		} catch (Exception e) {
			response = ERROR_UTILS.internalServerError(e);
		}
		
		return response;
	}
	
}
