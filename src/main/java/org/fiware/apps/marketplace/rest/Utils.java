package org.fiware.apps.marketplace.rest;

import java.text.Normalizer;

public class Utils {
	
	public static String getURLName(String name) {
		
		// Normalize the String
		name = Normalizer
				.normalize(name, Normalizer.Form.NFD)
				.replaceAll("[^\\p{ASCII}]", "");
		
		// Remove special characters ("-" are preserved")
		name = name.replaceAll("[^a-zA-Z0-9\\s\\-]+", "");

		// Replace " " by "-"
		name = name.replaceAll("[\\s\\-]+", "-");
		
		// Lower case
		return name.toLowerCase();
	}

}
