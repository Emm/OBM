/*
 * Created on May 25, 2004
 *
 */
package org.obm.sync.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Utility methods to extract data from a DOM.
 * 
 * @author Thomas Cataldo
 * 
 */
public final class DOMUtils {

	private static final Logger logger = LoggerFactory.getLogger(DOMUtils.class);

	private static TransformerFactory fac;
	private static DocumentBuilderFactory dbf;
	private static ThreadLocal<DocumentBuilder> builder = new ThreadLocal<DocumentBuilder>();

	static {
		fac = TransformerFactory.newInstance();
		dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setValidating(false);
	}

	private static DocumentBuilder builder() {
		DocumentBuilder ret = builder.get();
		if (ret == null) {
			try {
				ret = dbf.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				logger.error(e.getMessage(), e);
			}
			builder.set(ret);
		}
		return ret;
	}

	public static String getElementTextInChildren(Element root,
			String elementName) {
		NodeList list = root.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i).getNodeType() == Node.TEXT_NODE) {
				continue;
			}
			Element e = (Element) list.item(i);
			if (e.getTagName().equals(elementName)) {
				return getElementText((Element) list.item(i));
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("No element named '" + elementName + "' under '" //$NON-NLS-1$ //$NON-NLS-2$
					+ root.getNodeName() + "'"); //$NON-NLS-1$
		}
		return null;
	}

	/**
	 * Renvoie une élément qui doit être unique dans le document.
	 * 
	 * @param root
	 * @param elementName
	 * @return
	 */
	public static Element getUniqueElementInChildren(Element root,
			String elementName) {

		NodeList list = root.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i).getNodeType() == Node.TEXT_NODE) {
				continue;
			}
			Element e = (Element) list.item(i);
			if (e.getTagName().equals(elementName)) {
				return (Element) list.item(i);
			}
		}
		return null;
	}

	public static String getElementText(Element root, String elementName) {
		NodeList list = root.getElementsByTagName(elementName);
		if (list.getLength() == 0) {
			if (logger.isDebugEnabled()) {
				logger.debug("No element named '" + elementName + "' under '" //$NON-NLS-1$ //$NON-NLS-2$
						+ root.getNodeName() + "'"); //$NON-NLS-1$
			}
			return null;
		}
		return getElementText((Element) list.item(0));
	}

	public static String getElementText(Element node) {
		Text txtElem = (Text) node.getFirstChild();
		if (txtElem == null) {
			return null;
		}
		return txtElem.getData();
	}

	public static String[] getTexts(Element root, String elementName) {
		NodeList list = root.getElementsByTagName(elementName);
		String[] ret = new String[list.getLength()];
		for (int i = 0; i < list.getLength(); i++) {
			Text txt = (Text) list.item(i).getFirstChild();
			if (txt != null) {
				ret[i] = txt.getData();
			} else {
				ret[i] = ""; //$NON-NLS-1$
			}
		}
		return ret;
	}

	/**
	 * Renvoie sous la forme d'un tableau la valeur des attributs donnés pour
	 * toutes les occurences d'un élément donnée dans le dom
	 * 
	 * <code>
	 *  <toto>
	 *   <titi id="a" val="ba"/>
	 *   <titi id="b" val="bb"/>
	 *  </toto>
	 * </code>
	 * 
	 * et getAttributes(&lt;toto&gt;, "titi", { "id", "val" }) renvoie { { "a",
	 * "ba" } { "b", "bb" } }
	 * 
	 * @param root
	 * @param elementName
	 * @param wantedAttributes
	 * @return
	 */
	public static String[][] getAttributes(Element root, String elementName,
			String[] wantedAttributes) {
		NodeList list = root.getElementsByTagName(elementName);
		String[][] ret = new String[list.getLength()][wantedAttributes.length];
		for (int i = 0; i < list.getLength(); i++) {
			Element elem = (Element) list.item(i);
			for (int j = 0; j < wantedAttributes.length; j++) {
				ret[i][j] = elem.getAttribute(wantedAttributes[j]);
			}
		}
		return ret;
	}

	/**
	 * Renvoie la valeur de l'attribut donné, d'un élément donné qui doit être
	 * unique sous l'élément racine
	 * 
	 * @param root
	 * @param elementName
	 * @param attribute
	 * @return
	 */
	public static String getElementAttribute(Element root, String elementName,
			String attribute) {
		NodeList list = root.getElementsByTagName(elementName);
		if (list.getLength() == 0) {
			return null;
		}
		return ((Element) list.item(0)).getAttribute(attribute);
	}

	/**
	 * Renvoie une élément qui doit être unique dans le document.
	 * 
	 * @param root
	 * @param elementName
	 * @return
	 */
	public static Element getUniqueElement(Element root, String elementName) {
		NodeList list = root.getElementsByTagName(elementName);
		return (Element) list.item(0);
	}

	public static Element findElementWithUniqueAttribute(Element root,
			String elementName, String attribute, String attributeValue) {
		NodeList list = root.getElementsByTagName(elementName);
		for (int i = 0; i < list.getLength(); i++) {
			Element tmp = (Element) list.item(i);
			if (tmp.getAttribute(attribute).equals(attributeValue)) {
				return tmp;
			}
		}
		return null;
	}

	/**
	 * This method ensures that the output String has only valid XML unicode
	 * characters as specified by the XML 1.0 standard. For reference, please
	 * see <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
	 * standard</a>. This method will return an empty String if the input is
	 * null or empty.
	 * 
	 * @param in
	 *            The String whose non-valid characters we want to remove.
	 * @return The in String, stripped of non-valid characters.
	 */
	private static String stripNonValidXMLCharacters(String in) {
		StringBuffer out = new StringBuffer(in.length()); // Used to hold the
		// output.
		char current;

		for (int i = 0; i < in.length(); i++) {
			current = in.charAt(i);
			if ((current == 0x9) || (current == 0xA) || (current == 0xD)
					|| ((current >= 0x20) && (current <= 0xD7FF))
					|| ((current >= 0xE000) && (current <= 0xFFFD))
					|| ((current >= 0x10000) && (current <= 0x10FFFF)))
				out.append(current);
		}
		return out.toString();
	}

	public static Element createElementAndText(Element parent,
			String elementName, String text) {
		if (text == null) {
			throw new NullPointerException("null text");
		}
		Element el = parent.getOwnerDocument().createElement(elementName);
		parent.appendChild(el);
		Text txt = el.getOwnerDocument().createTextNode(
				stripNonValidXMLCharacters(text));
		el.appendChild(txt);
		return el;
	}

	public static Element createElement(Element parent, String elementName) {
		Element el = parent.getOwnerDocument().createElement(elementName);
		parent.appendChild(el);
		return el;
	}

	private static void serialize(Document doc, OutputStream out, boolean pretty)
			throws TransformerException {
		Transformer tf = fac.newTransformer();
		if (pretty) {
			tf.setOutputProperty(OutputKeys.INDENT, "yes");
		} else {
			tf.setOutputProperty(OutputKeys.INDENT, "no");
		}
		Source input = new DOMSource(doc.getDocumentElement());
		Result output = new StreamResult(out);
		tf.transform(input, output);
	}

	public static void serialize(Document doc, OutputStream out)
			throws TransformerException {
		serialize(doc, out, false);
	}

	public static void logDom(Document doc) throws TransformerException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		serialize(doc, out, true);
		logger.info(out.toString());
	}

	public static Document parse(InputStream is) throws SAXException,
			IOException, FactoryConfigurationError {
		return builder().parse(is);
	}

	public static Document parse(String input) throws SAXException,
		IOException, FactoryConfigurationError {
		return builder().parse(new InputSource(new StringReader(input)));
	}

	
	public static Document createDoc(String namespace, String rootElement)
			throws FactoryConfigurationError {
		DOMImplementation di = builder().getDOMImplementation();
		Document ret = null;
		ret = di.createDocument(namespace, rootElement, null);
		return ret;
	}
}
