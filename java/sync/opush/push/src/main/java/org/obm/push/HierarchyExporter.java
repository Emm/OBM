package org.obm.push;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.obm.push.backend.FolderBackend;
import org.obm.push.backend.IHierarchyExporter;
import org.obm.push.bean.BackendSession;
import org.obm.push.bean.HierarchyItemsChanges;
import org.obm.push.bean.ItemChange;
import org.obm.push.calendar.CalendarBackend;
import org.obm.push.contacts.ContactsBackend;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.UnknownObmSyncServerException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.mail.MailBackend;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class HierarchyExporter implements IHierarchyExporter {

	private final FolderBackend folderExporter;
	private final MailBackend mailExporter;
	private final CalendarBackend calendarExporter;
	private final ContactsBackend contactsBackend;

	@Inject
	private HierarchyExporter(FolderBackend folderExporter,
			MailBackend mailExporter, CalendarBackend calendarExporter,
			ContactsBackend contactsBackend) {
		
		this.folderExporter = folderExporter;
		this.mailExporter = mailExporter;
		this.calendarExporter = calendarExporter;
		this.contactsBackend = contactsBackend;
	}

	private List<ItemChange> getTasksChanges(BackendSession bs) throws DaoException {
		return calendarExporter.getHierarchyTaskChanges(bs);
	}

	private List<ItemChange> getCalendarChanges(BackendSession bs) throws DaoException, CollectionNotFoundException, UnknownObmSyncServerException {
		return calendarExporter.getHierarchyChanges(bs);
	}

	private List<ItemChange> getMailChanges(BackendSession bs) throws DaoException {
		return mailExporter.getHierarchyChanges(bs);
	}

	@Override
	public HierarchyItemsChanges getChanged(BackendSession bs, Date lastSync) throws DaoException, CollectionNotFoundException, UnknownObmSyncServerException {
		LinkedList<ItemChange> allItemsChanged = new LinkedList<ItemChange>();
		
		allItemsChanged.addAll(getCalendarChanges(bs));
		allItemsChanged.addAll(getMailChanges(bs));
		
		HierarchyItemsChanges itemsContactChanged = listContactFoldersChanged(bs, lastSync);
		allItemsChanged.addAll(itemsContactChanged.getItemsAddedOrUpdated());
		
		allItemsChanged.addAll(getTasksChanges(bs));

		return new HierarchyItemsChanges(
				allItemsChanged, itemsContactChanged.getItemsDeleted(), itemsContactChanged.getLastSync());
	}
	
	@Override
	public HierarchyItemsChanges listContactFoldersChanged(BackendSession bs, Date lastSync) throws DaoException, CollectionNotFoundException, UnknownObmSyncServerException {
		return contactsBackend.getHierarchyChanges(bs, lastSync);
	}
	
	@Override
	public void initHierarchyFolder(BackendSession bs) throws DaoException {
		contactsBackend.createDefaultContactFolder(bs);
	}
	
	@Override
	public int getRootFolderId(BackendSession bs) throws DaoException, CollectionNotFoundException {
		return folderExporter.getServerIdFor(bs);
	}

	@Override
	public String getRootFolderUrl(BackendSession bs) {
		return folderExporter.getColName(bs);
	}

}