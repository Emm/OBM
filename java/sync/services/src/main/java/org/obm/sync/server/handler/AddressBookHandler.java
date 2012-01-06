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
package org.obm.sync.server.handler;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.FactoryConfigurationError;

import org.obm.sync.auth.AccessToken;
import org.obm.sync.auth.ContactNotFoundException;
import org.obm.sync.auth.ServerFault;
import org.obm.sync.base.KeyList;
import org.obm.sync.book.AddressBook;
import org.obm.sync.book.BookItemsParser;
import org.obm.sync.book.BookType;
import org.obm.sync.book.Contact;
import org.obm.sync.items.AddressBookChangesResponse;
import org.obm.sync.items.ContactChanges;
import org.obm.sync.items.FolderChanges;
import org.obm.sync.server.ParametersSource;
import org.obm.sync.server.XmlResponder;
import org.obm.sync.services.IAddressBook;
import org.obm.sync.utils.DateHelper;
import org.xml.sax.SAXException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import fr.aliacom.obm.common.contact.AddressBookBindingImpl;
import fr.aliacom.obm.common.session.SessionManagement;

@Singleton
public class AddressBookHandler extends SecureSyncHandler {

	private IAddressBook binding;
	private BookItemsParser bip;

	@Inject
	private AddressBookHandler(SessionManagement sessionManagement, AddressBookBindingImpl addressBookBindingImpl) {
		super(sessionManagement);
		binding = addressBookBindingImpl;
		bip = new BookItemsParser();
	}

	@Override
	public void handle(String method, ParametersSource params, XmlResponder responder) throws Exception {
		
		AccessToken token = getCheckedToken(params);
		
		if ("isReadOnly".equals(method)) {
			isReadOnly(token, params, responder);
		} else if ("listBooks".equals(method)) {
			listBooks(token, responder);
		} else if ("listAllBooks".equals(method)) {
			listAllBooks(token, responder);
		} else if ("listContactsChanged".equals(method)) {
			listContactsChanged(token, params, responder);
		} else if ("createContact".equals(method)) {
			createContact(token, params, responder);
		} else if ("createContactWithoutDuplicate".equals(method)) {
			createContactWithoutDuplicate(token, params, responder);
		} else if ("modifyContact".equals(method)) {
			modifyContact(token, params, responder);
		} else if ("removeContact".equals(method)) {
			removeContact(token, params, responder);
		} else if ("getContactFromId".equals(method)) {
			getContactFromId(token, params, responder);
		} else if ("getContactTwinKeys".equals(method)) {
			getContactTwinKeys(token, params, responder);
		} else if ("searchContact".equals(method)) {
			searchContact(token, params, responder);
		} else if ("searchContactInGroup".equals(method)) {
			searchContactInGroup(token, params, responder);
		} else if ("getAddressBookSync".equals(method)) {
			getAddressBookSync(token, params, responder);
		} else if ("createContactInBook".equals(method)) {
			createContactInBook(token, params, responder);
		} else if ("modifyContactInBook".equals(method)) {
			modifyContactInBook(token, params, responder);
		} else if ("removeContactInBook".equals(method)) {
			removeContactInBook(token, params, responder);
		} else if ("unsubscribeBook".equals(method)) {
			unsubscribeBook(token, params, responder);
		} else if ("listAddressBooksChanged".equals(method)) {
			listAddressBooksChanged(token, params, responder);
		} else {
			responder.sendError("Cannot handle method '" + method + "'");
		}
	}

	private void unsubscribeBook(AccessToken token, ParametersSource params,
			XmlResponder responder) throws ServerFault {
		boolean ret = binding.unsubscribeBook(token, getBookId(params));
		responder.sendBoolean(ret);
	}

	private void getContactTwinKeys(AccessToken at, ParametersSource params, XmlResponder responder) throws SAXException, IOException, FactoryConfigurationError, ServerFault {
		Contact contact = getContactFromParams(params);
		KeyList ret = binding.getContactTwinKeys(at, type(params), contact);
		responder.sendKeyList(ret);
	}

	private BookType type(ParametersSource params) {
		return BookType.valueOf(params.getParameter("book"));
	}

	private void getContactFromId(AccessToken at, ParametersSource params, XmlResponder responder) 
			throws ServerFault, ContactNotFoundException {
		
		Integer contactId = Integer.valueOf( p(params, "contactId") );
		Integer addressBookId = Integer.valueOf( p(params, "addressBookId") );
		Contact ret = binding.getContactFromId(at, addressBookId, contactId);
		responder.sendContact(ret);
	}

	private void removeContact(AccessToken at, ParametersSource params, XmlResponder responder) 
			throws ServerFault, ContactNotFoundException {
		
		Contact ret = binding.removeContact(at, type(params), p(params, "id"));
		responder.sendContact(ret);
	}

	private void modifyContact(AccessToken at, ParametersSource params, XmlResponder responder) throws ServerFault, SAXException, IOException, FactoryConfigurationError {
		Contact contact = getContactFromParams(params);
		Contact ret = binding.modifyContact(at, type(params), contact);
		responder.sendContact(ret);
	}

