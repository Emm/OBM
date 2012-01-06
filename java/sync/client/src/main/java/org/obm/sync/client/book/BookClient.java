/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2012  Linagora
 *
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version, provided you comply 
 * with the Additional Terms applicable for OBM connector by Linagora 
 * pursuant to Section 7 of the GNU Affero General Public License, 
 * subsections (b), (c), and (e), pursuant to which you must notably (i) retain 
 * the “Message sent thanks to OBM, Free Communication by Linagora” 
 * signature notice appended to any and all outbound messages 
 * (notably e-mail and meeting requests), (ii) retain all hypertext links between 
 * OBM and obm.org, as well as between Linagora and linagora.com, and (iii) refrain 
 * from infringing Linagora intellectual property rights over its trademarks 
 * and commercial brands. Other Additional Terms apply, 
 * see <http://www.linagora.com/licenses/> for more details. 
 *
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details. 
 *
 * You should have received a copy of the GNU Affero General Public License 
 * and its applicable Additional Terms for OBM along with this program. If not, 
 * see <http://www.gnu.org/licenses/> for the GNU Affero General Public License version 3 
 * and <http://www.linagora.com/licenses/> for the Additional Terms applicable to 
 * OBM connectors. 
 * 
 * ***** END LICENSE BLOCK ***** */
package org.obm.sync.client.book;

import java.util.Date;
import java.util.List;

import org.obm.sync.auth.AccessToken;
import org.obm.sync.auth.ContactNotFoundException;
import org.obm.sync.auth.ServerFault;
import org.obm.sync.base.KeyList;
import org.obm.sync.book.AddressBook;
import org.obm.sync.book.BookItemsParser;
import org.obm.sync.book.BookItemsWriter;
import org.obm.sync.book.BookType;
import org.obm.sync.book.Contact;
import org.obm.sync.client.impl.AbstractClientImpl;
import org.obm.sync.client.impl.SyncClientException;
import org.obm.sync.items.AddressBookChangesResponse;
import org.obm.sync.items.ContactChanges;
import org.obm.sync.items.FolderChanges;
import org.obm.sync.locators.Locator;
import org.obm.sync.services.IAddressBook;
import org.obm.sync.utils.DOMUtils;
import org.obm.sync.utils.DateHelper;
import org.w3c.dom.Document;

