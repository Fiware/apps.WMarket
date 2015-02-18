package org.fiware.apps.marketplace.dao.impl;

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

import java.util.List;

import org.fiware.apps.marketplace.dao.OfferingDao;
import org.fiware.apps.marketplace.exceptions.OfferingNotFoundException;
import org.fiware.apps.marketplace.model.Offering;
import org.fiware.apps.marketplace.utils.MarketplaceHibernateDao;
import org.springframework.stereotype.Service;

@Service("offeringDao")
public class OfferingDaoImpl extends MarketplaceHibernateDao implements OfferingDao  {
	
	private static final String TABLE_NAME = Offering.class.getName();

	@Override
	public void save(Offering offering) {
		getSession().save(offering);
	}

	@Override
	public void update(Offering offering) {
		getSession().update(offering);
	}

	@Override
	public void delete(Offering offering) {
		getSession().delete(offering);
	}

	@Override
	public Offering findByStoreDescriptionAndStore(String storeName, String descriptionName, 
			String offeringName) throws OfferingNotFoundException {
		
		List<?> offerings = getSession().createQuery("from " + TABLE_NAME + " WHERE "
				+ "describedIn.name = :descriptionName, describedIn.store.name = :storeName "
				+ "AND name = :offeringName;")
				.setParameter("storeName", storeName)
				.setParameter("descriptionName", descriptionName)
				.setParameter("offeringName", offeringName)
				.list();
		
		if (offerings.size() == 0) {
			throw new OfferingNotFoundException("Offering " + 
					offeringName + " not found in description " + descriptionName);
		} else {
			return (Offering) offerings.get(0);
		}
	}

	@Override
	public List<Offering> getAllOfferings() {
		return getOfferingsPage(0, Integer.MAX_VALUE);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Offering> getOfferingsPage(int offset, int max) {
		return getSession()
				.createCriteria(Offering.class)
				.setFirstResult(offset)
				.setMaxResults(max)
				.list();
	}

	@Override
	public List<Offering> getAllStoreOfferings(String storeName) {
		return getStoreOfferingsPage(storeName, 0, Integer.MAX_VALUE);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Offering> getStoreOfferingsPage(String storeName, int offset,	
			int max) {
		
		return getSession().createQuery("FROM " + TABLE_NAME + " WHERE describedIn.store.name = :storeName;")
				.setParameter("storeName", storeName)
				.setFirstResult(offset)
				.setMaxResults(max)
				.list();
	}

	@Override
	public List<Offering> getAllDescriptionOfferings(String storeName, String descriptionName) {
		return getDescriptionOfferingsPage(storeName, descriptionName, 0, Integer.MAX_VALUE);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Offering> getDescriptionOfferingsPage(String storeName, String descriptionName,
			int offset, int max) {
		return getSession().createQuery("FROM " + TABLE_NAME + " "
				+ "WHERE describedIn.name = :descriptionName AND describedIn.store.name = :storeName;")
				.setParameter("descriptionName", descriptionName)
				.setParameter("storeName", storeName)
				.setFirstResult(offset)
				.setMaxResults(max)
				.list();
	}

}