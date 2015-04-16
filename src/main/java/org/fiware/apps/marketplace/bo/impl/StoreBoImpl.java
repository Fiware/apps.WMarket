package org.fiware.apps.marketplace.bo.impl;

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

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Path;

import org.apache.commons.codec.binary.Base64;
import org.fiware.apps.marketplace.bo.StoreBo;
import org.fiware.apps.marketplace.bo.UserBo;
import org.fiware.apps.marketplace.controllers.MediaContentController;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("storeBo")
public class StoreBoImpl implements StoreBo{

	@Autowired private StoreDao storeDao;
	@Autowired private StoreAuth storeAuth;
	@Autowired private StoreValidator storeValidator;
	@Autowired private UserBo userBo;
	
	@Value("${media.folder}") private String MEDIA_FOLDER;
	
	private static final String MEDIA_URL = MediaContentController.class.getAnnotation(Path.class).value();
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////// AUXILIAR //////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Gets the name of the file where the image of the store is saved (or where it should be saved)
	 * @param store The store whose image name wants to be retrieved
	 * @return The name of the file that contains the image of the store
	 */
	private String getStoreImageName(Store store) {
		return "store-" + store.getName() + ".png";
	}
	
	/**
	 * Gets the full name where the image of the store is saved (or where it should be saved)
	 * @param store The store whose image path wants to be retrieved
	 * @return The path of the file that contains the image of the store
	 */
	private String getStoreImagePath(Store store) {
		return MEDIA_FOLDER + "/" + getStoreImageName(store);
	}
	
	/**
	 * Decodes the image and saves it in the media folder
	 * @param store The store whose image wants to be decoded and saved
	 */
	private void readAndSaveImage(Store store) {
		
		
		String imageb64 = store.getImageBase64();
		
		if (imageb64 != null) {
			
			String imagePath = getStoreImagePath(store);
			byte[] decodedImage = Base64.decodeBase64(imageb64);
			
			try (FileOutputStream fos = new FileOutputStream(imagePath)) {
				fos.write(decodedImage);
			} catch (Exception e) {
				// This exception will be handled by the controller that will return
				// 500 Internal Server Error
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * Set imagePath based on the Media URL and the name of the store
	 * @param store The store whose imagePath wants to be set
	 */
	private void setImageURL(Store store) {
		if (new File(getStoreImagePath(store)).exists()) {
			store.setImagePath(MEDIA_URL + "/" + getStoreImageName(store));
		}
	}
	
	/**
	 * Set imagePath based on the Media URL and the name of the store
	 * @param stores The list of stores whose imagePath wants to be set
	 */
	private void setImageURL(List<Store> stores) {
		for (Store store: stores) {
			setImageURL(store);
		}
	}

	
	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////// PUBLIC METHODS ///////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////

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
			
			// Set default name based on the display name
			store.setName(NameGenerator.getURLName(store.getDisplayName()));
	
			// Check rights and raise exception if user is not allowed to perform this action
			if (!storeAuth.canCreate(store)) {
				throw new NotAuthorizedException("create store");
			}
			
			// Exception is risen if the store is not valid
			storeValidator.validateNewStore(store);
			
			// Save the image
			readAndSaveImage(store);
			
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
			
			Store storeToBeUpdate = storeDao.findByName(storeName);
			
			// Check rights and raise exception if user is not allowed to perform this action
			if (!storeAuth.canUpdate(storeToBeUpdate)) {
				throw new NotAuthorizedException("update store");
			}
			
			// Exception is risen if the store is not valid
			// Store returned by the BBDD cannot be updated if the updated store is not valid.
			storeValidator.validateUpdatedStore(storeToBeUpdate, updatedStore);
			
			// Update fields
			if (updatedStore.getUrl() != null) {
				storeToBeUpdate.setUrl(updatedStore.getUrl());
			}

			if (updatedStore.getComment() != null) {
				storeToBeUpdate.setComment(updatedStore.getComment());
			}

			if (updatedStore.getDisplayName() != null) {
				storeToBeUpdate.setDisplayName(updatedStore.getDisplayName());
			}
			
			if (updatedStore.getImageBase64() != null) {
				storeToBeUpdate.setImageBase64(updatedStore.getImageBase64());
				readAndSaveImage(storeToBeUpdate);
			}
			
			storeToBeUpdate.setLasteditor(user);
			
			// Save the image
			
			storeDao.update(storeToBeUpdate);
			
		} catch (UserNotFoundException ex) {
			// This exception is not supposed to happen
			throw new RuntimeException(ex);
		}
		
	}

	@Override
	@Transactional(readOnly=false)
	public void delete(String storeName) throws NotAuthorizedException, StoreNotFoundException {
		
		Store store = storeDao.findByName(storeName);
		
		// Check rights and raise exception if user is not allowed to perform this action
		if (!storeAuth.canDelete(store)) {
			throw new NotAuthorizedException("delete store");
		}
		
		// When the store is deleted, its image must be deleted
		new File(getStoreImagePath(store)).delete();
		
		storeDao.delete(store);
		
	}

	@Override
	@Transactional
	public Store findByName(String name) throws StoreNotFoundException, 
			NotAuthorizedException {
		
		Store store = storeDao.findByName(name);
		
		// Check rights and raise exception if user is not allowed to perform this action
		if (!storeAuth.canGet(store)) {
			throw new NotAuthorizedException("find store");
		}
		
		// Set store icon URL
		setImageURL(store);
		
		return store;
	}
	
	@Override
	@Transactional
	public List<Store> getStoresPage(int offset, int max) 
			throws NotAuthorizedException {
		
		// Check rights and raise exception if user is not allowed to perform this action
		if (!storeAuth.canList()) {
			throw new NotAuthorizedException("list stores");
		}
		
		// Set image path
		List<Store> stores = storeDao.getStoresPage(offset, max);
		
		// Set store icon URL
		setImageURL(stores);
		
		return stores;
	}
	
	@Override
	@Transactional
	public List<Store> getAllStores() throws NotAuthorizedException {
		
		// Check rights and raise exception if user is not allowed to perform this action
		if (!storeAuth.canList()) {
			throw new NotAuthorizedException("list stores");
		}
		
		// Set image path
		List<Store> stores = storeDao.getAllStores();
		
		// Set store icon URL
		setImageURL(stores);
		
		return stores;
	}
}
