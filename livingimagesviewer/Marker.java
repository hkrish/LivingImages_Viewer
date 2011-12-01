/**
 * 
 */
package livingimagesviewer;

import java.awt.Image;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import toxi.geom.Rect;
import toxi.geom.Triangle2D;
import toxi.geom.Vec2D;

/**
 * Type - Marker
 * 
 * @author hari 18 Nov 2011
 * 
 * 
 */
public class Marker {
	BufferedImage mask = null, sample = null;
	Vec2D center = null;
	Boolean person = true;
	Triangle2D faceTriangle = null;
	Rect fullrect = null, facerect = null;
	
	Path2D mask2d = null;
	
	String name = "", urlfacebook = "", urltwitter = "", urlflickr = "";
	int miscdata = 0, miscdata1 = 0;
}
