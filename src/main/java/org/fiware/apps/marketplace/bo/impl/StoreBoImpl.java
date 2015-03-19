package org.fiware.apps.marketplace.bo.impl;

/*
 * #%L
 * FiwareMarketplace
 * %%
 * Copyright (C) 2012 SAP
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

import java.util.Date;
import java.util.List;

import org.fiware.apps.marketplace.bo.StoreBo;
import org.fiware.apps.marketplace.bo.UserBo;
import org.fiware.apps.marketplace.dao.StoreDao;
import org.fiware.apps.marketplace.exceptions.NotAuthorizedException;
import org.fiware.apps.marketplace.exceptions.StoreNotFoundException;
import org.fiware.apps.marketplace.exceptions.UserNotFoundException;
import org.fiware.apps.marketplace.exceptions.ValidationException;
import org.fiware.apps.marketplace.model.Store;
import org.fiware.apps.marketplace.model.User;
import org.fiware.apps.marketplace.model.validators.StoreValidator;
import org.fiware.apps.marketplace.security.auth.StoreAuth;
import org.fiware.apps.marketplace.utils.NameGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("storeBo")
public class StoreBoImpl implements StoreBo{

	@Autowired private StoreDao storeDao;
	@Autowired private StoreAuth storeAuth;
	@Autowired private StoreValidator storeValidator;
	@Autowired private UserBo userBo;

	@Override
	@Transactional(readOnly=false)
	public void save(Store store) throws NotAuthorizedException, 
			ValidationException {

		try {
			// Get the currently logged-in user
			User user = userBo.getCurrentUser();
	
			// Set the current date as registration date of this store
			store.setRegistrationDate(new Date());
	
			// Set user as creator and latest editor of this store
			store.setCreator(user);
			store.setLasteditor(user);
	
			// Exception is risen if the user is not allowed
			storeAuth.canCreate(store);
			
			// Exception is risen if the store is not valid
			storeValidator.validateStore(store, true);
			
			store.setName(NameGenerator.getURLName(store.getDisplayName()));
			storeDao.save(store);
		} catch (UserNotFoundException ex) {
			// This exception is not supposed to happen
			throw new RuntimeException(ex);
		}
		
	}

	@Override
	@Transactional(readOnly=false)
	public void update(String storeName, Store updatedStore) throws NotAuthorizedException, 
			ValidationException, StoreNotFoundException {
		
		try {
			// Get the currently logged-in user
			User user = userBo.getCurrentUser();
			
			Store storeToUpdate = this.findByName(storeName);								

			// Exception is risen if user is not allowed
			storeAuth.canUpdate(storeToUpdate);
			
			// Exception is risen if the store is not valid
			// Store returned by the BBDD cannot be updated if the updated store is not valid.
			storeValidator.validateStore(updatedStore, false);
			
			// Update fields
			if (updatedStore.getUrl() != null) {
				storeToUpdate.setUrl(updatedStore.getUrl());
			}

			if (updatedStore.getDescription() != null) {
				storeToUpdate.setDescription(updatedStore.getDescription());
			}

			if (updatedStore.getDisplayName() != null) {
				storeToUpdate.setDisplayName(updatedStore.getDisplayName());
			}
			
			storeToUpdate.setLasteditor(user);
			
			storeDao.update(storeToUpdate);
			
		} catch (UserNotFoundException ex) {
			// This exception is not supposed to happen
			throw new RuntimeException(ex);
		}
		
	}

	@Override
	@Transactional(readOnly=false)
	public void delete(String storeName) throws NotAuthorizedException, StoreNotFoundException {
		Store store = this.findByName(storeName);
		// Check rights (exception is risen if user is not allowed)
		storeAuth.canDelete(store);
		storeDao.delete(store);
		
	}

	@Override
	@Transactional
	public Store findByName(String name) throws StoreNotFoundException, 
			NotAuthorizedException {
		
		Store store = storeDao.findByName(name);
		
		// Exception is risen if user is not allowed
		storeAuth.canGet(store);
		
		return store;
	}
	
	@Override
	@Transactional
	public List<Store> getStoresPage(int offset, int max) 
			throws NotAuthorizedException {
		
		// Exception is risen if user is not allowed
		storeAuth.canList();
		
		return storeDao.getStoresPage(offset, max);
	}
	
	@Override
	@Transactional
	public List<Store> getAllStores() throws NotAuthorizedException {
		
		// Exception is risen if user is not allowed
		storeAuth.canList();
		
		return storeDao.getAllStores();
	}
}
