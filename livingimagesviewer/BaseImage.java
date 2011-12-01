/**
 * 
 */
package livingimagesviewer;

import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
import static com.googlecode.javacv.cpp.opencv_core.cvROIToRect;
import static com.googlecode.javacv.cpp.opencv_core.cvRect;
import static com.googlecode.javacv.cpp.opencv_core.cvSetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import static com.googlecode.javacv.cpp.opencv_objdetect.cvHaarDetectObjects;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.PixelGrabber;
import java.awt.image.RGBImageFilter;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.googlecode.javacv.cpp.opencv_imgproc;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import processing.core.PGraphicsJava2D;
import processing.core.PImage;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import toxi.geom.Rect;
import toxi.geom.Triangle2D;
import toxi.geom.Vec2D;

/**
 * Type - BaseImage
 * 
 * @author hari 18 Nov 2011
 * 
 * 
 */
public class BaseImage extends PImage {
	BaseImageFactory parent;
	
	public String name = "", basefilename = "";
	private String filename = "", basepath = "";
	
	public ArrayList<Marker> markers;
	
	public boolean LoadingFailed = false;
	
	static Pattern pattern = Pattern.compile("(\\d{4}):(\\d{2}):(\\d{2})\\s(\\d{2}):(\\d{2}):(\\d{2})");
	
	public BaseImage(BaseImageFactory p, PImage img, String fnme) {
		super(img.width, img.height, img.format);
		super.copy(img, 0, 0, img.width, img.height, 0, 0, img.width, img.height);
		
		this.parent = p;
		
		//		this.filename = fnme;
		setFilename(fnme);
		
		String[] tmps = fnme.split("/");
		basepath = "";
		for (int i = 0; i < tmps.length - 1; i++)
			basepath = basepath + "/" + tmps[i];
		basepath = basepath + "/";
		tmps = tmps[tmps.length - 1].split("\\.");
		this.name = tmps[0];
		
		markers = new ArrayList<Marker>();
		
		findPeopleInImage();
		LoadingFailed = false;
	}
	
	public BaseImage(BaseImageFactory p, String fname) {
		super();
		
		this.parent = p;
		load(fname);
	}
	
