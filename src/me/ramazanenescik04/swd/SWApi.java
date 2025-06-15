package me.ramazanenescik04.swd;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * SitWatch Downloader API
 * 
 * This class serves as a placeholder for the SitWatch Downloader API.
 * It can be expanded with methods and properties to interact with the SitWatch service.
 * 
 * @author Ramazanenescik04
 * @version 1.0
 */
public class SWApi {

	// Private constructor to prevent instantiation
	private SWApi() {
	}
	
	public static final String API_URL = "https://sitwatch.net/api"; // Example API URL
	
	public static URI getVideoURI(int videoId) {
		try {
			return new URI(API_URL + "/videos/" + videoId);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static boolean isNumericSpace(final CharSequence cs) {
		if (cs == null) {
	    	return false;
	    }
	    final int sz = cs.length();
		     for (int i = 0; i < sz; i++) {
		         if (!Character.isDigit(cs.charAt(i))) {
		             return false;
		         }
		}
		return true;
	}
	
	public static int getVideoID(String url) {
		if (isNumericSpace(url)) {
			return Integer.parseInt(url);
		}
		
		url.replaceAll("http:", "https:"); // Ensure URL is secure
		String[] parts = url.split("/");
		
		if (parts.length < 3 || !parts[2].equals("sitwatch.net") || !(parts[3].equals("watch") || parts[3].equals("swipe") || parts[3].equals("api"))) {
			System.err.println("Invalid SitWatch URL: " + url);
			return -1; // Invalid URL
		}
		
		if (parts[4].equals("videos"))
			return Integer.parseInt(parts[5]); // Extract video ID from URL
		else 
			return Integer.parseInt(parts[4]); // Extract video ID from URL
	}

}
