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
package org.obm.sync.items;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.obm.sync.book.Contact;
import org.obm.sync.book.Folder;
import org.obm.sync.book.ContactKey;

public class AddressBookChangesResponse {

	private ContactChanges contactChanges;
	private FolderChanges booksChanges;
	private Date lastSync;

	public AddressBookChangesResponse() {
		contactChanges = new ContactChanges();
		booksChanges = new FolderChanges();
	}

	public Date getLastSync() {
		return lastSync;
	}

	public void setLastSync(Date lastSync) {
		this.lastSync = lastSync;
	}

	public List<Contact> getUpdatedContacts() {
		return contactChanges.getUpdated();
	}

	public Set<ContactKey> getRemovedContacts() {
		return contactChanges.getRemoved();
	}

	public void setContactChanges(ContactChanges contactChanges) {
		this.contactChanges = contactChanges;
	}
	
	public void setUpdatedAddressBooks(Set<Folder> updated) {
		this.booksChanges.setUpdated(updated);
	}
	
	public Set<Folder> getUpdatedAddressBooks() {
		return this.booksChanges.getUpdated();
	}
	
	public void setRemovedAddressBooks(Set<Folder> removed) {
		this.booksChanges.setRemoved(removed);
	}
	
	public Set<Folder> getRemovedAddressBooks() {
		return this.booksChanges.getRemoved();
	}

	public void setBooksChanges(FolderChanges booksChanges) {
		this.booksChanges = booksChanges;
	}

	public FolderChanges getBooksChanges() {
		return booksChanges;
	}
	
	public ContactChanges getContactChanges() {
		return contactChanges;
	}
}
