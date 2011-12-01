/**
 * 
 */
package livingimagesviewer;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.PriorityQueue;

import javax.media.jai.Histogram;
import javax.media.jai.JAI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import sun.java2d.loops.DrawLine;
import toxi.geom.Vec2D;

/**
 * Type - Liveimage
 * 
 * @author hari 28 Nov 2011
 * 
 * 
 */
public class LiveImage {
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
		
		private void loadImage() {
			image = new PImage(marker.sample.getWidth(), marker.sample.getHeight(), PConstants.ARGB);
			marker.sample.getRGB(0, 0, image.width, image.height, image.pixels, 0, image.width);
			image.updatePixels();
		}
	}
	
	/**
	 * Type - ImageLayerList
	 * 
	 * @author hari 28 Nov 2011
	 * 
	 * 
	 */
	private class ImageLayerList extends PriorityQueue<ImageLayer> {
		/**
		 * 
		 */
		public ImageLayerList() {
			super();
		}
	}
	
	protected Directory directory = null;
	protected BaseImageFactory parent = null;
	
	protected BaseImage baseImage = null;
	private float baseImageScale = 1f;
	// All the persons in this image
	ArrayList<Person> persons = null;
	
	private ImageLayerList images = null;
	private ArrayList<Marker> markers = null;
	
	private boolean anim = false;
	private float animfactor = 0f, animstep = 0.05f, zoomx = 480, zoomy = 270;
	public static final int SKIP = 0;
	public static final int CROSSFADE = 1;
	public static final int ADDBLEND = 2;
	public static final int ZOOMFADE = 3;
	private Graphics2D g2d;
	
	private Date baseDateTime = null, lastDateTime = null, currentTime = null;
	
	private Object currentImage = null, nextImage = null;
	
	private float currentT = 1f;
	
	private int binCount = 256;
	
	private Calendar cal = Calendar.getInstance();
	private PGraphics pg = null;
	
	/**
	 * 
	 */
	public LiveImage(PGraphics pg, Directory directory, String fname) {
		super();
		
		this.directory = directory;
		this.parent = directory.baseFactory;
		
		this.baseImage = new BaseImage(parent, fname);
		if (baseImage.LoadingFailed)
			this.baseImage = null;
		else {
			// OK do the stuff here!
			this.baseDateTime = BaseImage.getDateTime(this.baseImage);
			this.pg = pg;
			this.baseImageScale = getScaleFactor();
			this.currentImage = baseImage;
			
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
			Date mind = this.baseDateTime, maxd = this.baseDateTime;
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
						if (imgl.marker.sample != null) {
							if (imgl.dateTime.before(mind))
								mind = (Date) imgl.dateTime.clone();
							if (imgl.dateTime.after(maxd))
								maxd = (Date) imgl.dateTime.clone();
							images.add(imgl);
						}
					}
				}
			}
			
			this.baseDateTime = mind;
			this.lastDateTime = maxd;
			
			setCurrentT(1);
			
			System.out.println(mind + " to " + maxd);
		}
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
	
