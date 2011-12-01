/**
 * 
 */
package livingimagesviewer;

import static com.googlecode.javacv.cpp.opencv_core.cvLoad;

import java.awt.geom.Path2D;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PShape;
import processing.core.PShapeSVG;
import toxi.geom.Vec2D;

import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_objdetect.CvHaarClassifierCascade;

/**
 * Type - BaseImageFactory
 * 
 * @author hari 22 Nov 2011
 * 
 * 
 */
public class BaseImageFactory {
	public PApplet parent;
	
	//	String basepath = "/Users/hari/Work/code/LivingImages_MetaMaker/src/data/";
	
	public CvHaarClassifierCascade upperbodyCascade1, upperbodyCascade2, upperbodyCascade3;
	public CvHaarClassifierCascade faceCascade1, faceCascade2, faceCascade3, faceCascade4, faceCascade5;
	public CvHaarClassifierCascade eyesCascade1, eyesCascade2, eyesCascade3, eyesCascade4;
	public CvHaarClassifierCascade lefteyeCascade1, righteyeCascade1, lefteyeCascade2, righteyeCascade2;
	public CvHaarClassifierCascade mouthCascade;
	public CvMemStorage[] storage = new CvMemStorage[15];
	
	public PShape masksvg, mask, center;
	public Path2D mask2;
	public Vec2D maskCenter;
	
	BaseImageFactory(PApplet parent, String basepath) {
		this.parent = parent;
		
		String upperbodyCascadePath1 = basepath + "haar/HS.xml";
		String upperbodyCascadePath2 = basepath + "haar/haarcascade_upperbody.xml";
		String upperbodyCascadePath3 = basepath + "haar/haarcascade_mcs_upperbody.xml";
		
		String faceCascadePath1 = basepath + "haar/haarcascade_frontalface_default.xml";
		String faceCascadePath2 = basepath + "haar/haarcascade_frontalface_alt.xml";
		String faceCascadePath3 = basepath + "haar/haarcascade_frontalface_alt2.xml";
		String faceCascadePath4 = basepath + "haar/haarcascade_frontalface_alt_tree.xml";
		String faceCascadePath5 = basepath + "haar/haarcascade_profileface.xml";
		
		String eyesCascadePath1 = basepath + "haar/haarcascade_eye_tree_eyeglasses.xml";
		String eyesCascadePath2 = basepath + "haar/haarcascade_eye.xml";
		String eyesCascadePath3 = basepath + "haar/haarcascade_mcs_eyepair_big.xml";
		String eyesCascadePath4 = basepath + "haar/haarcascade_mcs_eyepair_small.xml";
		
		String lefteyeCascadePath1 = basepath + "haar/haarcascade_mcs_lefteye.xml";
		String lefteyeCascadePath2 = basepath + "haar/haarcascade_lefteye_2splits.xml";
		String righteyeCascadePath1 = basepath + "haar/haarcascade_mcs_righteye.xml";
		String righteyeCascadePath2 = basepath + "haar/haarcascade_righteye_2splits.xml";
		
		String mouthCascadePath1 = basepath + "haar/haarcascade_mcs_mouth.xml";
		
		upperbodyCascade1 = new CvHaarClassifierCascade(cvLoad(upperbodyCascadePath1));
		upperbodyCascade2 = new CvHaarClassifierCascade(cvLoad(upperbodyCascadePath2));
		upperbodyCascade3 = new CvHaarClassifierCascade(cvLoad(upperbodyCascadePath3));
		
		faceCascade1 = new CvHaarClassifierCascade(cvLoad(faceCascadePath1));
		faceCascade2 = new CvHaarClassifierCascade(cvLoad(faceCascadePath2));
		faceCascade3 = new CvHaarClassifierCascade(cvLoad(faceCascadePath3));
		faceCascade4 = new CvHaarClassifierCascade(cvLoad(faceCascadePath4));
		faceCascade5 = new CvHaarClassifierCascade(cvLoad(faceCascadePath5));
		
		eyesCascade1 = new CvHaarClassifierCascade(cvLoad(eyesCascadePath1));
		eyesCascade2 = new CvHaarClassifierCascade(cvLoad(eyesCascadePath2));
		eyesCascade3 = new CvHaarClassifierCascade(cvLoad(eyesCascadePath3));
		eyesCascade4 = new CvHaarClassifierCascade(cvLoad(eyesCascadePath4));
		
		lefteyeCascade1 = new CvHaarClassifierCascade(cvLoad(lefteyeCascadePath1));
		righteyeCascade1 = new CvHaarClassifierCascade(cvLoad(righteyeCascadePath1));
		lefteyeCascade2 = new CvHaarClassifierCascade(cvLoad(lefteyeCascadePath2));
		righteyeCascade2 = new CvHaarClassifierCascade(cvLoad(righteyeCascadePath2));
		
		mouthCascade = new CvHaarClassifierCascade(cvLoad(mouthCascadePath1));
		
		for (int i = 0; i < storage.length; i++) {
			storage[i] = CvMemStorage.create();
		}
		
		// masks
		masksvg = parent.loadShape(basepath + "UpperbodyMask2.svg");
		mask = masksvg.findChild("mask");
		center = masksvg.findChild("center");
		maskCenter = new Vec2D(center.getVertexX(0), center.getVertexY(0));
		
		mask2 = createFromPShape(mask);
	}
	