import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BookClient extends AbstractClientImpl implements IAddressBook {

	private final BookItemsParser respParser;
	private final BookItemsWriter biw;
	private final Locator Locator;

	@Inject
	private BookClient(SyncClientException syncClientException, Locator Locator) {
		super(syncClientException);
		this.Locator = Locator;
		this.respParser = new BookItemsParser();
		this.biw = new BookItemsWriter();
	}

	@Override
	public Contact createContact(AccessToken token, BookType book, Contact contact) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		params.put("book", book.toString());
		params.put("contact", biw.getContactAsString(contact));
		Document doc = execute(token, "/book/createContact", params);
		exceptionFactory.checkServerFaultException(doc);
		return respParser.parseContact(doc.getDocumentElement());
	}

	@Override
	public Contact createContactWithoutDuplicate(AccessToken token, BookType book, Contact contact) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		params.put("book", book.toString());
		params.put("contact", biw.getContactAsString(contact));
		Document doc = execute(token, "/book/createContactWithoutDuplicate", params);
		exceptionFactory.checkServerFaultException(doc);
		return respParser.parseContact(doc.getDocumentElement());
	}

	@Override
	public Contact getContactFromId(AccessToken token, BookType book, String id) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		params.put("book", book.toString());
		params.put("id", id);
		Document doc = execute(token, "/book/getContactFromId", params);
		exceptionFactory.checkServerFaultException(doc);
		return respParser.parseContact(doc.getDocumentElement());
	}

	@Override
	public KeyList getContactTwinKeys(AccessToken token, BookType book, Contact contact) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		params.put("book", book.toString());
		params.put("contact", biw.getContactAsString(contact));
		Document doc = execute(token, "/book/getContactTwinKeys", params);
		exceptionFactory.checkServerFaultException(doc);
		return respParser.parseKeyList(doc);
	}

	@Override
	public ContactChanges listContactsChanged(AccessToken token, Date lastSync) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		params.put("lastSync", DateHelper.asString(lastSync));
		Document doc = execute(token, "/book/listContactsChanged", params);
		exceptionFactory.checkServerFaultException(doc);
		return respParser.parseChanges(doc);
	}

	@Override
	public boolean isReadOnly(AccessToken token, BookType book) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		params.put("book", book.toString());
		Document doc = execute(token, "/book/isReadOnly", params);
		exceptionFactory.checkServerFaultException(doc);
		return "true".equals(respParser.parseArrayOfString(doc)[0]);
	}

	@Override
	public BookType[] listBooks(AccessToken token) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		Document doc = execute(token, "/book/listBooks", params);
		exceptionFactory.checkServerFaultException(doc);
		String[] sa = respParser.parseArrayOfString(doc);
		BookType[] bts = new BookType[sa.length];
		for (int i = 0; i < sa.length; i++) {
			bts[i] = BookType.valueOf(sa[i]);
		}
		return bts;
	}

	@Override
	public List<AddressBook> listAllBooks(AccessToken token) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		Document doc = execute(token, "/book/listAllBooks", params);
		exceptionFactory.checkServerFaultException(doc);
		List<AddressBook> addressBooks = respParser.parseListAddressBook(doc);
		return addressBooks;
	}
	
	@Override
	public Contact modifyContact(AccessToken token, BookType book, Contact contact) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		params.put("book", book.toString());
		String ct = biw.getContactAsString(contact);
		params.put("contact", ct);
		Document doc = execute(token, "/book/modifyContact", params);
		exceptionFactory.checkServerFaultException(doc);
		return respParser.parseContact(doc.getDocumentElement());
	}

	@Override
	public Contact removeContact(AccessToken token, BookType book, String uid) throws ServerFault, ContactNotFoundException {
		Multimap<String, String> params = initParams(token);
		params.put("book", book.toString());
		params.put("id", uid);
		Document doc = execute(token, "/book/removeContact", params);
		exceptionFactory.checkContactNotFoundException(doc);
		return respParser.parseContact(doc.getDocumentElement());
	}

	@Override
	public List<Contact> searchContact(AccessToken token, String query, int limit) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		params.put("query", query);
		params.put("limit", "" + limit);
		Document doc = execute(token, "/book/searchContact", params);
		exceptionFactory.checkServerFaultException(doc);
		return respParser.parseListContact(doc.getDocumentElement());
	}

	@Override
	public List<Contact> searchContactInGroup(AccessToken token, AddressBook group, String query, int limit) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		params.put("query", query);
		params.put("limit", String.valueOf(limit));
		params.put("group", String.valueOf(group.getUid()));
		Document doc = execute(token, "/book/searchContactInGroup", params);
		exceptionFactory.checkServerFaultException(doc);
		return respParser.parseListContact(doc.getDocumentElement());
	}
	
	@Override
	public AddressBookChangesResponse getAddressBookSync(AccessToken token, Date lastSync) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		if (lastSync != null) {
			params.put("lastSync", DateHelper.asString(lastSync));
		} else {
			params.put("lastSync", "0");
		}

		Document doc = execute(token, "/book/getAddressBookSync", params);
		exceptionFactory.checkServerFaultException(doc);
		return respParser.parseAddressBookChanges(doc);
	}
	
	@Override
	public Contact createContactInBook(AccessToken token, int addressBookId, Contact contact) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		params.put("bookId", String.valueOf(addressBookId));
		params.put("contact", biw.getContactAsString(contact));
		Document doc = execute(token, "/book/createContactInBook", params);
		exceptionFactory.checkServerFaultException(doc);
		return respParser.parseContact(doc.getDocumentElement());
	}
	
	@Override
	public Contact getContactInBook(AccessToken token, int addressBookId, String id) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		params.put("bookId", String.valueOf(addressBookId));
		params.put("id", id);
		Document doc = execute(token, "/book/getContactInBook", params);
		return respParser.parseContact(doc.getDocumentElement());
	}
	
	@Override
	public Contact modifyContactInBook(AccessToken token, int addressBookId, Contact contact) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		params.put("bookId", String.valueOf(addressBookId));
		String ct = biw.getContactAsString(contact);
		params.put("contact", ct);
		Document doc = execute(token, "/book/modifyContactInBook", params);
		exceptionFactory.checkServerFaultException(doc);
		return respParser.parseContact(doc.getDocumentElement());
	}
	
	@Override
	public Contact removeContactInBook(AccessToken token, int addressBookId, String uid) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		params.put("bookId", String.valueOf(addressBookId));
		params.put("id", uid);
		Document doc = execute(token, "/book/removeContactInBook", params);
		exceptionFactory.checkServerFaultException(doc);
		return respParser.parseContact(doc.getDocumentElement());
	}

	@Override
	public boolean unsubscribeBook(AccessToken token, Integer addressBookId) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		params.put("bookId", String.valueOf(addressBookId));
		Document doc = execute(token, "/book/unsubscribeBook", params);
		exceptionFactory.checkServerFaultException(doc);
		return "true".equalsIgnoreCase(DOMUtils.getElementText(doc
				.getDocumentElement(), "value"));
	}

	@Override
	public FolderChanges listAddressBooksChanged(AccessToken token, Date lastSync) throws ServerFault {
		Multimap<String, String> params = initParams(token);
		params.put("lastSync", DateHelper.asString(lastSync));
		Document doc = execute(token, "/book/listAddressBooksChanged", params);
		exceptionFactory.checkServerFaultException(doc);
		return respParser.parseFolderChangesResponse(doc);
	}

	@Override
	protected Locator getLocator() {
		return Locator;
	}
	
}