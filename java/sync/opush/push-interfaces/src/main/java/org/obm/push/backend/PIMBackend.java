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

import java.util.List;

import org.obm.push.bean.BackendSession;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.IApplicationData;
import org.obm.push.bean.ItemChange;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.SyncState;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.UnexpectedObmSyncServerException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.ItemNotFoundException;
import org.obm.push.exception.activesync.NotAllowedException;
import org.obm.push.exception.activesync.ProcessingEmailException;

public interface PIMBackend {

	String createOrUpdate(BackendSession bs, Integer collectionId,
			String serverId, String clientId, IApplicationData data)
			throws CollectionNotFoundException, ProcessingEmailException, 
			DaoException, UnexpectedObmSyncServerException, ItemNotFoundException;
	
	String move(BackendSession bs, String srcFolder, String dstFolder,
			String messageId) throws CollectionNotFoundException,
			ProcessingEmailException;
	
	public void delete(BackendSession bs, Integer collectionId, String serverId, Boolean moveToTrash) 
			throws CollectionNotFoundException, DaoException, UnexpectedObmSyncServerException, ItemNotFoundException, ProcessingEmailException;
	
	DataDelta getChanged(BackendSession bs, SyncState state, FilterType filterType, Integer collectionId) 
			throws DaoException, CollectionNotFoundException, UnexpectedObmSyncServerException, ProcessingEmailException;

	List<ItemChange> fetch(BackendSession bs, List<String> itemIds)
			throws CollectionNotFoundException, DaoException, ProcessingEmailException, UnexpectedObmSyncServerException;

	int getItemEstimateSize(BackendSession bs, FilterType filterType, Integer collectionId, SyncState state) 
			throws CollectionNotFoundException, ProcessingEmailException, DaoException, UnexpectedObmSyncServerException;

	void emptyFolderContent(BackendSession bs, String collectionPath, boolean deleteSubFolder) throws NotAllowedException, CollectionNotFoundException, ProcessingEmailException;
	
	PIMDataType getPIMDataType();
	
}