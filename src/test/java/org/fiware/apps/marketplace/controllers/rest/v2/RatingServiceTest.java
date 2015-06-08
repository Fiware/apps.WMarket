package org.fiware.apps.marketplace.controllers.rest.v2;

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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.fiware.apps.marketplace.bo.OfferingBo;
import org.fiware.apps.marketplace.exceptions.DescriptionNotFoundException;
import org.fiware.apps.marketplace.exceptions.NotAuthorizedException;
import org.fiware.apps.marketplace.exceptions.OfferingNotFoundException;
import org.fiware.apps.marketplace.exceptions.RatingNotFoundException;
import org.fiware.apps.marketplace.exceptions.StoreNotFoundException;
import org.fiware.apps.marketplace.exceptions.UserNotFoundException;
import org.fiware.apps.marketplace.exceptions.ValidationException;
import org.fiware.apps.marketplace.model.ErrorType;
import org.fiware.apps.marketplace.model.Rating;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class RatingServiceTest {

	private UriInfo uri;

	@Mock private OfferingBo offeringBoMock;
	@InjectMocks private RatingService ratingService;

	private static final String PATH = "/api/v2/store/storeName/description/descName/offering/offeringName/rating";

	@Before 
	public void setUp() throws UserNotFoundException {
		MockitoAnnotations.initMocks(this);

		uri = mock(UriInfo.class);
		when(uri.getPath()).thenReturn(PATH);

	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////// CREATE ///////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////

	private void testCreateRatingException(Exception ex, int statusCode, ErrorType errorType, 
			String message, String field) {

		try {

			String storeName = "store";
			String descriptionName = "description";
			String offeringName = "offering";
			Rating rating = new Rating();

			// Mocks
			doThrow(ex).when(offeringBoMock).createRating(storeName, descriptionName, offeringName, rating);

			// Actual call
			Response res = ratingService.createRating(uri, storeName, descriptionName, offeringName, rating);
			GenericRestTestUtils.checkAPIError(res, statusCode, errorType, message, field);
			
		} catch (Exception e1) {
			fail("Exception not expected", e1);
		}

	}

	@Test
	public void testCreateRatingNotAuthorized() {
		NotAuthorizedException ex = new NotAuthorizedException("rate offering");
		testCreateRatingException(ex, 403, ErrorType.FORBIDDEN, ex.getMessage(), null);
	}
	
	@Test
	public void testCreateRatingStoreNotFound() {
		StoreNotFoundException ex = new StoreNotFoundException("store not found");
		testCreateRatingException(ex, 404, ErrorType.NOT_FOUND, ex.getMessage(), null);
	}
	
	@Test
	public void testCreateRatingDescriptionNotFound() {
		DescriptionNotFoundException ex = new DescriptionNotFoundException("description not found");
		testCreateRatingException(ex, 404, ErrorType.NOT_FOUND, ex.getMessage(), null);
	}
	
	@Test
	public void testCreateRatingOfferingNotFound() {
		OfferingNotFoundException ex = new OfferingNotFoundException("offering not found");
		testCreateRatingException(ex, 404, ErrorType.NOT_FOUND, ex.getMessage(), null);
	}
	
	@Test
	public void testCreateRatingValidationException() {
		String field = "score";
		ValidationException ex = new ValidationException(field, "invalid");
		testCreateRatingException(ex, 400, ErrorType.VALIDATION_ERROR, ex.getMessage(), field);
	}
	
	@Test
	public void testCreateRating() throws Exception {
		
		String storeName = "store";
		String descriptionName = "description";
		String offeringName = "offering";
		Rating rating = new Rating();
		final int id = 9;
		
		// Mocks
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				invocation.getArgumentAt(3, Rating.class).setId(id);
				return null;
			}
		}).when(offeringBoMock).createRating(storeName, descriptionName, offeringName, rating);
		
		// Actual call
		Response res = ratingService.createRating(uri, storeName, descriptionName, offeringName, rating);
		
		// Check response and headers
		assertThat(res.getStatus()).isEqualTo(201);
		assertThat(res.getHeaders().get("Location").get(0).toString()).isEqualTo(PATH + "/" + id);
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////// UPDATE ///////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	private void testUpdateRatingException(Exception ex, int statusCode, ErrorType errorType, 
			String message, String field) {

		try {

			String storeName = "store";
			String descriptionName = "description";
			String offeringName = "offering";
			int ratingId = 7;
			Rating rating = new Rating();

			// Mocks
			doThrow(ex).when(offeringBoMock).updateRating(storeName, descriptionName, offeringName, ratingId, rating);

			// Actual call
			Response res = ratingService.updateRating(storeName, descriptionName, offeringName, ratingId, rating);
			GenericRestTestUtils.checkAPIError(res, statusCode, errorType, message, field);
			
		} catch (Exception e1) {
			fail("Exception not expected", e1);
		}

	}

	@Test
	public void testUpdateRatingNotAuthorized() {
		NotAuthorizedException ex = new NotAuthorizedException("rate offering");
		testUpdateRatingException(ex, 403, ErrorType.FORBIDDEN, ex.getMessage(), null);
	}
	
	@Test
	public void testUpdateRatingStoreNotFound() {
		StoreNotFoundException ex = new StoreNotFoundException("store not found");
		testUpdateRatingException(ex, 404, ErrorType.NOT_FOUND, ex.getMessage(), null);
	}
	
	@Test
	public void testUpdateRatingDescriptionNotFound() {
		DescriptionNotFoundException ex = new DescriptionNotFoundException("description not found");
		testUpdateRatingException(ex, 404, ErrorType.NOT_FOUND, ex.getMessage(), null);
	}
	
	@Test
	public void testUpdateRatingOfferingNotFound() {
		OfferingNotFoundException ex = new OfferingNotFoundException("offering not found");
		testUpdateRatingException(ex, 404, ErrorType.NOT_FOUND, ex.getMessage(), null);
	}
	
	@Test
	public void testUpdateRatingRatingNotFound() {
		RatingNotFoundException ex = new RatingNotFoundException("rating not found");
		testUpdateRatingException(ex, 404, ErrorType.NOT_FOUND, ex.getMessage(), null);
	}
	
	@Test
	public void testUpdateRatingValidationException() {
		String field = "score";
		ValidationException ex = new ValidationException(field, "invalid");
		testUpdateRatingException(ex, 400, ErrorType.VALIDATION_ERROR, ex.getMessage(), field);
	}
	
	@Test
	public void testUpdateRating() throws Exception {
		
		String storeName = "store";
		String descriptionName = "description";
		String offeringName = "offering";
		Rating rating = new Rating();
		int ratingId = 9;
		
		// Actual call
		Response res = ratingService.updateRating(storeName, descriptionName, offeringName, ratingId, rating);
		
		// Check response
		assertThat(res.getStatus()).isEqualTo(200);
	}	
}