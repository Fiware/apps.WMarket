package org.fiware.apps.marketplace.bo.impl;

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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fiware.apps.marketplace.bo.UserBo;
import org.fiware.apps.marketplace.bo.impl.DescriptionBoImpl;
import org.fiware.apps.marketplace.dao.CategoryDao;
import org.fiware.apps.marketplace.dao.DescriptionDao;
import org.fiware.apps.marketplace.dao.ServiceDao;
import org.fiware.apps.marketplace.dao.StoreDao;
import org.fiware.apps.marketplace.exceptions.DescriptionNotFoundException;
import org.fiware.apps.marketplace.exceptions.NotAuthorizedException;
import org.fiware.apps.marketplace.exceptions.StoreNotFoundException;
import org.fiware.apps.marketplace.exceptions.ValidationException;
import org.fiware.apps.marketplace.helpers.OfferingResolver;
import org.fiware.apps.marketplace.model.Category;
import org.fiware.apps.marketplace.model.Description;
import org.fiware.apps.marketplace.model.Offering;
import org.fiware.apps.marketplace.model.PricePlan;
import org.fiware.apps.marketplace.model.Service;
import org.fiware.apps.marketplace.model.Store;
import org.fiware.apps.marketplace.model.User;
import org.fiware.apps.marketplace.model.validators.DescriptionValidator;
import org.fiware.apps.marketplace.rdf.RdfIndexer;
import org.fiware.apps.marketplace.security.auth.DescriptionAuth;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.web.util.HtmlUtils;

import com.hp.hpl.jena.shared.JenaException;

public class DescriptionBoImplTest {

	@Mock private DescriptionAuth descriptionAuthMock;
	@Mock private DescriptionValidator descriptionValidatorMock;
	@Mock private DescriptionDao descriptionDaoMock;
	@Mock private StoreDao storeDaoMock;
	@Mock private UserBo userBoMock;
	@Mock private OfferingResolver offeringResolverMock;
	@Mock private RdfIndexer rdfIndexerMock;
	@Mock private SessionFactory sessionFactory;
	@Mock private CategoryDao categoryDao;
	@Mock private ServiceDao serviceDao;
	@InjectMocks private DescriptionBoImpl descriptionBo;

	private static final String STORE_NAME = "store";
	private static final String NAME = "description-display-name";
	private static final String DESCRIPTION_DISPLAY_NAME = "Description Display Name";
	private static final String DESCRIPTION_URL = "http://repo.lab.fiware.org/sampleUsdl.rdf";
	private static final String NOT_AUTHORIZED_BASE = "You are not authorized to %s";
	private static final String JENA_EXCEPTION = "Your RDF could not be parsed.";

	@Before 
	public void setUp() {
		
		MockitoAnnotations.initMocks(this);
		
		Session session = mock(Session.class);
		when(sessionFactory.getCurrentSession()).thenReturn(session);
		
		this.descriptionBo = spy(this.descriptionBo);
	}

	private Description generateDescription(List<Offering> offerings) {
		
		Description description = new Description();
		// description.setName(DESCRIPTION_NAME);
		description.setDisplayName(DESCRIPTION_DISPLAY_NAME);
		description.setUrl(DESCRIPTION_URL);
		description.setComment("Sample Description");
		description.setOfferings(offerings);

		return description;
	}

	private Offering generateOffering(String url, Description describedIn, 
			String name, String displayName, String description, String imageUrl, 
			String version) {
		
		Offering offering = new Offering();
		offering.setUri(url);
		offering.setDescribedIn(describedIn);
		offering.setName(name);
		offering.setDisplayName(displayName);
		offering.setDescription(description);
		offering.setImageUrl(imageUrl);
		offering.setVersion(version);
		offering.setCategories(new HashSet<Category>());
		offering.setServices(new HashSet<Service>());
		offering.setPricePlans(new HashSet<PricePlan>());

		return offering;
	}
	
	private Category generateCategory(String categoryName) {
		
		Category category = new Category();
		category.setName(categoryName);
		category.setServices(new HashSet<Service>());
		
		return category;
	}
	
	private Service generateService(String uri) {
		
		Service service = new Service();
		service.setUri(uri);
		service.setCategories(new HashSet<Category>());
		
		return service;
	}

	
	///////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////// SAVE ////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////

	private void testSaveException(Description description) throws Exception {

		// Configure methods
		Store store = mock(Store.class);
		when(storeDaoMock.findByName(STORE_NAME)).thenReturn(store);

		try {
			// Call the method
			descriptionBo.save(STORE_NAME, description);
			fail("Exception expected");
		} catch (Exception e) {
			// Verify that the DAO has not been called
			verify(storeDaoMock, never()).save(store);
			// Throw the exception
			throw e;
		}

	}

	@Test
	public void testSaveNotAuthorized() throws Exception {
		
		try {
			
			Description description = generateDescription(new ArrayList<Offering>());
			when(descriptionAuthMock.canCreate(description)).thenReturn(false);

			// Call the method and check that DAO is not called
			testSaveException(description);

			// If the exception is not risen, the method is not properly working
			failBecauseExceptionWasNotThrown(NotAuthorizedException.class);
			
		} catch (NotAuthorizedException ex) {
			
			assertThat(ex.getMessage()).isEqualTo(
					String.format(NOT_AUTHORIZED_BASE, "create description"));
		}
	}

