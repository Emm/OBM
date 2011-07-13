package org.obm.sync.book;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.FactoryConfigurationError;

import org.obm.sync.items.AbstractItemsParser;
import org.obm.sync.items.AddressBookChangesResponse;
import org.obm.sync.items.ContactChanges;
import org.obm.sync.items.ContactChangesResponse;
import org.obm.sync.items.FolderChanges;
import org.obm.sync.items.FolderChangesResponse;
import org.obm.sync.utils.DOMUtils;
import org.obm.sync.utils.DateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class BookItemsParser extends AbstractItemsParser {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory
			.getLogger(BookItemsParser.class);

	public AddressBook parseAddressBook(Element root) {
		AddressBook book = new AddressBook();
		book.setUid(Integer.valueOf(root.getAttribute("uid")));
		book.setName(root.getAttribute("name"));
		return book;
	}
	
	public Contact parseContact(Element root) {

		Contact c = new Contact();

		if (root.hasAttribute("uid")) {
			c.setUid(Integer.parseInt(root.getAttribute("uid")));
		}
		
		if ("true".equals(root.getAttribute("collected"))) {
			c.setCollected(true);
		}
		
		c.setCommonname(s(root, "commonname"));
		c.setFirstname(s(root, "first"));
		c.setLastname(s(root, "last"));
		c.setTitle(s(root, "title"));
		c.setService(s(root, "service"));
		c.setAka(s(root, "aka"));
		c.setCompany(s(root, "company"));
		c.setComment(s(root, "comment"));

		c.setMiddlename(s(root, "middlename"));
		c.setSuffix(s(root, "suffix"));
		c.setAssistant(s(root, "assistant"));
		c.setManager(s(root, "manager"));
		c.setSpouse(s(root, "spouse"));
		c.setFolderId(i(root, "addressbookid"));
		c.setCalUri(s(root, "caluri"));
		String bday = s(root, "birthday");
		if (bday != null && bday.length() > 0) {
			c.setBirthday(DateHelper.asDate(bday));
		}

		String anniv = s(root, "anniversary");
		if (anniv != null && anniv.length() > 0) {
			c.setAnniversary(DateHelper.asDate(anniv));
		}
		

		parsePhones(c, DOMUtils.getUniqueElement(root, "phones"));
		parseInstantMessagingIdentifiers(c, DOMUtils.getUniqueElement(root,
				"instantmessaging"));
		parseAddresses(c, DOMUtils.getUniqueElement(root, "addresses"));
		parseEmails(c, DOMUtils.getUniqueElement(root, "emails"));
		parseWebsites(c, DOMUtils.getUniqueElement(root, "websites"));

		return c;

	}

	private void parseWebsites(Contact c, Element uniqueElement) {
		if(uniqueElement == null){
			return;
		}
		
		String[] attrs = { "label", "url" };
		String[][] values = DOMUtils
				.getAttributes(uniqueElement, "site", attrs);
		for (String[] p : values) {
			c.addWebsite(new Website(p[0], p[1]));
		}

	}

	private void parseEmails(Contact c, Element uniqueElement) {
		if(uniqueElement == null){
			return;
		}
		
		String[] attrs = { "label", "value" };
		String[][] values = DOMUtils
				.getAttributes(uniqueElement, "mail", attrs);
		for (String[] p : values) {
			c.addEmail(p[0], new Email(p[1]));
		}
	}

	private void parseAddresses(Contact c, Element uniqueElement) {
		if(uniqueElement == null){
			return;
		}
		
		String[] attrs = { "label", "zip", "expressPostal", "town", "country",
				"state" };
		String[][] values = DOMUtils.getAttributes(uniqueElement, "address",
				attrs);
		String[] streets = DOMUtils.getTexts(uniqueElement, "address");
		int i = 0;
		for (String[] p : values) {
			c.addAddress(p[0], new Address(streets[i++], p[1], p[2], p[3],
					p[4], p[5]));
		}
	}

	private void parseInstantMessagingIdentifiers(Contact c,
			Element uniqueElement) {
		if(uniqueElement == null){
			return;
		}
		
		String[] attrs = { "label", "protocol", "address" };
		String[][] values = DOMUtils.getAttributes(uniqueElement, "im", attrs);
		for (String[] p : values) {
			c.addIMIdentifier(p[0], new InstantMessagingId(p[1], p[2]));
		}
	}

	private void parsePhones(Contact c, Element uniqueElement) {
		if(uniqueElement == null){
		  return;
		}
		String[] attrs = { "label", "number" };
		String[][] values = DOMUtils.getAttributes(uniqueElement, "phone",
				attrs);
		for (String[] p : values) {
			c.addPhone(p[0], new Phone(p[1]));
		}
	}

	public Contact parseContact(String parameter) throws SAXException,
			IOException, FactoryConfigurationError {
		Document doc = DOMUtils.parse(parameter);
		Element root = doc.getDocumentElement();
		return parseContact(root);
	}

	/**
	 * Parse the contact changes document returned by the getSync call
	 */
	public ContactChangesResponse parseChanges(Document doc) {
		ContactChangesResponse changes = new ContactChangesResponse();
		Element root = doc.getDocumentElement();
		Date lastSync = DateHelper.asDate(root.getAttribute("lastSync"));
		changes.setLastSync(lastSync);
		changes.setChanges(parseContactChanges(root));
		return changes;
	}

	private ContactChanges parseContactChanges(Element root) {
		ContactChanges contactChanges = new ContactChanges();
		
		Element removed = DOMUtils.getUniqueElement(root, "removed");
		Element updated = DOMUtils.getUniqueElement(root, "updated");

		NodeList rmed = removed.getElementsByTagName("contact");
		Set<Integer> removedIds = new HashSet<Integer>();
		for (int i = 0; i < rmed.getLength(); i++) {
			Element e = (Element) rmed.item(i);
			removedIds.add(Integer.parseInt(e.getAttribute("uid")));
		}
		contactChanges.setRemoved(removedIds);

		NodeList upd = updated.getElementsByTagName("contact");
		List<Contact> updatedEvents = new ArrayList<Contact>(
				upd.getLength() + 1);
		for (int i = 0; i < upd.getLength(); i++) {
			Element e = (Element) upd.item(i);
			Contact ev = parseContact(e);
			updatedEvents.add(ev);
		}
		contactChanges.setUpdated(updatedEvents);
		return contactChanges;
	}
	
	public List<AddressBook> parseListAddressBook(Document doc) {
		Element documentElement = doc.getDocumentElement();
		List<AddressBook> ret = new LinkedList<AddressBook>();
		NodeList booksNodeList = documentElement.getElementsByTagName("book");
		for (int i = 0; i < booksNodeList.getLength(); i++) {
			Element e = (Element) booksNodeList.item(i);
			AddressBook book = parseAddressBook(e);
			ret.add(book);
		}
		return ret;
	}
	
	public List<Contact> parseListContact(Element documentElement) {
		List<Contact> ret = new LinkedList<Contact>();
		NodeList contactNodeList = documentElement
				.getElementsByTagName("contact");
		for (int i = 0; i < contactNodeList.getLength(); i++) {
			Element e = (Element) contactNodeList.item(i);
			Contact contact = parseContact(e);
			ret.add(contact);
		}
		return ret;
	}

	public FolderChangesResponse parseFolderChangesResponse(Document doc) {
		FolderChangesResponse changes = new FolderChangesResponse();
		Element root = doc.getDocumentElement();
		Date lastSync = DateHelper.asDate(root.getAttribute("lastSync"));
		changes.setLastSync(lastSync);
		changes.setFolderChanges(parseFolderChanges(root));
		return changes;
	}

	private FolderChanges parseFolderChanges(Element root) {
		FolderChanges folderChanges = new FolderChanges();
		
		Element removed = DOMUtils.getUniqueElement(root, "removed");
		Element updated = DOMUtils.getUniqueElement(root, "updated");

		NodeList rmed = removed.getElementsByTagName("foler");
		Set<Integer> removedIds = new HashSet<Integer>();
		for (int i = 0; i < rmed.getLength(); i++) {
			Element e = (Element) rmed.item(i);
			removedIds.add(Integer.parseInt(e.getAttribute("uid")));
		}
		folderChanges.setRemoved(removedIds);

		NodeList upd = updated.getElementsByTagName("folder");
		List<Folder> updatedFolders = new ArrayList<Folder>(upd.getLength() + 1);
		for (int i = 0; i < upd.getLength(); i++) {
			Element e = (Element) upd.item(i);
			Folder f = parseFolder(e);
			updatedFolders.add(f);
		}
		folderChanges.setUpdated(updatedFolders);
		return folderChanges;
	}
	
	private Folder parseFolder(Element root) {
		Folder f = new Folder();

		if (root.hasAttribute("uid")) {
			f.setUid(Integer.parseInt(root.getAttribute("uid")));
		}
		f.setName(s(root, "name"));
		f.setOwnerDisplayName(DOMUtils.getElementTextInChildren(root, "ownerDisplayName"));
		return f;
	}

	public Folder parseFolder(String parameter) throws SAXException,
			IOException, FactoryConfigurationError {
		Document doc = DOMUtils.parse(parameter);
		Element root = doc.getDocumentElement();
		return parseFolder(root);
	}

	public AddressBookChangesResponse parseAddressBookChanges(Document doc) {
		AddressBookChangesResponse response = new AddressBookChangesResponse();
		Element root = doc.getDocumentElement();
		Date lastSync = DateHelper.asDate(root.getAttribute("lastSync"));
		response.setLastSync(lastSync);
		response.setBooksChanges(parseFolderChanges(DOMUtils.getUniqueElement(root, "addressbooks")));
		response.setContactChanges(parseContactChanges(DOMUtils.getUniqueElement(root, "contacts")));
		return response;
	}
	
}
