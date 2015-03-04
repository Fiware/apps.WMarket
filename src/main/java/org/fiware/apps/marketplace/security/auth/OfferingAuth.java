package org.fiware.apps.marketplace.security.auth;

/*
 * #%L
 * FiwareMarketplace
 * %%
 * Copyright (C) 2014 CoNWeT Lab, Universidad Politécnica de Madrid
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

import org.fiware.apps.marketplace.exceptions.NotAuthorizedException;
import org.fiware.apps.marketplace.model.Description;
import org.fiware.apps.marketplace.model.Offering;
import org.fiware.apps.marketplace.model.Store;
import org.fiware.apps.marketplace.model.User;
import org.springframework.stereotype.Service;

@Service("offeringAuth")
public class OfferingAuth extends AbstractAuth<Offering> {

	@Override
	protected User getEntityOwner(Offering offering) {
		return offering.getDescribedIn().getCreator();
	}
	
	@Override
	protected String genEntityName(Offering entity) {
		return String.format("%s (Store: %s, Description: %s)", entity.getName(), 
				entity.getStore().getName(), entity.getDescribedIn().getName());
	}
	
	/**
	 * Determines if a user can list all the offerings described in an description.
	 * @param description The description where the offerings are described
	 * @throws NotAuthorizedException If the user is not allowed to list all
	 * the descriptions contained in the description
	 */
	public void canList(Description description) throws NotAuthorizedException {

	}
	
	/**
	 * Determines if a user can list all the offerings that belongs to a Store
	 * @param store The store where the offerings are contained
	 * @throws NotAuthorizedException If the user is not allowed to list all
	 * the descriptions contained in the Store
	 */
	public void canList(Store store) throws NotAuthorizedException {

	}

}