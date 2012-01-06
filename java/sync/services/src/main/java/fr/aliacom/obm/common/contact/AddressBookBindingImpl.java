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
package fr.aliacom.obm.common.contact;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.NoPermissionException;

import org.obm.annotations.transactional.Transactional;
import org.obm.configuration.ContactConfiguration;
import org.obm.push.utils.DateUtils;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.auth.EventNotFoundException;
import org.obm.sync.auth.ServerFault;
import org.obm.sync.base.KeyList;
import org.obm.sync.book.AddressBook;
import org.obm.sync.book.BookType;
import org.obm.sync.book.Contact;
import org.obm.sync.book.Folder;
import org.obm.sync.exception.ContactAlreadyExistException;
import org.obm.sync.exception.ContactNotFoundException;
import org.obm.sync.items.AddressBookChangesResponse;
import org.obm.sync.items.ContactChanges;
import org.obm.sync.items.FolderChanges;
import org.obm.sync.services.IAddressBook;
import org.obm.sync.utils.DateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import fr.aliacom.obm.common.FindException;
import fr.aliacom.obm.common.StoreException;
import fr.aliacom.obm.services.constant.ConstantService;
import fr.aliacom.obm.utils.LogUtils;
import fr.aliacom.obm.utils.ObmHelper;

/**
 * OBM {@link IAddressBook} web service implementation
 */
@Singleton
public class AddressBookBindingImpl implements IAddressBook {

	private static final Logger logger = LoggerFactory
			.getLogger(AddressBookBindingImpl.class);
	public static final String GLOBAL_ADDRESS_BOOK_SYNC = "globalAddressBookSync";
	public static final boolean GLOBAL_ADDRESS_BOOK_SYNC_DEFAULT_VALUE = true;

	private final ContactDao contactDao;
	private final UserDao userDao;
	private final ObmHelper obmHelper;
	private final ContactMerger contactMerger;
	private final ConstantService configuration;
	private final ContactConfiguration contactConfiguration;

	@Inject
	/*package*/ AddressBookBindingImpl(ContactDao contactDao, UserDao userDao, ContactMerger contactMerger, ObmHelper obmHelper, 
			ConstantService configuration, ContactConfiguration contactConfiguration) {
		this.contactDao = contactDao;
		this.userDao = userDao;
		this.contactMerger = contactMerger;
		this.obmHelper = obmHelper;
		this.configuration = configuration;
		this.contactConfiguration = contactConfiguration;
	}

	@Override
	@Transactional
	public boolean isReadOnly(AccessToken token, BookType book) {
		if (book == BookType.contacts) {
			return false;
		}
		return true;
	}

	@Override
	@Transactional
	public BookType[] listBooks(AccessToken token) {
		return new BookType[] { BookType.contacts, BookType.users };
	}

	@Override
	@Transactional
	public List<AddressBook> listAllBooks(AccessToken token) throws ServerFault {
		Connection con = null;
		try {
			con = obmHelper.getConnection();
			List<AddressBook> addressBooks = contactDao.findAddressBooks(con, token);
			addressBooks.add(
					new AddressBook(contactConfiguration.getAddressBookUsersName(), 
							contactConfiguration.getAddressBookUserId(), true));
			return addressBooks;
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault("error finding addressbooks ");
		} finally {
			obmHelper.cleanup(con, null, null);
		}
	}
	