//	private void animEnd() {
//		anim = false;
//		current = nextimage;
//		
//		pg.tint(255, 255);
//		drawBase();
//		pg.image(images[current], 0, 0);
//	}
//	
//	private void animRun(int type) {
//		if (animfactor >= 1f)
//			endAnim();
//		
//		if (anim && nextimage != current) {
//			switch (type) {
//			case CROSSFADE:
//				tint(255, 255);
//				image(images[0], 0, 0);
//				tint(255, (1 - animfactor) * 255);
//				image(images[current], 0, 0);
//				tint(255, animfactor * 255);
//				image(images[nextimage], 0, 0);
//				break;
//			
//			case ADDBLEND:
//				float af = (animfactor <= 0.5f) ? map(animfactor, 0f, 0.5f, 0f, 1f) : map(animfactor - 0.5f, 0f, 0.5f, 1f, 0f);
//				g2d.setComposite(AlphaComposite.Src);
//				g2d.drawImage(images[0].getImage(), 0, 0, null);
//				g2d.setComposite(AlphaComposite.Src.derive(constrain(1 - animfactor, 0, 1)));
//				g2d.drawImage(images[current].getImage(), 0, 0, null);
//				g2d.setComposite(BlendComposite.Add.derive(constrain(af, 0, 1)));
//				g2d.drawImage(images[nextimage].getImage(), 0, 0, null);
//				g2d.setComposite(AlphaComposite.Src);
//				break;
//			
//			case ZOOMFADE:
//				tint(255, 255);
//				image(images[0], 0, 0);
//				
//				pushMatrix();
//				translate(zoomx, zoomy);
//				scale(1 + (float) Math.sin(animfactor * Math.PI / 2f));
//				translate(-zoomx, -zoomy);
//				tint(255, (1 - animfactor) * 255);
//				image(images[current], 0, 0);
//				popMatrix();
//				
//				tint(255, animfactor * 255);
//				image(images[nextimage], 0, 0);
//				break;
//			}
//		}
//		animfactor += animstep;
//	}
	
	private void animStart() {
		animfactor = 0f;
		anim = true;
	}
	
	public void draw() {
		if (markers == null || markers.isEmpty())
			return;
		
		pg.background(0);
		drawImage(baseImage);
		
		// TODO Update according to currentT value
		if (currentT > 0) {
			for (ImageLayer il : images) {
				if (il.dateTime.after(currentTime))
					break;
				
				drawImage(il);
			}
		}
	}
	
	private void drawImage(Object img) {
		pg.pushMatrix();
		pg.scale(baseImageScale);
		
		if (img.getClass().equals(BaseImage.class))
			pg.image((PImage) img, 0, 0);
		else {
			ImageLayer il = (ImageLayer) img;
			Marker m = getmarker(il.marker.name);
			if (m == null)
				return;
			
			float sf = m.fullrect.width / (float) il.marker.fullrect.width;
			Vec2D tmpm = il.marker.center.sub(il.marker.fullrect.getTopLeft()).scale(sf);
			pg.pushMatrix();
			pg.translate(m.center.x - tmpm.x, m.center.y - tmpm.y);
			pg.scale(sf);
			pg.image(il.image, 0, 0);
			pg.popMatrix();
		}
		
		pg.popMatrix();
	}
	
	// Retrieves a histogram for the image.
	private int[][] getHistogram(BufferedImage img, int binCount) {
		// Get the band count.
		int numBands = img.getSampleModel().getNumBands();
		
		// Allocate histogram memory.
		int[] numBins = new int[numBands];
		double[] lowValue = new double[numBands];
		double[] highValue = new double[numBands];
		for (int i = 0; i < numBands; i++) {
			numBins[i] = binCount;
			lowValue[i] = 0.0;
			highValue[i] = 255.0;
		}
		
		// Create the Histogram object.
		Histogram hist = new Histogram(numBins, lowValue, highValue);
		
		// Set the ROI to the entire image.
		ROIShape roi = new ROIShape(new Rectangle(0, 0, img.getWidth(), img.getHeight()));
		
		// Create the histogram op.
		RenderedOp histImage = JAI.create("histogram", img, hist, roi, new Integer(1), new Integer(1));
		
		// Retrieve the histogram.
		hist = (Histogram) histImage.getProperty("histogram");
		
		// get histogram contents
		int[][] local_array = new int[numBands][];
		for (int j = 0; j < numBands; j++) {
			local_array[j] = new int[hist.getNumBins(j)];
			for (int i = 0; i < hist.getNumBins(j); i++) {
				local_array[j][i] = hist.getBinSize(j, i);
			}
		}
		
		return local_array;
	}
	
	private Marker getmarker(String name) {
		for (Marker m : markers)
			if (m.name.equalsIgnoreCase(name))
				return m;
		return null;
	}
	
	// Retrieves a histogram for the image.
	private RenderedOp getRenderedOp(BufferedImage img) {
		// Get the band count.
		int numBands = img.getSampleModel().getNumBands();
		
		// Allocate histogram memory.
		int[] numBins = new int[numBands];
		double[] lowValue = new double[numBands];
		double[] highValue = new double[numBands];
		for (int i = 0; i < numBands; i++) {
			numBins[i] = binCount;
			lowValue[i] = 0.0;
			highValue[i] = 255.0;
		}
		
		// Create the Histogram object.
		Histogram hist = new Histogram(numBins, lowValue, highValue);
		
		// Set the ROI to the entire image.
		ROIShape roi = new ROIShape(new Rectangle(0, 0, img.getWidth(), img.getHeight()));
		
		// Create the histogram op.
		RenderedOp histImage = JAI.create("histogram", img, hist, roi, new Integer(1), new Integer(1));
		
		return histImage;
	}
	
	private float getScaleFactor() {
		if (this.baseImage.width > this.baseImage.height)
			return pg.width / (float) this.baseImage.width;
		else
			return pg.height / (float) this.baseImage.height;
	}
	
	Long map(Float val, long x, long y) {
		return Math.round((double) (x + (y - x) * val));
	}
	
	private BufferedImage matchHistogram(BufferedImage src, BufferedImage dst, int binCount) {
		int numBands = dst.getSampleModel().getNumBands();
		
		int[][] hist = getHistogram(src, 256);
		
		// TODO match histograms
		float[][] CDFeq = new float[numBands][];
		for (int b = 0; b < numBands; b++) {
			CDFeq[b] = new float[binCount];
			for (int i = 0; i < binCount; i++) {
				CDFeq[b][i] = (float) (i + 1) / (float) binCount;
			}
		}
		
		RenderedOp img = getRenderedOp(dst);
		
		// Create a histogram-equalized image.
		RenderedOp eq = JAI.create("matchcdf", img, CDFeq);
		
		return eq.getAsBufferedImage();
	}
	
	/**
	 * @return the currentT
	 */
	public float getCurrentT() {
		return currentT;
	}
	
	/**
	 * @param currentT
	 *            the currentT to set
	 */
	public void setCurrentT(float ct) {
		this.currentT = PApplet.constrain(ct, 0, 1);
		currentTime = new Date(map(currentT, baseDateTime.getTime(), lastDateTime.getTime()));
		
		nextImage = null;
		// TODO Also set the current and next image!
		if (currentT > 0) {
			for (ImageLayer il : images) {
				if (il.dateTime.after(currentTime))
					break;
				
				nextImage = il.image;
			}
		}
	}
	
}
