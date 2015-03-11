package org.fiware.apps.marketplace.rest.v2;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.fiware.apps.marketplace.bo.OfferingBo;
import org.fiware.apps.marketplace.exceptions.NotAuthorizedException;
import org.fiware.apps.marketplace.model.ErrorType;
import org.fiware.apps.marketplace.model.Offering;
import org.fiware.apps.marketplace.model.Offerings;
import org.fiware.apps.marketplace.model.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AllOfferingsServiceTest {
	
	@Mock private OfferingBo offeringBoMock;
	@InjectMocks private AllOfferingsService allOfferingsService;
	
	private static final String OFFSET_MAX_INVALID = "offset (%d) and/or max (%d) are not valid";
	
	@Before 
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void testListAllOfferingNotAllowed() throws NotAuthorizedException {
		String userName = "example-user";
		
		// Mocks
		User user = mock(User.class);
		when(user.getUserName()).thenReturn(userName);
		Exception e = new NotAuthorizedException(user, "list offerings");
		doThrow(e).when(offeringBoMock).getOfferingsPage(anyInt(), anyInt());

		// Call the method
		Response res = allOfferingsService.listOfferings(0, 100);

		// Assertions
		GenericRestTestUtils.checkAPIError(res, 401, ErrorType.UNAUTHORIZED, 
				e.toString());
	}
	
	private void testListAllOfferingsInvalidParams(int offset, int max) {
		// Call the method
		Response res = allOfferingsService.listOfferings(offset, max);

		// Assertions
		GenericRestTestUtils.checkAPIError(res, 400, ErrorType.BAD_REQUEST, 
				String.format(OFFSET_MAX_INVALID, offset, max));
	}
	
	@Test
	public void testListAllDescriptionsInvalidOffset() {
		testListAllOfferingsInvalidParams(-1, 100);
	}
	
	@Test
	public void testListAllDescriptionsInvalidMax() {
		testListAllOfferingsInvalidParams(0, -1);
	}
	
	@Test
	public void testListAllDescriptionsInvalidOffsetMax() {
		testListAllOfferingsInvalidParams(-1, -1);
	}
	
	@Test
	public void testListAllOfferingsGetNoErrors() throws NotAuthorizedException {
		List<Offering> oferrings = new ArrayList<Offering>();
		for (int i = 0; i < 3; i++) {
			Offering offering = new Offering();
			offering.setId(i);
			oferrings.add(offering);
		}
		
		// Mocks
		when(offeringBoMock.getOfferingsPage(anyInt(), anyInt())).
				thenReturn(oferrings);
		
		// Call the method
		int offset = 0;
		int max = 100;
		Response res = allOfferingsService.listOfferings(offset, max);
		
		// Verify
		verify(offeringBoMock).getOfferingsPage(offset, max);
		
		// Assertions
		assertThat(res.getStatus()).isEqualTo(200);
		assertThat(((Offerings) res.getEntity()).
				getOfferings()).isEqualTo(oferrings);
	}
	
	@Test
	public void testListAllDescriptionsException() throws NotAuthorizedException {
		// Mocks
		String exceptionMsg = "exception";
		doThrow(new RuntimeException("", new Exception(exceptionMsg)))
				.when(offeringBoMock).getOfferingsPage(anyInt(), anyInt());

		// Call the method
		int offset = 0;
		int max = 100;
		Response res = allOfferingsService.listOfferings(offset, max);
		
		// Verify
		verify(offeringBoMock).getOfferingsPage(offset, max);
		
		// Check exception
		GenericRestTestUtils.checkAPIError(res, 500, ErrorType.INTERNAL_SERVER_ERROR, exceptionMsg);
	}

}
