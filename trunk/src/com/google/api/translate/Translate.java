/**
 * Translate.java
 *
 * Copyright (C) 2007,  Richard Midwinter
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.google.api.translate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Makes the Google Translate API available to Java applications.
 * 
 * @author Richard Midwinter
 * @author Emeric Vernat
 * @author Juan B Cabral
 */
public class Translate {
	
    private static final String ENCODING = "UTF-8";
    private static final String INTERMEDIATE_LANGUAGE = Language.ENGLISH;
    private static final String URL_STRING = "http://translate.google.com/translate_t?langpair=";
    private static final String TEXT_VAR = "&text=";
    private static final int RATE_DELAY = 2000;
    
    private static boolean rateControl = true;
    private static long lastQueryTime = 0l;
    
    /**
     * Are we throttling queries to prevent being blocked? Defaults to true.
     * 
     * @return Returns true if throttling query frequency.
     */
    public static boolean isUsingRateControl() {
    	return rateControl;
    }
    
    /**
     * Allows turning off rate control
     * 
     * @param rateControl Turns on rate control if true, off if false.
     */
    public static void setUsingRateControl(boolean rateControl) {
    	Translate.rateControl = rateControl;
    }

    /**
     * Reads an InputStream and returns its contents as a String. Also effects rate control.
     * @param inputStream The InputStream to read from.
     * @return The contents of the InputStream as a String.
     * @throws Exception
     */
    private static String toString(InputStream inputStream) throws Exception {
    	StringBuilder outputBuilder = new StringBuilder();
    	try {
    		while (rateControl && (lastQueryTime+RATE_DELAY > System.currentTimeMillis())) {
    			try {
    				Thread.sleep(lastQueryTime+RATE_DELAY-System.currentTimeMillis());
    			} catch (InterruptedException e) {
    				System.out.println("[google-api-translate-java] Interrupted sleep.");
    			}
    		}
    		String string;
    		if (inputStream != null) {
    			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, ENCODING));
    			while (null != (string = reader.readLine())) {
    				outputBuilder.append(string).append('\n');
    			}
    		}
    		if (rateControl) lastQueryTime = System.currentTimeMillis();
    	} catch (Exception ex) {
    		throw new Exception("[google-api-translate-java] Error reading translation stream.", ex);
    	}
    	return outputBuilder.toString();
    }

    /**
     * Translates text from a given language to another given language using Google Translate
     * 
     * @param text The String to translate.
     * @param from The language code to translate from.
     * @param to The language code to translate to.
     * @return The translated String.
     * @throws MalformedURLException
     * @throws IOException
     */
    public static String translate(String text, String from, String to) throws Exception {
    	if (Language.isValidLanguagePair(from, to)) {
    		return retrieveTranslation(text, from, to);
    	} else {
    		return retrieveTranslation(retrieveTranslation(text, from, INTERMEDIATE_LANGUAGE), INTERMEDIATE_LANGUAGE, to);
    	}
    }
    /**
     * Forms an HTTP request and parses the response for a translation.
     * 
     * @param text The String to translate.
     * @param from The language code to translate from.
     * @param to The language code to translate to.
     * @return The translated String.
     * @throws Exception
     */
    private static String retrieveTranslation(String text, String from, String to) throws Exception {
    	try {
    		StringBuilder url = new StringBuilder();
    		url.append(URL_STRING).append(from).append('|').append(to);
    		url.append(TEXT_VAR).append(URLEncoder.encode(text, ENCODING));

    		HttpURLConnection uc = (HttpURLConnection) new URL(url.toString()).openConnection();
    		try {
    			uc.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1)");
    			String page = toString(uc.getInputStream());

    			int resultBox = page.indexOf("<div id=result_box dir=");        
    			if (resultBox < 0) throw new Error("No translation result returned.");

    			String start = page.substring(resultBox);
    			return start.substring(start.indexOf('>')+1, start.indexOf("</div>"));
    		} finally { // http://java.sun.com/j2se/1.5.0/docs/guide/net/http-keepalive.html
    			uc.getInputStream().close();
    			if (uc.getErrorStream() != null) uc.getErrorStream().close();
    		}
    	} catch (Exception ex) {
    		throw new Exception("[google-api-translate-java] Error retrieving translation.", ex);
    	}
    }
}