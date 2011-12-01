package livingimagesviewer;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;

import processing.core.PApplet;
import processing.core.PFont;

public class LivingImagesViewer extends PApplet {
	private static final long serialVersionUID = 4112841635641141583L;
	
	Directory directory;
	BaseImageFactory bif;
	
	LiveImage liveimage;
	
	public void setup() {
		size(800, 600, JAVA2D);
//		smooth();
		
//		println(PFont.list());
		textFont(createFont("HelveticaNeue-Bold", 16));
		
		background(0);
		print("Loading...");
		bif = new BaseImageFactory(this, "/Users/hari/Work/code/LivingImages_MetaMaker/src/data/");
		directory = new Directory(bif, "/Users/hari/Work/code/LivingImages_MetaMaker/src/data/directory.xml");
		directory.scan("/Users/hari/Work/CIID/Final/Prototype/MetaMaker/Selected");
		println("done!");
		
		//		directory.save("/Users/hari/Work/code/LivingImages_MetaMaker/src/data/directory.xml");
		liveimage = new LiveImage(g, directory, directory.get("john").baseimages.get(directory.get("john").baseimages.size() - 1));
	}
	
	public void draw() {
		liveimage.draw();
		
		if (frameCount % 100 == 0) {
			println(frameRate);
		}
		
		text("This is how the font will look like!", 100,100);
	}
	
	public static void main(String _args[]) {
		PApplet.main(new String[] { livingimagesviewer.LivingImagesViewer.class.getName() });
	}
}