	@Override
	@Transactional
	public ContactChanges listContactsChanged(AccessToken token, Date lastSync) throws ServerFault {
		logger.info(LogUtils.prefix(token) + "AddressBook : listContactsChanged");
		try {
			return getContactsChanges(token, lastSync);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
	}
	
	@Override
	@Transactional
	public AddressBookChangesResponse getAddressBookSync(AccessToken token, Date timestamp)
			throws ServerFault {
		try {
			logger.info(LogUtils.prefix(token) + "AddressBook : getAddressBookSync()");
			return getSync(token, timestamp);
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault("error synchronizing contacts ", e);
		}
	}
	
	private AddressBookChangesResponse getSync(AccessToken token, Date timestamp) throws ServerFault {
		AddressBookChangesResponse response = new AddressBookChangesResponse();
		Connection connection = null;
		try {
			connection = obmHelper.getConnection();
			response.setContactChanges(getContactsChanges(token, timestamp));
			response.setBooksChanges(listAddressBooksChanged(token, timestamp));
			response.setLastSync(obmHelper.selectNow(connection));
		} catch (Throwable t) {
			logger.error(LogUtils.prefix(token) + t.getMessage(), t);
			throw new ServerFault(t);
		} finally {
			obmHelper.cleanup(connection, null, null);
		}
		
		return response;
	}
	
	private ContactChanges getContactsChanges(AccessToken token, Date timestamp) throws ServerFault, SQLException {
		ContactUpdates contactUpdates = contactDao.findUpdatedContacts(timestamp, token);

		ContactUpdates userUpdates = new ContactUpdates();
		if (configuration.getBooleanValue(GLOBAL_ADDRESS_BOOK_SYNC,
				GLOBAL_ADDRESS_BOOK_SYNC_DEFAULT_VALUE)) {
			userUpdates = userDao.findUpdatedUsers(timestamp, token);
		} else {
			userUpdates = new ContactUpdates();
		}
		Set<Integer> removalCandidates = contactDao.findRemovalCandidates(timestamp, token);
		
		return new ContactChanges(getUpdatedContacts(contactUpdates, userUpdates),
				getRemovedContacts(contactUpdates, userUpdates, removalCandidates), getLastSync());
	}
	
	private Set<Integer> getRemovedContacts(ContactUpdates contactUpdates,
			ContactUpdates userUpdates, Set<Integer> removalCandidates) {
		SetView<Integer> archived = Sets.union( contactUpdates.getArchived(), userUpdates.getArchived());
		return Sets.union(archived, removalCandidates);
	}

	private List<Contact> getUpdatedContacts(ContactUpdates contactUpdates, ContactUpdates userUpdates) {
		List<Contact> updates = new ArrayList<Contact>(contactUpdates.getContacts().size()+userUpdates.getContacts().size());
		updates.addAll(contactUpdates.getContacts());
		updates.addAll(userUpdates.getContacts());
		return updates;
	}

	@Override
	@Transactional
	public Contact createContact(AccessToken token, Integer addressBookId, Contact contact) 
			throws ServerFault, ContactAlreadyExistException, NoPermissionException {
		
		try {
			if (addressBookId.intValue() == contactConfiguration.getAddressBookUserId()) {
				throw new NoPermissionException("no permission to add a contact to address book users.");
			} else {
				if (!contactAlreadyExist(token, contact)) {
					return contactDao.createContactInAddressBook(token, contact, addressBookId);
				}
				throw new ContactAlreadyExistException("Contact already exist", contact);	
			}
		} catch (SQLException e) {
			throw new ServerFault(e.getMessage());
		}
	}
	
	private boolean contactAlreadyExist(AccessToken token, Contact contact) {
		List<String> duplicates = contactDao.findContactTwinKeys(token, contact);
		if (duplicates != null && !duplicates.isEmpty()) {
			return true;
		}
		return false;
	}

	private void checkContactsAddressBook(AccessToken token, BookType book) throws StoreException {
		if (isReadOnly(token, book)) {
			throw new StoreException("users not writable");
		}
		if (book != BookType.contacts) {
			throw new StoreException("booktype not supported");
		}
	}

	@Override
	@Transactional
	public Contact modifyContact(AccessToken token, BookType book, Contact contact)
		throws ServerFault {

		try {
			checkContactsAddressBook(token, book);
			
			Contact c = modifyContact(token, contact);

			logger.info(LogUtils.prefix(token) + "contact[" + c.getFirstname()
					+ " " + c.getLastname() + "] modified");
			return c;
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	private Contact modifyContact(AccessToken token, Contact c) throws SQLException, FindException, EventNotFoundException, ServerFault {
		Contact modifiedContact;
		try {
			Contact previous = contactDao.findContact(token, c.getUid());
			if (!contactDao.hasRightsOn(token, c.getUid())) {
				logger.warn("contact " + c.getLastname() + " " + c.getFirstname()
						+ "(" + c.getUid() + ") not modified. not allowed for "
						+ token.getEmail());
				modifiedContact = previous;
			} else {
				contactMerger.merge(previous, c);
				modifiedContact = contactDao.modifyContact(token, c);
			}
		} catch (ContactNotFoundException e) {
			logger.warn("previous version not found for c.uid: " + c.getUid() + " c.last: " + c.getLastname());
			modifiedContact = c;
		}
		return modifiedContact;
	}
	
	@Override
	@Transactional
	public Contact removeContact(AccessToken token, Integer addressBookId, Integer contactId) 
			throws ServerFault, ContactNotFoundException, NoPermissionException {
		
		if (addressBookId.intValue() == contactConfiguration.getAddressBookUserId()) {
			throw new NoPermissionException("no permission to delete an user obm contact.");
		} else {
			try {
				return contactDao.removeContact(token, contactId);
			} catch (SQLException e) {
				throw new ServerFault(e);
			}
		}
	}

	@Override
	@Transactional
	public Contact getContactFromId(AccessToken token, Integer addressBookId, Integer contactId) 
			throws ServerFault, ContactNotFoundException {
		try {
			if (addressBookId.intValue() == contactConfiguration.getAddressBookUserId()) {
				return userDao.findUserObmContact(token, contactId);
			} else {
				return contactDao.findContact(token, contactId);
			}
		} catch (SQLException e) {
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public KeyList getContactTwinKeys(AccessToken token, BookType book, Contact contact)
		throws ServerFault {

		try {
			checkContactsAddressBook(token, book);
			
			List<String> keys = contactDao.findContactTwinKeys(token, contact);
			return new KeyList(keys);

		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public List<Contact> searchContact(AccessToken token, String query, int limit) 
		throws ServerFault {

		try {
			return contactDao.searchContact(token, query, limit);
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public List<Contact> searchContactInGroup(AccessToken token, AddressBook book, String query, int limit) throws ServerFault {

		try {
			return contactDao.searchContact(token, book, query, limit);
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public Contact modifyContactInBook(AccessToken token, int addressBookId,
			Contact contact) throws ServerFault {
		try  {
			return modifyContact(token, contact);
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e);
		}
	}
	
	@Override
	@Transactional
	public boolean unsubscribeBook(AccessToken token, Integer addressBookId) throws ServerFault {
		try {
			return contactDao.unsubscribeBook(token, addressBookId);
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}
	
	@Override
	@Transactional
	public FolderChanges listAddressBooksChanged(AccessToken token, Date timestamp) throws ServerFault {
		List<Folder> updated = contactDao.findUpdatedFolders(timestamp, token);
		if (configuration.getBooleanValue(GLOBAL_ADDRESS_BOOK_SYNC,
				GLOBAL_ADDRESS_BOOK_SYNC_DEFAULT_VALUE)) {
			updated.addAll(createAddressBookForUsers(timestamp));
		}
		
		Set<Integer> removed = contactDao.findRemovedFolders(timestamp, token);
		return new FolderChanges(updated, removed, getLastSync());
	}
	
	private boolean isFirstSync(Date timestamp) {
		if (timestamp == null || DateHelper.asDate("0").equals(timestamp) || 
				(DateUtils.getEpochPlusOneSecondCalendar().getTime().getTime() == timestamp.getTime())) {
			return true;
		}
		return false;
	}
	
	private List<Folder> createAddressBookForUsers(Date timestamp) {
		if (isFirstSync(timestamp)) {
			Folder folder = new Folder();
			folder.setUid(contactConfiguration.getAddressBookUserId());
			folder.setName(contactConfiguration.getAddressBookUsersName());
			return ImmutableList.of(folder);
		}
		return ImmutableList.of();
	}

	@Override
	@Transactional
	public ContactChanges listContactsChanged(AccessToken token, Date lastSync, Integer addressBookId) throws ServerFault {
		try {
			Set<Integer> removal = new HashSet<Integer>();
			ContactUpdates contactUpdates = null;
			
			if (addressBookId == contactConfiguration.getAddressBookUserId()) {
				contactUpdates = userDao.findUpdatedUsers(lastSync, token);
				removal = userDao.findRemovalCandidates(lastSync, token);
			} else {
				contactUpdates = contactDao.findUpdatedContacts(lastSync, addressBookId, token);
				removal = contactDao.findRemovalCandidates(lastSync, addressBookId, token);
			}
			
			return new ContactChanges(contactUpdates.getContacts(), removal, getLastSync());
	
		} catch (SQLException ex) {
			throw new ServerFault(ex);
		}
	}
	
	private Date getLastSync() throws ServerFault {
		Connection connection = null;
		try {
			connection = obmHelper.getConnection();
			return obmHelper.selectNow(connection);
		} catch (SQLException ex) {
			throw new ServerFault(ex);
		} finally {
			obmHelper.cleanup(connection, null, null);
		}
	}
	
}