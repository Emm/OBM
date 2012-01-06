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
package org.obm.push.backend;

import org.obm.push.bean.AttendeeStatus;
import org.obm.push.bean.BackendSession;
import org.obm.push.bean.IApplicationData;
import org.obm.push.bean.MSEmail;
import org.obm.push.bean.PIMDataType;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.SendEmailException;
import org.obm.push.exception.SmtpInvalidRcptException;
import org.obm.push.exception.UnknownObmSyncServerException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.NotAllowedException;
import org.obm.push.exception.activesync.ProcessingEmailException;
import org.obm.push.exception.activesync.ServerItemNotFoundException;

/**
 * Content management interface, ie. CRUD API.
 */
public interface IContentsImporter {

	String importMessageChange(BackendSession bs, Integer collectionId, String serverId, String clientId, IApplicationData data)
			throws CollectionNotFoundException, DaoException, UnknownObmSyncServerException, ProcessingEmailException, ServerItemNotFoundException;

	void importMessageDeletion(BackendSession bs, PIMDataType type, Integer collectionId, String serverId, Boolean moveToTrash) 
			throws CollectionNotFoundException, DaoException, UnknownObmSyncServerException, ProcessingEmailException, ServerItemNotFoundException;

	String importMoveItem(BackendSession bs, PIMDataType type, String srcFolder, String dstFolder, String messageId)
			throws CollectionNotFoundException, DaoException, ProcessingEmailException;

	String importCalendarUserStatus(BackendSession bs, Integer invitationCollectionId, MSEmail invitation,
			AttendeeStatus userResponse) throws DaoException, CollectionNotFoundException, UnknownObmSyncServerException, ServerItemNotFoundException;

	void sendEmail(BackendSession bs, byte[] mailContent, Boolean saveInSent)
			throws SendEmailException, ProcessingEmailException, SmtpInvalidRcptException;

	void replyEmail(BackendSession bs, byte[] mailContent, Boolean saveInSent,	Integer collectionId, String serverId) 
			throws SendEmailException, ProcessingEmailException, SmtpInvalidRcptException, CollectionNotFoundException, 
			DaoException, UnknownObmSyncServerException;

	void forwardEmail(BackendSession bs, byte[] mailContent, Boolean saveInSent, String collectionId, String serverId)
			throws SendEmailException, ProcessingEmailException, SmtpInvalidRcptException, CollectionNotFoundException, 
			UnknownObmSyncServerException, DaoException;

	void emptyFolderContent(BackendSession bs, String collectionPath, boolean deleteSubFolder) 
			throws CollectionNotFoundException, NotAllowedException, DaoException, ProcessingEmailException;
	
}
