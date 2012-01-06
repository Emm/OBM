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
package org.obm.push.mail;

import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.columba.ristretto.message.Address;
import org.minig.imap.IMAPException;
import org.obm.locator.LocatorClientException;
import org.obm.push.bean.BackendSession;
import org.obm.push.bean.Email;
import org.obm.push.bean.MSEmail;
import org.obm.push.bean.SyncState;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.SendEmailException;
import org.obm.push.exception.SmtpInvalidRcptException;
import org.obm.push.exception.activesync.ProcessingEmailException;
import org.obm.push.exception.activesync.StoreEmailException;
import org.obm.sync.services.ICalendar;

public interface IEmailManager {

	MailChanges getSync(BackendSession bs, SyncState state, Integer devId, Integer collectionId, String collectionName)
			throws IMAPException, DaoException, LocatorClientException;

	List<MSEmail> fetchMails(BackendSession bs, ICalendar calendarClient, Integer collectionId, String collectionName, 
			Collection<Long> uids) throws IMAPException, LocatorClientException;

	void updateReadFlag(BackendSession bs, String collectionName, Long uid, boolean read) throws IMAPException, LocatorClientException;

	String parseMailBoxName(BackendSession bs, String collectionName) throws IMAPException, LocatorClientException;

	void delete(BackendSession bs, Integer devId, String collectionPath, Integer collectionId, Long uid) throws IMAPException, DaoException, LocatorClientException;

	Long moveItem(BackendSession bs, Integer devId, String srcFolder, Integer srcFolderId, String dstFolder, Integer dstFolderId, 
			Long uid) throws IMAPException, DaoException, LocatorClientException;

	List<InputStream> fetchMIMEMails(BackendSession bs, ICalendar calendarClient, String collectionName, 
			Set<Long> uids) throws IMAPException, LocatorClientException;

	void setAnsweredFlag(BackendSession bs, String collectionName, Long uid) throws IMAPException, LocatorClientException;

	void sendEmail(BackendSession bs, Address from, Set<Address> setTo, Set<Address> setCc, Set<Address> setCci, InputStream mimeMail,
			Boolean saveInSent) throws SendEmailException, ProcessingEmailException, SmtpInvalidRcptException, StoreEmailException;

	InputStream findAttachment(BackendSession bs, String collectionName, Long mailUid, String mimePartAddress) throws IMAPException, LocatorClientException;

	void purgeFolder(BackendSession bs, Integer devId, String collectionPath, Integer collectionId) throws IMAPException, DaoException, LocatorClientException;

	Long storeInInbox(BackendSession bs, InputStream mailContent, boolean isRead) throws StoreEmailException, LocatorClientException;

	boolean getLoginWithDomain();

	boolean getActivateTLS();
	
	void updateData(Integer devId, Integer collectionId, Date lastSync, Collection<Long> removedEmailsIds, Collection<Email> updated)
			throws DaoException;

}