	public BaseImage createBaseImage(PImage img, String fnme) {
		BaseImage bimg = new BaseImage(this,img, fnme);
		return bimg;
	}
	
	public BaseImage createBaseImage(String fnme) {
		BaseImage bimg = new BaseImage(this, fnme);
		return bimg;
	}
	
	public static Path2D createFromPShape(PShape c) {
		if (c.getFamily() != PShapeSVG.PATH)
			return null;
		
		Path2D.Float tmpp = new Path2D.Float(Path2D.Float.WIND_EVEN_ODD);
		int vertexCodeCount = c.getVertexCodeCount();
		int vertexCount = c.getVertexCount();
		int vertexCode;
		float[] vert, vert1, vert2;
		boolean startNew = true;
		
		// Code from PShape class
		if (vertexCodeCount == 0) { // each point is a simple vertex
			for (int j = 0; j < vertexCount; j++) {
				vert = c.getVertex(j);
				if (startNew) {
					tmpp.moveTo(vert[PConstants.X], vert[PConstants.Y]);
					startNew = false;
				} else {
					tmpp.lineTo(vert[PConstants.X], vert[PConstants.Y]);
				}
			}
		} else { // coded set of vertices
			int index = 0;
			
			for (int j = 0; j < vertexCodeCount; j++) {
				vertexCode = c.getVertexCode(j);
				switch (vertexCode) {
				
				case PShape.VERTEX:
					vert = c.getVertex(index);
					
					if (startNew) {
						tmpp.moveTo(vert[PConstants.X], vert[PConstants.Y]);
						startNew = false;
					} else {
						tmpp.lineTo(vert[PConstants.X], vert[PConstants.Y]);
					}
					
					index++;
					break;
				
				case PShape.BEZIER_VERTEX:
					vert = c.getVertex(index);
					vert1 = c.getVertex(index + 1);
					vert2 = c.getVertex(index + 2);
					
					if (startNew) {
						tmpp.moveTo(vert[PConstants.X], vert[PConstants.Y]);
						startNew = false;
					}
					tmpp.curveTo(vert[PConstants.X], vert[PConstants.Y], vert1[PConstants.X], vert1[PConstants.Y], vert2[PConstants.X], vert2[PConstants.Y]);
					
					index += 3;
					break;
				
				case PShape.BREAK:
					tmpp.closePath();
					startNew = true;
				}
			}
		}
		return tmpp;
	}
}
