/**
 * 
 */
package livingimagesviewer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Type - Directory
 * 
 * @author hari 21 Nov 2011
 * 
 * 
 */
public class Directory extends HashMap<String, Person> {
	private static final long serialVersionUID = -2496864395073225064L;
	
	BaseImageFactory baseFactory;
	
	Directory(BaseImageFactory base, String fname) {
		super();
		
		this.baseFactory = base;
		load(fname);
	}
	
	Directory(BaseImageFactory base) {
		super();
		this.baseFactory = base;
	}
	
	public Person add(Person p) {
		return this.put(p.name, p);
	}
	
	/**
	 * Directory - scan
	 * 
	 * @param fname
	 *            Should be a directory name.
	 * 
	 */
	public void scan(String fname) {
		if (fname.substring(fname.length() - 1).equals("/"))
			fname = fname.substring(0, fname.length() - 1);
		
		File f = new File(fname);
		if (!f.exists() || !f.isDirectory())
			return;
		
		FilenameFilter ffltr = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				String[] nme = name.split("\\.");
				if (nme[nme.length - 1].equalsIgnoreCase("xml"))
					return true;
				return false;
			}
		};
		String[] fl = f.list(ffltr);
		Person pers;
		
		for (int i = 0; i < fl.length; i++) {
			BaseImage bimg = baseFactory.createBaseImage(fname + "/" + fl[i]);
			
			if (bimg.LoadingFailed)
				continue;
			
			for (Marker mrk : bimg.markers) {
				if (mrk.person) {
					if (this.containsKey(mrk.name))
						pers = this.get(mrk.name);
					else {
						pers = new Person();
						this.put(mrk.name, pers);
						pers.name = mrk.name;
						if (!mrk.urlfacebook.equals(""))
							pers.urlfacebook = mrk.urlfacebook;
						if (!mrk.urlflickr.equals(""))
							pers.urlflickr = mrk.urlflickr;
						if (!mrk.urltwitter.equals(""))
							pers.urltwitter = mrk.urltwitter;
					}
					
					// Check if this baseImage already exists
					if (!pers.baseimages.contains(fname + "/" + fl[i]))
						pers.baseimages.add(fname + "/" + fl[i]);
				}
			}
		}
	}
	
	public void save(String fname) {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = dbfac.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			
			Element root = doc.createElement("BaseImageDirectory");
			doc.appendChild(root);
			
			for (Person mrk : this.values()) {
				Element marker = doc.createElement("person");
				root.appendChild(marker);
				
				marker.setAttribute("name", mrk.name);
				marker.setAttribute("uri_facebook", mrk.urlfacebook);
				marker.setAttribute("uri_flickr", mrk.urlflickr);
				marker.setAttribute("uri_twitter", mrk.urltwitter);
				
				Element basetmp, mrktmp;
				for (String base : mrk.baseimages) {
					basetmp = doc.createElement("baseimage");
					marker.appendChild(basetmp);
					basetmp.setAttribute("loc", base);
					
					BaseImage bmig = baseFactory.createBaseImage(base);
					
					for (Marker m : bmig.markers) {
						if (mrk.name.equals(m.name))
							continue;
						mrktmp = doc.createElement("person");
						basetmp.appendChild(mrktmp);
						mrktmp.setAttribute("with", m.name);
					}
				}
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
	
	private void load(String fname) {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = dbfac.newDocumentBuilder();
			Document doc = docBuilder.parse(fname);
			doc.getDocumentElement().normalize();
			
			Node root = doc.getFirstChild();
			if (!root.getNodeName().equals("BaseImageDirectory"))
				return;
			
			NamedNodeMap nnm, nnm2;
			
			NodeList listOfPersons = doc.getElementsByTagName("person");
			for (int s = 0; s < listOfPersons.getLength(); s++) {
				Element mrkrElement = (Element) listOfPersons.item(s);
				
				nnm = mrkrElement.getAttributes();
				if (nnm.getNamedItem("with") != null)
					continue;
				String nme = nnm.getNamedItem("name").getNodeValue();
				Person pers;
				
				if (this.containsKey(nme))
					pers = this.get(nme);
				else {
					pers = new Person();
					this.put(nme, pers);
					pers.name = nme;
					pers.urlfacebook = nnm.getNamedItem("uri_facebook").getNodeValue();
					pers.urlflickr = nnm.getNamedItem("uri_flickr").getNodeValue();
					pers.urltwitter = nnm.getNamedItem("uri_twitter").getNodeValue();
				}
				
				NodeList listOfBases = mrkrElement.getElementsByTagName("baseimage");
				for (int t = 0; t < listOfBases.getLength(); t++) {
					Element baseimg = (Element) listOfBases.item(t);
					nnm2 = baseimg.getAttributes();
					
					String bloc = nnm2.getNamedItem("loc").getNodeValue();
					if (!pers.baseimages.contains(bloc))
						pers.baseimages.add(bloc);
				}
			}
			
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