	@Test(expected=ValidationException.class)
	public void testSaveInvalidDescription() throws Exception {

		Description description = generateDescription(new ArrayList<Offering>());
		doThrow(new ValidationException("a field", "invalid"))
				.when(descriptionValidatorMock)
				.validateNewDescription(description);
		when(descriptionAuthMock.canCreate(description)).thenReturn(true);

		// Call the method and check that DAO is not called
		testSaveException(description);
	}

	private void testSave(Description description, Exception indexerException) {

		Store store = mock(Store.class);

		try {

			// Description returned by find
			Description descriptionWithId = mock(Description.class);
			when(descriptionWithId.getId()).thenReturn(1);

			// Mocks configuring
			when(storeDaoMock.findByName(STORE_NAME)).thenReturn(store);
			when(descriptionAuthMock.canCreate(description)).thenReturn(true);
			when(descriptionDaoMock.findByNameAndStore(STORE_NAME, NAME))
					.thenReturn(descriptionWithId);
			
			if (indexerException != null) {
				
				doThrow(indexerException).when(rdfIndexerMock)
						.indexOrUpdateService(description);
			}

			descriptionBo.save(STORE_NAME, description);

			// Verify that the description has been saved
			// Descriptions are saved by adding them to one Store.
			verify(storeDaoMock).update(store);

			// Verify that the description has been indexed
			verify(rdfIndexerMock).indexOrUpdateService(description);

			// Verify that the description is now included in the store
			verify(store).addDescription(description);

			// Verify that the offering has been parsed
			verify(offeringResolverMock).resolveOfferingsFromServiceDescription(description);

		} catch (ValidationException e) {

			if (indexerException != null) {
				
				// Check exception message
				String expectedMessage;

				if (indexerException instanceof JenaException) {
					expectedMessage = JENA_EXCEPTION;
				} else {
					expectedMessage = indexerException.getMessage();
				}
				
				assertThat(e.getMessage()).isEqualTo(expectedMessage);

				// The description is added to the store to be saved in the database
				verify(store).addDescription(description);

				// RdfIndexer has been called
				try {
					verify(rdfIndexerMock).indexOrUpdateService(description);
				} catch (IOException ex) {
					// It should not happen
				}
				
				// When an exception is thrown rollback is done so the Store
				// is not updated with the new description
				
			} else {
				fail("Exception not expected", e);
			}

		} catch (Exception e) {
			fail("Exception not expected", e);
		}
	}

	@Test
	public void testSaveDescriptionNoParsingErros() {
		
		Description description = generateDescription(new ArrayList<Offering>());

		// Save & check
		testSave(description, null);
	}

	@Test
	public void testSaveDescriptionMalformedUrlException() {
		
		Description description = generateDescription(new ArrayList<Offering>());

		// Save & check
		testSave(description, new MalformedURLException("URL is malformed"));

	}

	@Test
	public void testSaveDescriptionJenaException() {
		
		Description description = generateDescription(new ArrayList<Offering>());

		// Save & check
		testSave(description, new JenaException());

	}
	
