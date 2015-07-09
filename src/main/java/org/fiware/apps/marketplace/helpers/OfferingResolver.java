package org.fiware.apps.marketplace.helpers;

/*
 * #%L
 * FiwareMarketplace
 * %%
 * Copyright (C) 2012 SAP
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fiware.apps.marketplace.bo.CategoryBo;
import org.fiware.apps.marketplace.bo.ServiceBo;
import org.fiware.apps.marketplace.exceptions.CategoryNotFoundException;
import org.fiware.apps.marketplace.exceptions.ServiceNotFoundException;
import org.fiware.apps.marketplace.model.Category;
import org.fiware.apps.marketplace.model.Offering;
import org.fiware.apps.marketplace.model.Description;
import org.fiware.apps.marketplace.model.PriceComponent;
import org.fiware.apps.marketplace.model.PricePlan;
import org.fiware.apps.marketplace.model.Service;
import org.fiware.apps.marketplace.model.Store;
import org.fiware.apps.marketplace.rdf.RdfHelper;
import org.fiware.apps.marketplace.utils.NameGenerator;
import org.springframework.beans.factory.annotation.Autowired;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Class to resolve offerings from store or service instances.
 * 
 * @author D058352
 *
 */
@org.springframework.stereotype.Service("offeringResolver")
public class OfferingResolver {
	
	@Autowired private RdfHelper rdfHelper;
	@Autowired private CategoryBo classificationBo;
	@Autowired private ServiceBo serviceBo;
	
	/**
	 * Gets all the offerings from a USDL
	 * @param model The USDL
	 * @return The offerings list contained in the USDL
	 */
	private List<String> getOfferingUris(Model model) {
		String query = RdfHelper.getQueryPrefixes() + "SELECT ?x WHERE { ?x a usdl:ServiceOffering . } ";
		return rdfHelper.queryUris(model, query, "x");
	}
 
	/**
	 * Gets all the services associated with the offering
	 * @param offeringUri The offering whose services want to be retrieved
	 * @param model The UDSL
	 * @return The list of services associated with the offering
	 */
	private List<String> getServiceUris(Model model, String offeringUri) {
		return rdfHelper.getObjectUris(model, offeringUri, "usdl:includes");
	}

	/**
	 * Gets all the price plans URIs associated with the offering
	 * @param model UDSL
	 * @param offeringUri The offering whose price plans want to be retrieved
	 * @return The list of price plans URIs associated with the offering
	 */
	private List<String> getPricePlanUris(Model model, String offeringUri) {
		return rdfHelper.getObjectUris(model, offeringUri, "usdl:hasPricePlan");
	}
	
	/**
	 * Get all the price components URIs associated to a price component
	 * @param model USDL
	 * @param pricePlanUri The price plan whose price components want to be retrieved
	 * @return The list of price components URIs associated to the price plan
	 */
	private List<String> getPriceComponentsUris(Model model, String pricePlanUri) {
		return rdfHelper.getObjectUris(model, pricePlanUri, "price:hasPriceComponent");
	}
	
	/**
	 * Get all the classifications associated to a service
	 * @param model USDL
	 * @param serviceURI The service whose classifications want to be retrieved
	 * @return The list of classifications associated to the service
	 */
	private List<String> getServiceClassifications(Model model, String serviceURI) {
		return rdfHelper.getBlankNodesLabels(model, serviceURI, "usdl:hasClassification");
	}
	
	/**
	 * Gets the title of an entity
	 * @param model USDL
	 * @param entityURI The entity URI whose title wants to be retrieved
	 * @return The title of the entity
	 */
	private String getTitle(Model model, String entityURI) {
		return rdfHelper.getLiteral(model, entityURI, "dcterms:title");
	}
	
	/**
	 * Gets the description of an entity
	 * @param model USDL
	 * @param entityURI The entity URI whose description wants to be retrieved
	 * @return The description of the entity
	 */
	private String getDescription(Model model, String entityURI) {
		return rdfHelper.getLiteral(model, entityURI, "dcterms:description");
	}
	
	/**
	 * Get the version of an offering 
	 * @param model USDL
	 * @param offeringUri The offering URI whose version wants to be retrieved
	 * @return The version of the offering
	 */
	private String getOfferingVersion(Model model, String offeringUri) {
		return rdfHelper.getLiteral(model, offeringUri, "usdl:versionInfo");
	}
	
	/**
	 * Gets the image of an offering
	 * @param model USDL
	 * @param offeringUri The offering URI whose image URL wants to be retrieved
	 * @return The image URL of the offering
	 */
	private String getOfferingImageUrl(Model model, String offeringUri) {
		String url = rdfHelper.getObjectUri(model, offeringUri, "foaf:thumbnail");
		// Remove '<' from the beginning and '>' from the end
		return url.substring(1, url.length() - 1);
	}
	
	/**
	 * Gets all offerings contained in the service descriptions in the given list of stores.
	 * @param stores
	 * @return
	 */
	public List<Offering> resolveOfferingsFromStores(List<Store> stores) {
		List<Offering> offerings = new ArrayList<Offering>();
		for (Store store : stores) {
			offerings.addAll(resolveOfferingsFromStore(store));
		}
		return offerings;
	}

	/**
	 * Gets all offerings contained in the service descriptions in the given store.
	 * @param store
	 * @return
	 */
	public List<Offering> resolveOfferingsFromStore(Store store) {
		return resolveOfferingsFromServiceDescriptions(store.getDescriptions());
	}

	/**
	 * Gets all offerings contained in the given service descriptions.
	 * @param offeringDescriptions
	 * @return
	 */
	public List<Offering> resolveOfferingsFromServiceDescriptions(List<Description> offeringDescriptions) {
		List<Offering> offerings = new ArrayList<Offering>();
		for (Description service : offeringDescriptions) {
			offerings.addAll(resolveOfferingsFromServiceDescription(service));
		}
		return offerings;
	}