	private void findPeopleInImage() {
		Rect baserect = new Rect(0, 0, this.width, this.height);
		// Prepare the image
		BufferedImage bimg = toBufferedImage(this);
		
		IplImage frame = IplImage.createFrom(bimg);
		IplImage frame_gray = cvCreateImage(cvSize(frame.width(), frame.height()), frame.depth(), 1);
		opencv_imgproc.cvCvtColor(frame, frame_gray, CV_RGB2GRAY);
		opencv_imgproc.cvEqualizeHist(frame_gray, frame_gray);
		
		//		((PGraphicsJava2D) parent.g).g2.drawImage(frame_gray.getBufferedImage(), 0, 0, parent.width, parent.height, 0, 0, frame_gray.width(), frame_gray.height(), null);
		
		//Find the upperbodies
		CvSeq[] ub = new CvSeq[3];
		ub[0] = cvHaarDetectObjects(frame_gray, parent.upperbodyCascade1, parent.storage[0], 1.15, 5, CV_HAAR_DO_CANNY_PRUNING);
		ub[1] = cvHaarDetectObjects(frame_gray, parent.upperbodyCascade2, parent.storage[1], 1.15, 5, CV_HAAR_DO_CANNY_PRUNING);
		ub[2] = cvHaarDetectObjects(frame_gray, parent.upperbodyCascade3, parent.storage[2], 1.15, 5, CV_HAAR_DO_CANNY_PRUNING);
		
		CvSeq UB = ub[0];
		for (int i = 1; i < ub.length; i++)
			if (ub[i].total() > UB.total())
				UB = ub[i];
		
		for (int i = 0; i < UB.total(); i++) {
			CvRect hsr = new CvRect(cvGetSeqElem(UB, i));
			cvSetImageROI(frame_gray, hsr);
			
			Rect hsrt = new Rect(hsr.x(), hsr.y(), hsr.width(), hsr.height());
			if (!baserect.containsPoint(hsrt.getTopLeft()) || !baserect.containsPoint(hsrt.getBottomRight()))
				continue;
			
			// Create the marker
			Marker mrk = new Marker();
			this.markers.add(mrk);
			float newwid = (parent.mask.width * hsr.width() / 206.5f);
			//			mrk.fullrect = new Rect(hsr.x() , hsr.y(), hsr.width(), hsr.height());
			mrk.fullrect = new Rect(hsr.x() - (newwid - hsr.width()) * 0.5f, hsr.y(), parent.mask.width * hsr.width() / 206.5f, parent.mask.height
					* hsr.width() / 206.5f);
			
			//-- Detect faces
			CvSeq[] fc = new CvSeq[5];
			fc[0] = cvHaarDetectObjects(frame_gray, parent.faceCascade1, parent.storage[3], 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
			fc[1] = cvHaarDetectObjects(frame_gray, parent.faceCascade2, parent.storage[4], 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
			fc[2] = cvHaarDetectObjects(frame_gray, parent.faceCascade3, parent.storage[5], 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
			fc[3] = cvHaarDetectObjects(frame_gray, parent.faceCascade4, parent.storage[6], 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
			fc[4] = cvHaarDetectObjects(frame_gray, parent.faceCascade5, parent.storage[7], 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
			
			CvSeq FC = fc[0];
			for (int j = 1; j < fc.length; j++)
				if (fc[j].total() > FC.total())
					FC = fc[j];
			
			for (int j = 0; j < FC.total(); j++) {
				CvRect r = new CvRect(cvGetSeqElem(FC, j));
				r.x(r.x() + hsr.x());
				r.y(r.y() + hsr.y());
				Vec2D facecenter = new Vec2D(r.x() + r.width() * 0.5f, r.y() + r.height() * 0.5f);
				
				// the mask
				PGraphicsJava2D pg = (PGraphicsJava2D) parent.parent.createGraphics(this.width, this.height, JAVA2D);
				pg.beginDraw();
				pg.smooth();
				pg.background(0);
				pg.pushMatrix();
				Vec2D tmpm = parent.maskCenter.scale(hsr.width() / 206.5f);
				pg.translate(facecenter.x - tmpm.x, facecenter.y - tmpm.y);
				pg.scale(hsr.width() / 206.5f);
				pg.shape(parent.mask);
				pg.popMatrix();
				pg.endDraw();
				
				// DEBUG Path2D mask
				mrk.mask2d = (Path2D) parent.mask2.clone();
				AffineTransform aff = new AffineTransform();
				aff.translate(facecenter.x - tmpm.x, facecenter.y - tmpm.y);
				aff.scale(hsr.width() / 206.5f, hsr.width() / 206.5f);
				mrk.mask2d.transform(aff);
				
				//DONE cut this imag out and save as sample
				Image img = TransformGrayToTransparency((BufferedImage) pg.image);
				BufferedImage img2 = ApplyTransparency(frame.getBufferedImage(), img);
				
				Rect rrx = mrk.fullrect.copy();
				Rect imgr = new Rect(0, 0, img2.getWidth(), img2.getHeight());
				rrx = rrx.intersectionRectWith(imgr);
				mrk.sample = img2.getSubimage(Math.round(rrx.x), Math.round(rrx.y), Math.round(rrx.width), Math.round(rrx.height));
				
				// DEBUG modified rectangle to fit within the image
				mrk.fullrect = rrx;
				
				// Update the marker
				mrk.mask = toBufferedImage(img);
				mrk.center = facecenter.copy();
				mrk.person = true;
				mrk.facerect = new Rect(r.x(), r.y(), r.width(), r.height());
				
				cvSetImageROI(frame_gray, cvRect(r.x(), r.y() + Math.round(r.height() / 5.5f), r.width(), Math.round(r.height() / 3f)));
				
				//-- In each face, detect eyes
				CvSeq[] ey = new CvSeq[6];
				Vec2D[] eyes = new Vec2D[2];
				boolean findeyes = true;
				ey[0] = cvHaarDetectObjects(frame_gray, parent.eyesCascade1, parent.storage[8], 1.1, 3, 0);
				if (ey[0].total() < 2) {
					ey[1] = cvHaarDetectObjects(frame_gray, parent.eyesCascade2, parent.storage[9], 1.1, 3, 0);
					if (ey[1].total() < 2) {
						// id left and right eye separately
						// Left eye
						cvSetImageROI(frame_gray,
								cvRect(r.x(), r.y() + Math.round(r.height() / 5.5f), Math.round(r.width() * 0.5f), Math.round(r.height() / 3f)));
						ey[2] = cvHaarDetectObjects(frame_gray, parent.lefteyeCascade1, parent.storage[10], 1.1, 3, 0);
						ey[3] = cvHaarDetectObjects(frame_gray, parent.lefteyeCascade2, parent.storage[11], 1.1, 3, 0);
						if (ey[2].total() < 1 && ey[3].total() < 1) {
							findeyes = false;
						} else {
							CvSeq EY;
							EY = (ey[2].total() < 1) ? ey[3] : ey[2];
							// Add the point
							CvRect er = cvROIToRect(frame_gray.roi());
							CvRect er1 = new CvRect(cvGetSeqElem(EY, 0));
							eyes[0] = new Vec2D(er.x() + er1.x() + er1.width() * 0.5f, er.y() + er1.y() + er1.height() * 0.5f);
							
							// Right eye
							cvSetImageROI(
									frame_gray,
									cvRect(r.x() + Math.round(r.width() * 0.5f), r.y() + Math.round(r.height() / 5.5f), Math.round(r.width() * 0.5f),
											Math.round(r.height() / 3f)));
							ey[4] = cvHaarDetectObjects(frame_gray, parent.righteyeCascade1, parent.storage[12], 1.1, 3, 0);
							ey[5] = cvHaarDetectObjects(frame_gray, parent.righteyeCascade2, parent.storage[13], 1.1, 3, 0);
							if (ey[4].total() < 1 && ey[5].total() < 1) {
								findeyes = false;
							} else {
								EY = (ey[4].total() < 1) ? ey[5] : ey[4];
								// Add the point
								er = cvROIToRect(frame_gray.roi());
								er1 = new CvRect(cvGetSeqElem(EY, 0));
								eyes[1] = new Vec2D(er.x() + er1.x() + er1.width() * 0.5f, er.y() + er1.y() + er1.height() * 0.5f);
							}
						}
					} else {
						CvRect er = cvROIToRect(frame_gray.roi());
						CvRect er1 = new CvRect(cvGetSeqElem(ey[1], 0));
						CvRect er2 = new CvRect(cvGetSeqElem(ey[1], 1));
						eyes[0] = new Vec2D(er.x() + er1.x() + er1.width() * 0.5f, er.y() + er1.y() + er1.height() * 0.5f);
						eyes[1] = new Vec2D(er.x() + er2.x() + er2.width() * 0.5f, er.y() + er2.y() + er2.height() * 0.5f);
					}
				} else {
					CvRect er = cvROIToRect(frame_gray.roi());
					CvRect er1 = new CvRect(cvGetSeqElem(ey[0], 0));
					CvRect er2 = new CvRect(cvGetSeqElem(ey[0], 1));
					eyes[0] = new Vec2D(er.x() + er1.x() + er1.width() * 0.5f, er.y() + er1.y() + er1.height() * 0.5f);
					eyes[1] = new Vec2D(er.x() + er2.x() + er2.width() * 0.5f, er.y() + er2.y() + er2.height() * 0.5f);
				}
				
				// Hope we could identify the eyes on the face
				if (findeyes) {
					// We got the eyes now try and find the mouth
					cvSetImageROI(frame_gray, cvRect(r.x(), r.y() + Math.round(r.height() / 1.5f), r.width(), Math.round(r.height() / 2.5f)));
					CvSeq mth = cvHaarDetectObjects(frame_gray, parent.mouthCascade, parent.storage[14], 1.15, 5, 0);
					CvRect mr = cvROIToRect(frame_gray.roi());
					
					if (mth.total() > 0) {
						// The face triangle, set up the three vertices at the center of each eye and mouth.
						CvRect r2 = new CvRect(cvGetSeqElem(mth, 0));
						mrk.faceTriangle = new Triangle2D(eyes[0], eyes[1],
								new Vec2D(mr.x() + r2.x() + r2.width() * 0.5f, mr.y() + r2.y() + r2.height() * 0.5f));
					} else {
						// Couldn't find the face triangle
						mrk.faceTriangle = null;
					}
				} else {
					// Couldn't find the face triangle
					mrk.faceTriangle = null;
				}
			}
			
		}
		
		for (int i = 0; i < parent.storage.length; i++)
			cvClearMemStorage(parent.storage[i]);
	}
	
	public void save(String fname) {
		String deppath = basepath + name;
		File f = new File(deppath);
		if (!f.exists() || !f.isDirectory()) {
			if (!f.mkdir())
				return;
		}
		deppath += "/";
		
		// DEBUG the basefilename is the xml file.. it's handled in the Directory class itself
		//		this.basefilename = fname;
		
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = dbfac.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			
			Element root = doc.createElement("baseimage");
			doc.appendChild(root);
			
			root.setAttribute("name", this.name);
			//			root.setAttribute("basename", this.basefilename);
			root.setAttribute("image", this.filename);
			
			int num = 1;
			for (Marker mrk : markers) {
				Element marker = doc.createElement("marker");
				root.appendChild(marker);
				
				marker.setAttribute("name", mrk.name);
				marker.setAttribute("uri_facebook", mrk.urlfacebook);
				marker.setAttribute("uri_flickr", mrk.urlflickr);
				marker.setAttribute("uri_twitter", mrk.urltwitter);
				marker.setAttribute("person", mrk.person.toString());
				
				Element mrktmp;
				String tmps;
				
				if (mrk.mask != null) {
					mrktmp = doc.createElement("mask");
					marker.appendChild(mrktmp);
					tmps = deppath + name + "_mrk" + num + "_mask.png";
					saveImage(mrk.mask, tmps);
					mrktmp.setAttribute("image", tmps);
				}
				
				if (mrk.sample != null) {
					mrktmp = doc.createElement("sample");
					marker.appendChild(mrktmp);
					tmps = deppath + name + "_mrk" + num + "_sample.png";
					saveImage(mrk.sample, tmps);
					mrktmp.setAttribute("image", tmps);
				}
				
				if (mrk.center != null) {
					mrktmp = doc.createElement("center");
					marker.appendChild(mrktmp);
					mrktmp.setAttribute("x", mrk.center.x + "");
					mrktmp.setAttribute("y", mrk.center.y + "");
				}
				
				if (mrk.faceTriangle != null) {
					mrktmp = doc.createElement("faceTriangle");
					marker.appendChild(mrktmp);
					mrktmp.setAttribute("a.x", mrk.faceTriangle.a.x + "");
					mrktmp.setAttribute("a.y", mrk.faceTriangle.a.y + "");
					mrktmp.setAttribute("b.x", mrk.faceTriangle.b.x + "");
					mrktmp.setAttribute("b.y", mrk.faceTriangle.b.y + "");
					mrktmp.setAttribute("c.x", mrk.faceTriangle.c.x + "");
					mrktmp.setAttribute("c.y", mrk.faceTriangle.c.y + "");
				}
				
				if (mrk.fullrect != null) {
					mrktmp = doc.createElement("fullRect");
					marker.appendChild(mrktmp);
					mrktmp.setAttribute("x", mrk.fullrect.x + "");
					mrktmp.setAttribute("y", mrk.fullrect.y + "");
					mrktmp.setAttribute("w", mrk.fullrect.width + "");
					mrktmp.setAttribute("h", mrk.fullrect.height + "");
				}
				
				if (mrk.facerect != null) {
					mrktmp = doc.createElement("faceRect");
					marker.appendChild(mrktmp);
					mrktmp.setAttribute("x", mrk.facerect.x + "");
					mrktmp.setAttribute("y", mrk.facerect.y + "");
					mrktmp.setAttribute("w", mrk.facerect.width + "");
					mrktmp.setAttribute("h", mrk.facerect.height + "");
				}
				
				num++;
			}
			
			//set up a transformer
			TransformerFactory transfac = TransformerFactory.newInstance();
			Transformer trans = transfac.newTransformer();
			trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			
			//create string from xml tree
			StringWriter sw = new StringWriter();
			StreamResult result = new StreamResult(sw);
			DOMSource source = new DOMSource(doc);
			trans.transform(source, result);
			String xmlString = sw.toString();
			
			FileOutputStream fos = new FileOutputStream(fname);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			bos.write(xmlString.getBytes());
			
			bos.close();
			fos.close();
			
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void load(String fname) {
		LoadingFailed = true;
		
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = dbfac.newDocumentBuilder();
			Document doc = docBuilder.parse(fname);
			doc.getDocumentElement().normalize();
			
			Node root = doc.getFirstChild();
			if (!root.getNodeName().equals("baseimage"))
				return;
			
			NamedNodeMap nnm = root.getAttributes();
			this.name = nnm.getNamedItem("name").getNodeValue();
			this.setFilename(nnm.getNamedItem("image").getNodeValue());
			
			PImage img = parent.parent.loadImage(getFilename());
			this.init(img.width, img.height, img.format);
			this.copy(img, 0, 0, img.width, img.height, 0, 0, img.width, img.height);
			
			if (this.markers == null)
				this.markers = new ArrayList<Marker>();
			this.markers.clear();
			
			NodeList listOfPersons = doc.getElementsByTagName("marker");
			for (int s = 0; s < listOfPersons.getLength(); s++) {
				Element mrkrElement = (Element) listOfPersons.item(s);
				
				Marker mrk = new Marker();
				markers.add(mrk);
				
				nnm = mrkrElement.getAttributes();
				mrk.name = nnm.getNamedItem("name").getNodeValue();
				mrk.person = Boolean.parseBoolean(nnm.getNamedItem("person").getNodeValue());
				mrk.urlfacebook = nnm.getNamedItem("uri_facebook").getNodeValue();
				mrk.urlflickr = nnm.getNamedItem("uri_flickr").getNodeValue();
				mrk.urltwitter = nnm.getNamedItem("uri_twitter").getNodeValue();
				
				// Mask
				Element sub = (Element) mrkrElement.getElementsByTagName("mask").item(0);
				if (sub != null) {
					nnm = sub.getAttributes();
					mrk.mask = loadImage(nnm.getNamedItem("image").getNodeValue());
				}
				
				// Sample
				sub = (Element) mrkrElement.getElementsByTagName("sample").item(0);
				if (sub != null) {
					nnm = sub.getAttributes();
					mrk.sample = loadImage(nnm.getNamedItem("image").getNodeValue());
				}
				
				// Center
				sub = (Element) mrkrElement.getElementsByTagName("center").item(0);
				if (sub != null) {
					nnm = sub.getAttributes();
					mrk.center = new Vec2D(Float.parseFloat(nnm.getNamedItem("x").getNodeValue()), Float.parseFloat(nnm.getNamedItem("y").getNodeValue()));
				}
				
				// Face triangle
				sub = (Element) mrkrElement.getElementsByTagName("faceTriangle").item(0);
				if (sub != null) {
					nnm = sub.getAttributes();
					mrk.faceTriangle = new Triangle2D();
					mrk.faceTriangle.a = new Vec2D(Float.parseFloat(nnm.getNamedItem("a.x").getNodeValue()), Float.parseFloat(nnm.getNamedItem("a.y")
							.getNodeValue()));
					mrk.faceTriangle.b = new Vec2D(Float.parseFloat(nnm.getNamedItem("b.x").getNodeValue()), Float.parseFloat(nnm.getNamedItem("b.y")
							.getNodeValue()));
					mrk.faceTriangle.c = new Vec2D(Float.parseFloat(nnm.getNamedItem("c.x").getNodeValue()), Float.parseFloat(nnm.getNamedItem("c.y")
							.getNodeValue()));
				}
				
				// Fullrect
				sub = (Element) mrkrElement.getElementsByTagName("fullRect").item(0);
				if (sub != null) {
					nnm = sub.getAttributes();
					mrk.fullrect = new Rect();
					mrk.fullrect.x = Float.parseFloat(nnm.getNamedItem("x").getNodeValue());
					mrk.fullrect.y = Float.parseFloat(nnm.getNamedItem("y").getNodeValue());
					mrk.fullrect.width = Float.parseFloat(nnm.getNamedItem("w").getNodeValue());
					mrk.fullrect.height = Float.parseFloat(nnm.getNamedItem("h").getNodeValue());
				}
				
				// facerect
				sub = (Element) mrkrElement.getElementsByTagName("faceRect").item(0);
				if (sub != null) {
					nnm = sub.getAttributes();
					mrk.facerect = new Rect();
					mrk.facerect.x = Float.parseFloat(nnm.getNamedItem("x").getNodeValue());
					mrk.facerect.y = Float.parseFloat(nnm.getNamedItem("y").getNodeValue());
					mrk.facerect.width = Float.parseFloat(nnm.getNamedItem("w").getNodeValue());
					mrk.facerect.height = Float.parseFloat(nnm.getNamedItem("h").getNodeValue());
				}
			}
			
			LoadingFailed = false;
			
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String ImageToBase64(Image image) {
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
		ImageWriter writer = (ImageWriter) writers.next();
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ImageOutputStream ios;
		try {
			ios = ImageIO.createImageOutputStream(bos);
			writer.setOutput(ios);
			
			writer.write(toBufferedImage(image));
			
			byte[] imageBytes = bos.toByteArray();
			
			BASE64Encoder encoder = new BASE64Encoder();
			String base64String = encoder.encode(imageBytes);
			return base64String;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public Image Base64ToImage(String base64) {
		BASE64Decoder decoder = new BASE64Decoder();
		byte[] imagebytes;
		try {
			imagebytes = decoder.decodeBuffer(base64);
			ByteArrayInputStream bis = new ByteArrayInputStream(imagebytes);
			ImageInputStream iis = ImageIO.createImageInputStream(bis);
			
			Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("png");
			ImageReader reader = (ImageReader) readers.next();
			reader.setInput(iis);
			
			return reader.read(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void saveImage(Image image, String fname) {
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
		ImageWriter writer = (ImageWriter) writers.next();
		
		ImageOutputStream ios;
		try {
			FileOutputStream fos = new FileOutputStream(fname);
			ios = ImageIO.createImageOutputStream(fos);
			writer.setOutput(ios);
			
			writer.write(toBufferedImage(image));
			
			ios.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private BufferedImage loadImage(String fname) {
		FileInputStream fis;
		try {
			fis = new FileInputStream(fname);
			ImageInputStream iis = ImageIO.createImageInputStream(fis);
			
			Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("png");
			ImageReader reader = (ImageReader) readers.next();
			reader.setInput(iis);
			
			return reader.read(0);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static BufferedImage toBufferedImage(PImage src) {
		BufferedImage bimg = new BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_ARGB);
		src.loadPixels();
		bimg.setRGB(0, 0, src.width, src.height, src.pixels, 0, src.width);
		return bimg;
	}
	
	public static BufferedImage toBufferedImage(Image image) {
		if (image instanceof BufferedImage) {
			return (BufferedImage) image;
		}
		
		// This code ensures that all the pixels in the image are loaded
		image = new ImageIcon(image).getImage();
		
		// Determine if the image has transparent pixels; for this method's
		// implementation, see Determining If an Image Has Transparent Pixels
		boolean hasAlpha = hasAlpha(image);
		
		// Create a buffered image with a format that's compatible with the screen
		BufferedImage bimage = null;
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		try {
			// Determine the type of transparency of the new buffered image
			int transparency = Transparency.OPAQUE;
			if (hasAlpha) {
				transparency = Transparency.BITMASK;
			}
			
			// Create the buffered image
			GraphicsDevice gs = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gs.getDefaultConfiguration();
			bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
		} catch (HeadlessException e) {
			// The system does not have a screen
		}
		
		if (bimage == null) {
			// Create a buffered image using the default color model
			int type = BufferedImage.TYPE_INT_RGB;
			if (hasAlpha) {
				type = BufferedImage.TYPE_INT_ARGB;
			}
			bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
		}
		
		// Copy image to buffered image
		Graphics g = bimage.createGraphics();
		
		// Paint the image onto the buffered image
		g.drawImage(image, 0, 0, null);
		g.dispose();
		
		return bimage;
	}
	
	// This method returns true if the specified image has transparent pixels
	public static boolean hasAlpha(Image image) {
		// If buffered image, the color model is readily available
		if (image instanceof BufferedImage) {
			BufferedImage bimage = (BufferedImage) image;
			return bimage.getColorModel().hasAlpha();
		}
		
		// Use a pixel grabber to retrieve the image's color model;
		// grabbing a single pixel is usually sufficient
		PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
		try {
			pg.grabPixels();
		} catch (InterruptedException e) {
		}
		
		// Get the image's color model
		ColorModel cm = pg.getColorModel();
		return cm.hasAlpha();
	}
	
	public static Image TransformGrayToTransparency(BufferedImage image) {
		ImageFilter filter = new RGBImageFilter() {
			public final int filterRGB(int x, int y, int rgb) {
				return (rgb << 8) & 0xFF000000;
			}
		};
		
		ImageProducer ip = new FilteredImageSource(image.getSource(), filter);
		return Toolkit.getDefaultToolkit().createImage(ip);
	}
	
	public static BufferedImage ApplyTransparency(BufferedImage image, Image mask) {
		BufferedImage dest = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = dest.createGraphics();
		g2.drawImage(image, 0, 0, null);
		AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.DST_IN, 1.0F);
		g2.setComposite(ac);
		g2.drawImage(mask, 0, 0, null);
		g2.dispose();
		return dest;
	}
	
	public String getFilename() {
		return filename;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
		
		String[] tmps = this.filename.split("/");
		basepath = "";
		for (int i = 0; i < tmps.length - 1; i++)
			basepath = basepath + tmps[i] + "/";
		
		tmps = this.filename.split("\\.");
		basefilename = "";
		for (int i = 1; i < tmps.length - 1; i++)
			basefilename = basefilename + tmps[i] + ".";
		basefilename = basefilename + "xml";
	}
	
	public static Date getDateTime(BaseImage bimg) {
		Date tmpd = null;
		
		if (bimg != null) {
			File file = new File(bimg.getFilename());
			
			IImageMetadata metadata;
			try {
				metadata = Sanselan.getMetadata(file);
				
				if (metadata instanceof JpegImageMetadata) {
					JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
					
					// Jpeg EXIF metadata is stored in a TIFF-based directory structure
					// and is identified with TIFF tags.
					// Here we look for the "x resolution" tag, but
					// we could just as easily search for any other tag.
					//
					// see the TiffConstants file for a list of TIFF tags.
					
					TiffField field = jpegMetadata.findEXIFValue(TiffConstants.EXIF_TAG_CREATE_DATE);
					if (field == null)
						tmpd = new Date(file.lastModified());
					else {
						int yr, mth, day, hr, min, sec;
						Matcher match = pattern.matcher(field.getStringValue());
						
						if (match.find()) {
							yr = Integer.parseInt(match.group(1));
							mth = Integer.parseInt(match.group(2)) - 1;
							mth = (mth < 0) ? 0 : mth;
							day = Integer.parseInt(match.group(3));
							hr = Integer.parseInt(match.group(4));
							min = Integer.parseInt(match.group(5));
							sec = Integer.parseInt(match.group(6));
							
							Calendar cal = Calendar.getInstance();
							cal.set(yr, mth, day, hr, min, sec);
							tmpd = cal.getTime();
							System.out.println(field.getStringValue() + " , " + tmpd);
						}
					}
				}
				
			} catch (ImageReadException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return tmpd;
	}
}