	private void createContactWithoutDuplicate(AccessToken at, ParametersSource params, XmlResponder responder) throws ServerFault, SAXException, IOException, FactoryConfigurationError {
		Contact contact = getContactFromParams(params);
		Contact ret = binding.createContactWithoutDuplicate(at,	type(params), contact);
		responder.sendContact(ret);
	}

	private void createContact(AccessToken at, ParametersSource params, XmlResponder responder) throws ServerFault, SAXException, IOException, FactoryConfigurationError {
			Contact contact = getContactFromParams(params);
			Contact ret = binding.createContact(at, type(params), contact);
			responder.sendContact(ret);
	}

	private Contact getContactFromParams(ParametersSource params)
			throws SAXException, IOException, FactoryConfigurationError {
		return bip.parseContact(p(params, "contact"));
	}

	private void listContactsChanged(AccessToken at, ParametersSource params, XmlResponder responder) throws ServerFault {
		Date lastSync = getLastSyncFromParams(params);
		String addressBookId = p(params, "addressBookId");
		ContactChanges contactChanges = null;
		if (addressBookId == null) {
			contactChanges = binding.listContactsChanged(at, lastSync);
		} else {
			contactChanges = binding.listContactsChanged(at, lastSync, Integer.valueOf(addressBookId));
		}
		responder.sendContactChanges(contactChanges);
	}

	private Date getLastSyncFromParams(ParametersSource params) {
		return DateHelper.asDate(p(params, "lastSync"));
	}

	private void listBooks(AccessToken at, XmlResponder responder) throws ServerFault {
		BookType[] ret = binding.listBooks(at);
		String[] st = new String[ret.length];
		for (int i = 0; i < st.length; i++) {
			st[i] = ret[i].toString();
		}
		responder.sendArrayOfString(st);
	}

	private void listAllBooks(AccessToken at, XmlResponder responder) throws ServerFault {
		List<AddressBook> ret = binding.listAllBooks(at);
		responder.sendListAddressBooks(ret);
	}
	
	private void isReadOnly(AccessToken at, ParametersSource params, XmlResponder responder) throws ServerFault {
		boolean ret = binding.isReadOnly(at, type(params));
		responder.sendBoolean(ret);
	}

	private void searchContact(AccessToken at, ParametersSource params, XmlResponder responder) throws ServerFault {
		String query = p(params, "query");
		int limit = Integer.parseInt(p(params, "limit"));
		List<Contact> ret = binding.searchContact(at, query, limit);
		responder.sendListContact(ret);
	}

	private void searchContactInGroup(AccessToken at, ParametersSource params, XmlResponder responder) throws ServerFault {
		String query = p(params, "query");
		int limit = Integer.valueOf(p(params, "limit"));
		int groupId = Integer.valueOf(p(params, "group"));
		AddressBook book = getAddressBookFromUid(at, groupId);
		List<Contact> ret = binding.searchContactInGroup(at, book, query, limit);
		responder.sendListContact(ret);
	}

	private AddressBook getAddressBookFromUid(AccessToken at, int uid) throws ServerFault {
		List<AddressBook> allBooks = binding.listAllBooks(at);
		for (AddressBook book: allBooks) {
			if (book.getUid() == uid) {
				return book;
			}
		}
		throw new RuntimeException("book with uid " + uid + " not found");
	}

	private Integer getBookId(ParametersSource params) {
		return Integer.valueOf(p(params, "bookId"));
	}
	
	private void removeContactInBook(AccessToken token, ParametersSource params, XmlResponder responder) 
			throws ServerFault, ContactNotFoundException {
		
		Contact ret = binding.removeContactInBook(token, getBookId(params), p(params, "id"));
		responder.sendContact(ret);
	}

	private void modifyContactInBook(AccessToken token, ParametersSource params, XmlResponder responder) throws ServerFault, SAXException, IOException, FactoryConfigurationError {
		Contact contact = getContactFromParams(params);
		Contact ret = binding.modifyContactInBook(token, getBookId(params), contact);
		responder.sendContact(ret);		
	}

	private void createContactInBook(AccessToken token,
			ParametersSource params, XmlResponder responder) throws SAXException, IOException, FactoryConfigurationError, ServerFault {
		Contact contact = getContactFromParams(params);
		Contact ret = binding.createContactInBook(token, getBookId(params), contact);
		responder.sendContact(ret);		
	}

	private void getAddressBookSync(AccessToken token, ParametersSource params,
			XmlResponder responder) throws ServerFault {
		Date lastSync = getLastSyncFromParams(params);
		AddressBookChangesResponse response = binding.getAddressBookSync(token, lastSync);
		responder.sendAddressBookChanges(response);	
	}
	
	private void listAddressBooksChanged(AccessToken token, ParametersSource params, XmlResponder responder) throws ServerFault {
		Date lastSync = getLastSyncFromParams(params);
		FolderChanges folderChanges = binding.listAddressBooksChanged(token, lastSync);
		responder.sendlistAddressBooksChanged(folderChanges);	
	}
	
}