	/**
	 * Gets all offerings contained in the file in the given URI.
	 * @param uri
	 * @return
	 */
	public List<Offering> resolveOfferingsFromServiceDescription(Description offeringDescription) {
		
		Model model = rdfHelper.getModelFromUri(offeringDescription.getUrl());
		
		// Just in case the model cannot be processed
		if (model == null) {
			return Collections.emptyList();
		}

		List<Offering> offerings = new ArrayList<Offering>();
		List<String> offeringUris = getOfferingUris(model);
		
		// Classifications cache: To avoid SQL Constraint errors when the description contains 
		// two or more offerings with the same classification
		Map<String, Category> createdClassifications = new HashMap<String, Category>();
		
		// Services cache: To avoid SQL Constraint errors when the description contains
		// two or more offerings with the same service
		Map<String, Service> createdServices = new HashMap<String, Service>();
		
		for (String offeringUri : offeringUris) {
			
			Offering offering = new Offering();
			offering.setDisplayName(getTitle(model, offeringUri));
			// Maybe the name should depends on the creator and the version...
			offering.setName(NameGenerator.getURLName(offering.getDisplayName()));
			// Remove '<' from the beginning and '>' from the end
			offering.setUri(offeringUri.substring(1, offeringUri.length() - 1));
			offering.setDescribedIn(offeringDescription);
			offering.setVersion(getOfferingVersion(model, offeringUri));
			offering.setDescription(getDescription(model, offeringUri));
			offering.setImageUrl(getOfferingImageUrl(model, offeringUri));
			
			// PRICE PLANS (offerings contain one or more price plans)
			List<String> pricePlansUris = getPricePlanUris(model, offeringUri);
			Set<PricePlan> pricePlans = new HashSet<>();
			
			for (String pricePlanUri: pricePlansUris) {
				
				PricePlan pricePlan = new PricePlan();
				pricePlan.setTitle(getTitle(model, pricePlanUri));
				pricePlan.setComment(getDescription(model, pricePlanUri));
				pricePlan.setOffering(offering);
				
				List<String> priceComponentsUris = getPriceComponentsUris(model, pricePlanUri);
				Set<PriceComponent> priceComponents = new HashSet<>();
				
				for (String priceComponentUri: priceComponentsUris) {
			
					PriceComponent priceComponent = new PriceComponent();
					priceComponent.setPricePlan(pricePlan);
					priceComponent.setTitle(getTitle(model, priceComponentUri));
					priceComponent.setComment(getDescription(model, priceComponentUri));
					priceComponent.setCurrency(rdfHelper.getLiteral(model, priceComponentUri, "gr:hasCurrency"));
					priceComponent.setUnit(rdfHelper.getLiteral(model, priceComponentUri, "gr:hasUnitOfMeasurement"));
					priceComponent.setValue(Float.parseFloat(
							rdfHelper.getLiteral(model, priceComponentUri, "gr:hasCurrencyValue")));
					
					priceComponents.add(priceComponent);
				}
				
				// Update price components with the retrieved price components
				pricePlan.setPriceComponents(priceComponents);
				
				pricePlans.add(pricePlan);
			}
			
			// Update the price plans set with the retrieved price plans
			offering.setPricePlans(pricePlans);
			
			// SERVICES
			List<String> servicesUris = getServiceUris(model, offeringUri);
			Set<Service> offeringServices = new HashSet<>();
			Set<Category> offeringClassification = new HashSet<>();
			
			for (String serviceUri: servicesUris) {
				
				Service service;
				
				// Remove '<' from the beginning and '>' from the end
				String parserServiceUri = serviceUri.substring(1, serviceUri.length() - 1);
				
				// Try to get the service from the database
				try {
					service = serviceBo.findByURI(parserServiceUri);
				} catch (ServiceNotFoundException e) {
					// Look for another offering in this description that contains the same service.
					// Otherwise, a new service is created
					service = createdServices.get(parserServiceUri);
				}
				
				// If service is still null, create a new one
				if (service == null) {
					service = new Service();
				}
				
				// Service basic properties
				service.setUri(parserServiceUri);
				service.setDisplayName(getTitle(model, serviceUri));
				service.setComment(getDescription(model, serviceUri));
				
				// Service classifications (a service can have more than one classification)
				Set<Category> serviceClassifications = new HashSet<>();
				List<String> classificationsDisplayNames = getServiceClassifications(model, serviceUri);
				
				for (String classificationDisplayName: classificationsDisplayNames) {
					
					Category classification;
					String classificationName = NameGenerator.getURLName(classificationDisplayName);
					
					// Try to get the service from the database
					try {
						classification = classificationBo.findByName(classificationName);
					} catch (CategoryNotFoundException e1) {
						// Look for another offering/service in this description that contains
						// the same classification. Otherwise, a new classification is created.
						classification = createdClassifications.get(classificationName);
					}
											
					// If classification is still null, create a new one
					if (classification == null) {
						classification = new Category();
					}
					
					classification.setName(classificationName);
					classification.setDisplayName(classificationDisplayName);
					createdClassifications.put(classificationName, classification);
					
					serviceClassifications.add(classification);
				}
				
				service.setCategories(serviceClassifications);
				
				createdServices.put(parserServiceUri, service);

				offeringClassification.addAll(service.getCategories());
				offeringServices.add(service);
			}
			
			// Attach the services to the offering
			offering.setServices(offeringServices);
			offering.setCategories(offeringClassification);
			
			// Update the list of offerings
			offerings.add(offering);
			
		}
		
		return offerings;
	}
}
