/**
 * 
 */
package livingimagesviewer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.PriorityQueue;

import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import toxi.geom.Vec2D;

/**
 * Type - Liveimage
 * 
 * @author hari 28 Nov 2011
 * 
 * 
 */
public class LiveImage_BKUP {
	protected Directory directory = null;
	protected BaseImageFactory parent = null;
	protected BaseImage baseImage = null;
	
	// All the persons in this image
	ArrayList<Person> persons = null;
	ImageLayerList images = null;
	ArrayList<Marker> markers = null;
	
	private Date baseDateTime = null;
	
	private float currentT = 1f;
	
	/**
	 * 
	 */
	public LiveImage_BKUP(Directory directory, String fname) {
		super();
		
		this.directory = directory;
		this.parent = directory.baseFactory;
		
		this.baseImage = new BaseImage(parent, fname);
		if (baseImage.LoadingFailed)
			this.baseImage = null;
		else {
			// OK do the stuff here!
			currentT = 1f;
			this.baseDateTime = BaseImage.getDateTime(this.baseImage);
			
			persons = new ArrayList<Person>();
			markers = new ArrayList<Marker>();
			// Find all persons in the baseImage
			for (Marker m : this.baseImage.markers) {
				persons.add(this.directory.get(m.name));
				markers.add(m);
			}
			
			// Find all the base images for persons!
			// DEBUG find all persons since the date of creation of the BaseImage above. Now it adds all images
			images = new ImageLayerList();
			HashMap<String, BaseImage> bimages = new HashMap<String, BaseImage>();
			BaseImage bimg;
			for (Person p : persons) {
				for (String bi : p.baseimages) {
					if (bimages.containsKey(bi))
						bimg = bimages.get(bi);
					else {
						bimg = parent.createBaseImage(bi);
						bimages.put(bi, bimg);
					}
					
					ImageLayer imgl;
					if (bimg.getFilename().equalsIgnoreCase(baseImage.getFilename())) {
						//						imgl = new ImageLayer(bimg, p.name, true);
					} else {
						imgl = new ImageLayer(bimg, p.name, false);
						if (imgl.marker.sample != null)
							images.add(imgl);
					}
					
				}
				System.out.println(p.name + " - " + images.size());
			}
		}
	}
	
	public void draw(PGraphics pg) {
		if (markers == null || markers.isEmpty())
			return;
		
		Marker m;
		Float sf = 0f;
		
		//		pg.beginDraw();
		pg.background(0);
		
		pg.pushMatrix();
		// Scale image first
		pg.scale(getScaleFactor(pg, baseImage));
		pg.image(baseImage, 0, 0);
		
		// TODO Update according to currentT value
		for (ImageLayer il : images) {
			m = getmarker(il.marker.name);
			if (m == null)
				continue;
			
			sf = m.fullrect.width / (float) il.marker.fullrect.width;
			Vec2D tmpm = il.marker.center.sub(il.marker.fullrect.getTopLeft()).scale(sf);
			pg.pushMatrix();
			pg.translate(m.center.x - tmpm.x, m.center.y - tmpm.y);
			pg.scale(sf);
			pg.image(il.image, 0, 0);
			pg.popMatrix();
		}
		
		pg.popMatrix();
		//		pg.endDraw();
	}
	
//	public void draw(PGraphics pg) {
//		if (markers == null || markers.isEmpty())
//			return;
//		
//		Marker m;
//		Float sf = 0f;
//		
//		//		pg.beginDraw();
//		pg.background(0);
//		
//		pg.pushMatrix();
//		// Scale image first
//		pg.scale(getScaleFactor(pg, baseImage));
//		pg.image(baseImage, 0, 0);
//		
//		for (ImageLayer il : images) {
//			m = getmarker(il.marker.name);
//			if (m == null)
//				continue;
//			
//			sf = m.fullrect.width / (float) il.marker.fullrect.width;
//			Vec2D tmpm = il.marker.center.sub(il.marker.fullrect.getTopLeft()).scale(sf);
//			pg.pushMatrix();
//			pg.translate(m.center.x - tmpm.x, m.center.y - tmpm.y);
//			pg.scale(sf);
//			pg.image(il.image, 0, 0);
//			pg.popMatrix();
//			
//			System.out.println(m.center.x + " , " + tmpm.x + " , " + (m.center.x - tmpm.x));
//		}
//		
//		pg.popMatrix();
//		//		pg.endDraw();
//	}
	
	private Marker getmarker(String name) {
		for (Marker m : markers)
			if (m.name.equalsIgnoreCase(name))
				return m;
		return null;
	}
	
	private float getScaleFactor(PGraphics pg, BaseImage bimg) {
		if (bimg.width > bimg.height)
			return pg.width / (float) bimg.width;
		else
			return pg.height / (float) bimg.height;
	}
	
	/**
	 * Type - ImageLayer
	 * 
	 * @author hari 28 Nov 2011
	 * 
	 * 
	 */
	private class ImageLayer implements Comparable<ImageLayer> {
		//		BaseImage bImage = null;
		Marker marker = null;
		PImage image = null;
		
		boolean base = false;
		
		Date dateTime = null;
		
		/**
		 * 
		 */
		public ImageLayer(BaseImage bimg, Marker m, boolean b) {
			if (bimg != null && m != null) {
				//				this.bImage = bimg;
				this.marker = m;
				
				base = b;
				dateTime = BaseImage.getDateTime(bimg);
				loadImage();
			}
		}
		
		public ImageLayer(BaseImage bimg, String name, boolean b) {
			if (bimg != null && !name.equals("")) {
				//				this.bImage = bimg;
				for (Marker m : bimg.markers)
					if (m.name.equalsIgnoreCase(name))
						this.marker = m;
				
				base = b;
				dateTime = BaseImage.getDateTime(bimg);
				loadImage();
			}
		}
		
		private void loadImage() {
			image = new PImage(marker.sample.getWidth(), marker.sample.getHeight(), PConstants.ARGB);
			marker.sample.getRGB(0, 0, image.width, image.height, image.pixels, 0, image.width);
			image.updatePixels();
		}
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(ImageLayer o) {
			//			if (this.base)
			//				return -1;
			if (this.dateTime.before(o.dateTime))
				return -1;
			if (this.dateTime.after(o.dateTime))
				return 1;
			return 0;
		}
	}
	
	private class ImageLayerList extends PriorityQueue<ImageLayer> {
		/**
		 * 
		 */
		public ImageLayerList() {
			super();
		}
	}
	
}