	@Test
	public void testHtmlIsEscapedWhenCreating() throws Exception {
		
		Description descriptionWithId = mock(Description.class);
		when(descriptionDaoMock.findByNameAndStore(STORE_NAME, NAME))
				.thenReturn(descriptionWithId);
		
		Store store = mock(Store.class);
		when(storeDaoMock.findByName(STORE_NAME)).thenReturn(store);
		
		String html = "<img src=\"http://fiware.org/logo.png\">";

		Description description = mock(Description.class);
		when(description.getName()).thenReturn(NAME);
		when(description.getDisplayName()).thenReturn(DESCRIPTION_DISPLAY_NAME);
		when(description.getComment()).thenReturn(html);
		when(descriptionAuthMock.canCreate(description)).thenReturn(true);

		
		InOrder order = inOrder(description, storeDaoMock);
		
		descriptionBo.save(STORE_NAME, description);

		order.verify(description).setComment(HtmlUtils.htmlEscape(html));
		order.verify(storeDaoMock).update(store);
		
	}

	
	///////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////// UPDATE ////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	private void testUpdateException(String storeName, String descriptionName, 
			Description updatedDescription) throws Exception {		
		
		// Call the method
		try {
			descriptionBo.update(storeName, descriptionName, updatedDescription);
			fail("Exception expected");
			
		} catch (Exception e) {
			
			verify(descriptionDaoMock, never()).update(any(Description.class));
			throw e;
		}
	}
	
	@Test(expected=DescriptionNotFoundException.class)
	public void testUpdateNotFound() throws Exception {
		
		Description updatedDescription = mock(Description.class);
		
		// Configure mocks
		doThrow(new DescriptionNotFoundException("Description not found"))
				.when(descriptionDaoMock).findByNameAndStore(STORE_NAME, NAME);
		
		// Execute the function an check that DAO has not been called
		testUpdateException(STORE_NAME, NAME, updatedDescription);
	}

	@Test(expected=ValidationException.class)
	public void testUpdateNotValid() throws Exception {
		
		Description descriptionToUpdate = mock(Description.class);
		Description updatedDescription = mock(Description.class);
		
		// Configure mocks
		doReturn(descriptionToUpdate).when(descriptionDaoMock)
				.findByNameAndStore(STORE_NAME, NAME);
		doThrow(new ValidationException("a field", "not valid"))
				.when(descriptionValidatorMock)
				.validateUpdatedDescription(descriptionToUpdate, updatedDescription);
		when(descriptionAuthMock.canUpdate(descriptionToUpdate)).thenReturn(true);
		
		// Execute the function an check that DAO has not been called
		testUpdateException(STORE_NAME, NAME, updatedDescription);
	}
	
	@Test
	public void testUpdateNotAuthorized() throws Exception {
		
		try {
			
			Description descriptionToUpdate = mock(Description.class);
			Description updatedDescription = mock(Description.class);
			
			// Configure mocks
			doReturn(descriptionToUpdate).when(descriptionDaoMock)
					.findByNameAndStore(STORE_NAME, NAME);
			when(descriptionAuthMock.canUpdate(descriptionToUpdate))
					.thenReturn(false);
			
			// Execute the function an check that DAO has not been called
			testUpdateException(STORE_NAME, NAME, updatedDescription);
			
			// If the exception is not risen, the method is not properly working
			failBecauseExceptionWasNotThrown(NotAuthorizedException.class);

		} catch (NotAuthorizedException ex) {
			
			assertThat(ex.getMessage()).isEqualTo(String.format(
					NOT_AUTHORIZED_BASE, "update description"));
		}
	}

	private void testUpdateDescriptionField(Description newDescription) {
		
		try {

			User user = mock(User.class);

			Description description = generateDescription(new ArrayList<Offering>());

			// Mock
			doReturn(user).when(userBoMock).getCurrentUser();
			doReturn(description).when(descriptionDaoMock)
					.findByNameAndStore(STORE_NAME, NAME);
			when(descriptionAuthMock.canUpdate(description))
					.thenReturn(true);

			// Get the 
			String previousName = description.getName();

			// Call the method
			descriptionBo.update(STORE_NAME, NAME, newDescription);

			// Verify mocks
			verify(descriptionDaoMock).update(description);
			
			// Assert that description name has not changed
			assertThat(description.getName()).isEqualTo(previousName);

			// New values
			String newStoreName = newDescription.getName() != null ? 
					newDescription.getName() : description.getName();
			assertThat(description.getName()).isEqualTo(newStoreName);

			String newStoreUrl = newDescription.getUrl() != null ? 
					newDescription.getUrl() : description.getUrl();
			assertThat(description.getUrl()).isEqualTo(newStoreUrl);

			String newStoreDescription = newDescription.getComment() != null ? 
					newDescription.getComment() : description.getComment();
			assertThat(description.getComment()).isEqualTo(newStoreDescription);

		} catch (Exception ex) {
			
			// It's not supposed to happen
			fail("Exception " + ex + " is not supposed to happen");
		}
	}

	@Test
	public void testUpdateDescriptionName() {
		
		Description newDescription = new Description();
		newDescription.setDisplayName("new_name");
		
		testUpdateDescriptionField(newDescription);
	}

	@Test
	public void testUpdateDescriptionUrl() {
		
		Description newDescription = new Description();
		newDescription.setUrl(DESCRIPTION_URL + "a");
		
		testUpdateDescriptionField(newDescription);
	}

	@Test
	public void testUpdateDescriptionDescription() {
		
		Description newDescription = new Description();
		newDescription.setComment("New Description");
		
		testUpdateDescriptionField(newDescription);
	}

	private void compareOfferings(Offering offering1, Offering offering2) {

		assertThat(offering1.getDescription()).isEqualTo(offering2.getDescription());
		assertThat(offering1.getDisplayName()).isEqualTo(offering2.getDisplayName());
		assertThat(offering1.getName()).isEqualTo(offering2.getName());
		assertThat(offering1.getImageUrl()).isEqualTo(offering2.getImageUrl());
		assertThat(offering1.getVersion()).isEqualTo(offering2.getVersion());
		assertThat(offering1.getServices()).isEqualTo(offering2.getServices());
		assertThat(offering1.getPricePlans()).isEqualTo(offering2.getPricePlans());
		assertThat(offering1.getCategories()).isEqualTo(offering2.getCategories());

	}

	private void testUpdateDescriptionURL(String storedOfferingURI, String newOfferingURI) {

		try {

			// Create the description stored in the Database
			Description storedDescription = generateDescription(new ArrayList<Offering>());

			Offering storedOffering = generateOffering(storedOfferingURI, 
					storedDescription, "offering-1", "Offering 1", "Example Description", 
					"http://marketplace.com/link_to_img.jpg", "1.0");
			storedOffering.setId(1); 	// Stored offerings contain an ID

			storedDescription.addOffering(storedOffering);

			// New description
			Description updatedDescription = generateDescription(new ArrayList<Offering>());

			// Descriptions returned by offeringResolver when the updatedDescription is parsed
			Offering newOffering = generateOffering(newOfferingURI, storedDescription, 
					"cool-offering", "Cool Offering", "New Description", 
					"http://marketplace.com/link_to_new_img.jpg", "1.2");

			List<Offering> newOfferings = new ArrayList<Offering>();
			newOfferings.add(newOffering);

			// Configure the mocks
			doReturn(storedDescription).when(descriptionDaoMock)
					.findByNameAndStore(STORE_NAME, NAME);
			when(descriptionAuthMock.canUpdate(storedDescription))
					.thenReturn(true);	// User can update
			when(offeringResolverMock
					.resolveOfferingsFromServiceDescription(storedDescription))
					.thenReturn(newOfferings);

			// Call the method
			descriptionBo.update(STORE_NAME, NAME, updatedDescription);

			// Verify that the database has been updated
			verify(descriptionDaoMock).update(storedDescription);
			
			// Verify that the index has been updated
			verify(rdfIndexerMock).indexOrUpdateService(storedDescription);

			// Check that the URL has been updated properly
			assertThat(storedDescription.getUrl()).isEqualTo(updatedDescription.getUrl());

			// Check the offerings. The description should contain one
			assertThat(storedDescription.getOfferings().size()).isEqualTo(1);
			Offering updatedOffering = storedDescription.getOfferings().get(0);

			// updatedOffering should always contain the information returned by offeringResolver
			compareOfferings(updatedOffering, newOffering);

		} catch (Exception ex) {
			
			fail("Exception not expected", ex);
		}
	}

	@Test
	public void testUpdateDescriptionOfferingExist() throws Exception {
		
		String offeringURI = "http://store.lab.fiware.org/OFFERING_URI";
		
		testUpdateDescriptionURL(offeringURI, offeringURI);
	}

	@Test
	public void testUpdateDescriptionOfferingNotExist() throws Exception {
		
		String storedOfferingURI = "http://store.lab.fiware.org/OFFERING_URI";
		String newOfferingURI = "http://store.lab.fiware.org/NEW_OFFERING_URI";
		
		testUpdateDescriptionURL(storedOfferingURI, newOfferingURI);
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testUpdateComplexOffering() throws Exception {

		String repeatedOfferingURI = "http://store.lab.fiware.org/OFFERING_URI";
		String nonRepeatedOfferingURI = "http://store.lab.fiware.org/NEW_OFFERING_URI";

		// Create the description stored in the Database
		Description storedDescription = generateDescription(new ArrayList<Offering>());

		// Create the offering that will be included into the description
		Offering storedOffering = generateOffering(repeatedOfferingURI, storedDescription, 
				"offering-1", "Offering 1", "Example Description", 
				"http://marketplace.com/link_to_img.jpg", "1.0");
		storedOffering.setId(1); 	// Stored offerings contains an ID
		
		// Add two categories... One should be deleted because it won't 
		// be included in the new offerings
		final Category catA = generateCategory("CATEGORY_A");
		final Category catB = generateCategory("CATEGORY_B");
		Set<Category> categoriesOldOffering = new HashSet<Category>();
		categoriesOldOffering.add(catA);
		categoriesOldOffering.add(catB);
		storedOffering.setCategories(categoriesOldOffering);
		
		// Add two services... One should be deleted because it won't 
		// be included in the new offerings
		final Service serA = generateService("uri1");
		final Service serB = generateService("uri2");
		Set<Service> servicesOldOffering = new HashSet<Service>();
		servicesOldOffering.add(serA);
		servicesOldOffering.add(serB);
		storedOffering.setServices(servicesOldOffering);

		// Add the offering to the description
		storedDescription.addOffering(storedOffering);

		// New description
		Description updatedDescription = generateDescription(new ArrayList<Offering>());

		// Offerings contained in the new URL
		Offering repeatedOffering = generateOffering(repeatedOfferingURI, storedDescription, 
				"cool-offering", "Cool Offering", "New Description",
				"http://marketplace.com/link_to_new_img.jpg", "1.2");
		Offering newOffering = generateOffering(nonRepeatedOfferingURI, storedDescription, 
				"my-new-offering", "My New Offering", "New Description 2", 
				"http://marketplace.com/link_to_new_img2.jpg", "1.1");	
		
		// Set one price plan for the repeated offering
		PricePlan pricePlan = new PricePlan();
		pricePlan.setOffering(repeatedOffering);
		Set<PricePlan> pricePlans = new HashSet<>();
		pricePlans.add(pricePlan);
		repeatedOffering.setPricePlans(pricePlans);
		
		// Include serA in one offering, so setA should not be deleted and serA should be deleted.
		Set<Service> servicesRepeatedOffering = new HashSet<>();
		servicesRepeatedOffering.add(serA);
		repeatedOffering.setServices(servicesRepeatedOffering);
		
		// Include catB in one offering, so catA should be deleted and catB should not be deleted.
		Set<Category> categoriesNewOffering = new HashSet<>();
		categoriesNewOffering.add(catB);
		newOffering.setCategories(categoriesNewOffering);

		List<Offering> newOfferings = new ArrayList<Offering>();
		newOfferings.add(repeatedOffering);
		newOfferings.add(newOffering);

		// Configure the mocks
		doReturn(storedDescription).when(descriptionDaoMock).findByNameAndStore(STORE_NAME, NAME);
		when(descriptionAuthMock.canUpdate(storedDescription)).thenReturn(true);	// User can update
		when(offeringResolverMock.resolveOfferingsFromServiceDescription(storedDescription))
				.thenReturn(newOfferings);
		
		Session session = mock(Session.class);
		doAnswer(new Answer() {

			@SuppressWarnings("unchecked")
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				
				Set<Offering> noOfferings = mock(Set.class);
				when(noOfferings.size()).thenReturn(0);
				Set<Offering> oneOffering = mock(Set.class);
				when(oneOffering.size()).thenReturn(1);
				
				catA.setOfferings(noOfferings);
				catB.setOfferings(oneOffering);
				serA.setOfferings(oneOffering);
				serB.setOfferings(noOfferings);
				
				return null;
			}
			
		}).when(session).flush();
		when(sessionFactory.getCurrentSession()).thenReturn(session);

		// Call the method
		descriptionBo.update(STORE_NAME, NAME, updatedDescription);

		// Verify that the database has been updated
		verify(descriptionDaoMock).update(storedDescription);

		// Check that the URL has been updated properly
		assertThat(storedDescription.getUrl()).isEqualTo(updatedDescription.getUrl());

		// Check the offerings. The description should contain two
		assertThat(storedDescription.getOfferings().size()).isEqualTo(2);

		// Check first offering (it previously exist in the system so they're the same instance)
		Offering updatedOffering = storedDescription.getOfferings().get(0);
		compareOfferings(updatedOffering, repeatedOffering);
		assertThat(updatedOffering).isSameAs(storedOffering);
		assertThat(updatedOffering).isNotSameAs(repeatedOffering);
		
		// Check that price plans offering has been updated for this offering
		for (PricePlan storedPricePlan: updatedOffering.getPricePlans()) {
			assertThat(storedPricePlan.getOffering()).isSameAs(storedOffering);
			assertThat(storedPricePlan.getOffering()).isNotSameAs(repeatedOffering);
		}

		// Check second offering (it does not exist in the system so they are not the same instance)
		updatedOffering = storedDescription.getOfferings().get(1);
		compareOfferings(updatedOffering, newOffering);
		
		// Check that catA has been removed while catB has not.
		verify(categoryDao).delete(catA);
		verify(categoryDao, never()).delete(catB);
		verify(serviceDao, never()).delete(serA);
		verify(serviceDao).delete(serB);
	}
	
	private void testUpdateRdfError(Exception indexerException) {
		
		Description storedDescription = generateDescription(new ArrayList<Offering>());
		Description updatedDescription = generateDescription(new ArrayList<Offering>());
		
		try {
					
			// Configure mocks
			doReturn(storedDescription).when(descriptionDaoMock)
					.findByNameAndStore(STORE_NAME, NAME);
			when(descriptionAuthMock.canUpdate(storedDescription))
					.thenReturn(true);	// User can update
			doThrow(indexerException).when(rdfIndexerMock)
					.indexOrUpdateService(storedDescription);
			
			// Call the method
			descriptionBo.update(STORE_NAME, NAME, updatedDescription);
			
			failBecauseExceptionWasNotThrown(ValidationException.class);
			
		} catch (ValidationException ex) {
						
			try {
				verify(rdfIndexerMock).indexOrUpdateService(storedDescription);
			} catch (IOException e) {
				// Not expected
			}
			
			// Description has not been updated
			verify(descriptionDaoMock, never()).update(any(Description.class));
			
			String expectedMessage;
			if (indexerException instanceof JenaException) {
				expectedMessage = JENA_EXCEPTION;
			} else {
				expectedMessage = indexerException.getMessage();
			}
			
			assertThat(ex.getMessage()).isEqualTo(expectedMessage);

		} catch (Exception ex) {
			
			fail("Exception not expected", ex);
		}
	}
	
	@Test
	public void testUpdateJenaException() {
		
		testUpdateRdfError(new JenaException());
	}
	
	@Test
	public void testUpdateMalformedURLException() {
		
		testUpdateRdfError(new MalformedURLException());
	}
	
	@Test
	public void testHtmlIsEscapedWhenUpdating() throws Exception {
		
		String html = "<img src=\"http://fiware.org/logo.png\">";
		
		Description updatedDescription = mock(Description.class);
		when(updatedDescription.getComment()).thenReturn(html);
		
		Description descriptionToBeUpdated = mock(Description.class);
		
		// Mock
		doReturn(descriptionToBeUpdated).when(descriptionDaoMock)
				.findByNameAndStore(STORE_NAME, NAME);
		when(descriptionAuthMock.canUpdate(descriptionToBeUpdated))
				.thenReturn(true);
		
		InOrder order = inOrder(descriptionToBeUpdated, descriptionDaoMock);
		
		// Call the method
		descriptionBo.update(STORE_NAME, NAME, updatedDescription);

		// Verify that the html has been escaped before inserting it 
		// in the database
		order.verify(descriptionToBeUpdated).setComment(HtmlUtils.htmlEscape(html));
		order.verify(descriptionDaoMock).update(descriptionToBeUpdated);
		
	}	
	
	///////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////// DELETE ////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	private void testDeleteException(String storeName, 
			String descriptionName) throws Exception {
		
		Store store = mock(Store.class);
		doReturn(store).when(storeDaoMock).findByName(storeName);
		
		try {			
			// Call the method
			descriptionBo.delete(storeName, descriptionName);
			fail("Exception expected");
		} catch (Exception e) {
			// Verify that the DAO has not been called
			verify(storeDaoMock, never()).update(store);
			
			// Throw the exception
			throw e;
		}

	}
	
	private void testDeleteDescriptionNotFoundException(Exception ex) throws Exception {
		
		String storeName = "store";
		String descriptionName = "description";
		
		doThrow(ex).when(descriptionDaoMock).findByNameAndStore(storeName, descriptionName);
		
		testDeleteException(storeName, descriptionName);
	}
	
	@Test(expected=DescriptionNotFoundException.class)
	public void testDeleteDescriptionDescriptionNotFoundException() throws Exception {
		
		testDeleteDescriptionNotFoundException(new DescriptionNotFoundException("descriptionNotFound"));		
	}
	
	@Test(expected=StoreNotFoundException.class)
	public void testDeleteDescriptionStoreNotFoundException() throws Exception {
		
		testDeleteDescriptionNotFoundException(new StoreNotFoundException("descriptionNotFound"));		
	}
	
	
	@Test
	public void testDeleteNotAuthorizedException() throws Exception {
		
		try {
			
			Description description = mock(Description.class);
			String storeName = "store";
			String descriptionName = "description";
			
			doReturn(description).when(descriptionDaoMock).findByNameAndStore(storeName, descriptionName);
			when(descriptionAuthMock.canGet(description)).thenReturn(true);
			when(descriptionAuthMock.canDelete(description)).thenReturn(false);
			
			testDeleteException(storeName, descriptionName);
			
			// If the exception is not risen, the method is not properly working
			failBecauseExceptionWasNotThrown(NotAuthorizedException.class);
			
		} catch (NotAuthorizedException ex) {
			
			assertThat(ex.getMessage()).isEqualTo(String.format(
					NOT_AUTHORIZED_BASE, "delete description"));
		}

		
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void testDelete() throws Exception {
		
		Description description = new Description();
		Store store = mock(Store.class);
		Offering offering = generateOffering("uri1", description, "offering1", 
				"Offering 1", "DESC", "uri2", "1.0");
		
		// Add two categories... One should be deleted because it won't be included in other offerings
		final Category catA = generateCategory("CATEGORY_A");
		final Category catB = generateCategory("CATEGORY_B");
		Set<Category> categoriesOldOffering = new HashSet<Category>();
		categoriesOldOffering.add(catA);
		categoriesOldOffering.add(catB);
		offering.setCategories(categoriesOldOffering);
		
		// Add two services... One should be deleted because it won't be included in other offerings
		final Service serA = generateService("uri1");
		final Service serB = generateService("uri2");
		Set<Service> servicesOldOffering = new HashSet<Service>();
		servicesOldOffering.add(serA);
		servicesOldOffering.add(serB);
		offering.setServices(servicesOldOffering);

		// Add the offering to the description
		description.addOffering(offering);
		
		// Configure mocks
		String storeName = "store";
		String descriptionName = "description";
		doReturn(store).when(storeDaoMock).findByName(storeName);
		when(descriptionDaoMock.findByNameAndStore(storeName, descriptionName))
				.thenReturn(description);
		when(descriptionAuthMock.canGet(description)).thenReturn(true);
		when(descriptionAuthMock.canDelete(description)).thenReturn(true);
		
		Session session = mock(Session.class);
		doAnswer(new Answer() {

			@SuppressWarnings("unchecked")
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				
				Set<Offering> noOfferings = mock(Set.class);
				when(noOfferings.size()).thenReturn(0);
				Set<Offering> oneOffering = mock(Set.class);
				when(oneOffering.size()).thenReturn(1);
				
				catA.setOfferings(noOfferings);
				catB.setOfferings(oneOffering);
				serA.setOfferings(oneOffering);
				serB.setOfferings(noOfferings);
				
				return null;
			}
			
		}).when(session).flush();
		when(sessionFactory.getCurrentSession()).thenReturn(session);
		
		// Call the method
		descriptionBo.delete(storeName, descriptionName);
		
		// Verify that the description has been removed from the description
		verify(store).removeDescription(description);
		verify(storeDaoMock).update(store);
		
		// Verify that unused categories and services has been removed
		verify(categoryDao).delete(catA);
		verify(categoryDao, never()).delete(catB);
		verify(serviceDao, never()).delete(serA);
		verify(serviceDao).delete(serB);

	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////// FIND BY NAME /////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	private void testFindByNameNotFoundException(Exception ex) throws Exception {
		
		String storeName = "store";
		String descriptionName = "description";
		doThrow(ex).when(descriptionDaoMock).findByNameAndStore(storeName, descriptionName);
		
		descriptionBo.findByNameAndStore(storeName, descriptionName);
	}
	
	@Test(expected=DescriptionNotFoundException.class)
	public void testFindByNameDescriptionNotFoundException() throws Exception {
		
		testFindByNameNotFoundException(new DescriptionNotFoundException("description not found"));
	}
	
	@Test(expected=StoreNotFoundException.class)
	public void testFindByNameStorenNotFoundException() throws Exception {
		
		testFindByNameNotFoundException(new StoreNotFoundException("store not found"));
	}
	
	
	
	private void testFindByStoreAndName(boolean authorized) throws Exception {
		
		String storeName = "store";
		String descriptionName = "descriptionName";

		Description description = mock(Description.class);
		
		// Set up mocks
		when(descriptionDaoMock.findByNameAndStore(storeName, descriptionName))
				.thenReturn(description);
		when(descriptionAuthMock.canGet(description)).thenReturn(authorized);
		
		// Call the function
		try {
			Description returnedDescription = descriptionBo
					.findByNameAndStore(storeName, descriptionName);

			// If an exception is risen, this check is not executed
			assertThat(returnedDescription).isEqualTo(description);
		
		} catch (Exception ex) {
			throw ex;
		}
	}
	
	@Test
	public void testFinByNameNotAuthorized() throws Exception{
		
		try {
			
			testFindByStoreAndName(false);
			
			// If the exception is not risen, the method is not properly working
			failBecauseExceptionWasNotThrown(NotAuthorizedException.class);
		
		} catch (NotAuthorizedException ex) {
			
			assertThat(ex.getMessage()).isEqualTo(String.format(
					NOT_AUTHORIZED_BASE, "find description"));
		}

	}

	@Test
	public void testFindByName() throws Exception {
		
		testFindByStoreAndName(true);
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////// FIND BY ID /////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
		
	@Test(expected=DescriptionNotFoundException.class)
	public void testFindByIDDescriptionNotFoundException() throws Exception {
		
		int id = 2;
		doThrow(new DescriptionNotFoundException("not found"))
				.when(descriptionDaoMock).findById(id);
		
		descriptionBo.findById(id);
	}
	
	private void testFindById(boolean authorized) throws Exception {
		
		int id = 2;

		Description description = mock(Description.class);
		
		// Set up mocks
		when(descriptionDaoMock.findById(id)).thenReturn(description);
		when(descriptionAuthMock.canGet(description)).thenReturn(authorized);
		
		// Call the function
		try {
			Description returnedDescription = descriptionBo.findById(id);

			// If an exception is risen, this check is not executed
			assertThat(returnedDescription).isEqualTo(description);
		} catch (Exception ex) {
			throw ex;
		}
	}
	
	@Test
	public void testFinByIdNotAuthorized() throws Exception{
		
		try {
			
			testFindById(false);
			
			// If the exception is not risen, the method is not properly working
			failBecauseExceptionWasNotThrown(NotAuthorizedException.class);
		
		} catch (NotAuthorizedException ex) {
			
			assertThat(ex.getMessage()).isEqualTo(String.format(NOT_AUTHORIZED_BASE, "find description"));
		}

	}

	@Test
	public void testFindById() throws Exception {
		
		testFindById(true);
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// GET CURRENT USER DESCRIPTIONS ////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	@Test
	public void testGetCurrentUserDescriptions() throws Exception {
		
		String userName = "userName";
		
		@SuppressWarnings("unchecked")
		List<Description> descriptions = mock(List.class);
		User user = mock(User.class);
		
		
		// Configure mocks
		when(user.getUserName()).thenReturn(userName);
		when(descriptionDaoMock.getUserDescriptions(userName)).thenReturn(descriptions);
		when(userBoMock.getCurrentUser()).thenReturn(user);
		
		// Call the function
		List<Description> returnedDescriptions = descriptionBo.getCurrentUserDescriptions();
		
		// Verifications
		assertThat(returnedDescriptions).isEqualTo(descriptions);
		verify(descriptionDaoMock).getUserDescriptions(userName);
		verify(userBoMock).getCurrentUser();
		
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////// GET ALL DESCRIPTIONS ////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	@Test
	public void testGetAllDescriptions() throws Exception {
		
		@SuppressWarnings("unchecked")
		List<Description> descriptions = mock(List.class);		
		
		// Configure mocks
		when(descriptionDaoMock.getAllDescriptions()).thenReturn(descriptions);
		when(descriptionAuthMock.canList()).thenReturn(true);
		
		// Call the function
		List<Description> returnedDescriptions = descriptionBo.getAllDescriptions();
		
		// Verifications
		assertThat(returnedDescriptions).isEqualTo(descriptions);
		verify(descriptionDaoMock).getAllDescriptions();
		
	}
	
	@Test
	public void testGetAllDescriptionsNotAuthorized() throws Exception {
		
		try {
			
			when(descriptionAuthMock.canList()).thenReturn(false);
			
			// Call the function
			descriptionBo.getAllDescriptions();
			
			// Exception is expected
			failBecauseExceptionWasNotThrown(NotAuthorizedException.class);
			
		} catch (NotAuthorizedException ex) {
			// Check exception message
			assertThat(ex.getMessage()).isEqualTo(String.format(NOT_AUTHORIZED_BASE, "list descriptions"));
		}
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// GET DESCRIPTIONS PAGE ////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	@Test
	public void testGetDescriptionsPage() throws Exception {
		
		
		@SuppressWarnings("unchecked")
		List<Description> descriptions = mock(List.class);		
		
		int offset = 0;
		int max = 103;
		
		// Configure mocks
		when(descriptionDaoMock.getDescriptionsPage(offset, max)).thenReturn(descriptions);
		when(descriptionAuthMock.canList()).thenReturn(true);
		
		// Call the function
		List<Description> returnedDescriptions = descriptionBo.getDescriptionsPage(offset, max);
		
		// Verifications
		assertThat(returnedDescriptions).isEqualTo(descriptions);
		verify(descriptionDaoMock).getDescriptionsPage(offset, max);
		
	}
	
	@Test
	public void testGetDescriptionsPageNotAuthorized() throws Exception {
		
		try {
			
			when(descriptionAuthMock.canList()).thenReturn(false);
			
			// Call the function
			descriptionBo.getDescriptionsPage(0, 7);
			
			// Exception is expected
			failBecauseExceptionWasNotThrown(NotAuthorizedException.class);
			
		} catch (NotAuthorizedException ex) {
			
			// Check exception message
			assertThat(ex.getMessage()).isEqualTo(String.format(NOT_AUTHORIZED_BASE, "list descriptions"));
		}
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// GET ALL DESCRIPTIONS IN STORE ////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	@Test
	public void testGetStoreDescriptions() throws Exception {
		
		@SuppressWarnings("unchecked")
		List<Description> descriptions = mock(List.class);
		Store store = mock(Store.class);
		
		String storeName = "store";
		
		// Configure mocks
		when(store.getDescriptions()).thenReturn(descriptions);
		when(storeDaoMock.findByName(storeName)).thenReturn(store);
		when(descriptionAuthMock.canList(store)).thenReturn(true);
		
		// Call the function
		List<Description> returnedDescriptions = descriptionBo.getStoreDescriptions(storeName);
		
		// Verifications
		assertThat(returnedDescriptions).isEqualTo(descriptions);
		verify(storeDaoMock).findByName(storeName);
	}
	
	@Test
	public void testGetStoreDescriptionsNotAuthorized() throws Exception {
		
		Store store = mock(Store.class);
		String storeName = "store";
		
		when(store.getName()).thenReturn(storeName);
		
		try {
			
			when(storeDaoMock.findByName(storeName)).thenReturn(store);
			when(descriptionAuthMock.canList(store)).thenReturn(false);
			
			// Call the function
			descriptionBo.getStoreDescriptions("store");
			
			// Exception is expected
			failBecauseExceptionWasNotThrown(NotAuthorizedException.class);
			
		} catch (NotAuthorizedException ex) {
			
			// Check exception message
			assertThat(ex.getMessage()).isEqualTo(String.format(NOT_AUTHORIZED_BASE, 
					"list descriptions in store " + storeName));
		}
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// GET DESCRIPTIONS IN STORE PAGE ////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	@Test
	public void testGetStoreDescriptionspAGE() throws Exception {
		
		@SuppressWarnings("unchecked")
		List<Description> descriptions = mock(List.class);
		Store store = mock(Store.class);
		
		String storeName = "store";
		int offset = 1;
		int max = 9;
		
		// Configure mocks
		when(descriptionDaoMock.getStoreDescriptionsPage(storeName, offset, max)).thenReturn(descriptions);
		when(storeDaoMock.findByName(storeName)).thenReturn(store);
		when(descriptionAuthMock.canList(store)).thenReturn(true);
		
		// Call the function
		List<Description> returnedDescriptions = descriptionBo.getStoreDescriptionsPage(storeName, offset, max);
		
		// Verifications
		assertThat(returnedDescriptions).isEqualTo(descriptions);
		verify(storeDaoMock).findByName(storeName);
		verify(descriptionDaoMock).getStoreDescriptionsPage(storeName, offset, max);
	}
	
	@Test
	public void testGetStoreDescriptionsPageNotAuthorized() throws Exception {
		
		String storeName = "store";
		int offset = 7;
		int max = 45;
		
		Store store = mock(Store.class);		
		when(store.getName()).thenReturn(storeName);
		
		try {
			
			when(storeDaoMock.findByName(storeName)).thenReturn(store);
			when(descriptionAuthMock.canList(store)).thenReturn(false);
			
			// Call the function
			descriptionBo.getStoreDescriptionsPage(storeName, offset, max);
			
			// Exception is expected
			failBecauseExceptionWasNotThrown(NotAuthorizedException.class);
			
		} catch (NotAuthorizedException ex) {
			// Check exception message
			assertThat(ex.getMessage()).isEqualTo(String.format(NOT_AUTHORIZED_BASE, 
					"list descriptions in store " + storeName));
		}
	}
}
