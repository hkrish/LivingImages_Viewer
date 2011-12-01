/**
 * 
 */
package livingimagesviewer;

import java.util.ArrayList;

/**
 * Type - Person
 * 
 * @author hari 21 Nov 2011
 * 
 * 
 */
public class Person {
	String name = "", urlfacebook = "", urltwitter = "", urlflickr = "";
	
	ArrayList<String> baseimages = new ArrayList<String>();
	
	Person() {
		name = "";
		urlfacebook = "";
		urltwitter = "";
		urlflickr = "";
		baseimages = new ArrayList<String>();
	}
	
	Person(String name, String fb, String fkr, String twi) {
		this.name = name;
		this.urlfacebook = fb;
		this.urlflickr = fkr;
		this.urltwitter = twi;
		baseimages = new ArrayList<String>();
	}
}
