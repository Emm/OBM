package fr.aliasource.funambol.engine.source;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

import org.obm.configuration.ContactConfiguration;
import org.obm.sync.client.book.BookClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.funambol.common.pim.contact.Contact;
import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.SyncItemKey;
import com.funambol.framework.engine.SyncItemState;
import com.funambol.framework.engine.source.SyncContext;
import com.funambol.framework.engine.source.SyncSource;
import com.funambol.framework.engine.source.SyncSourceException;
import com.funambol.framework.tools.beans.LazyInitBean;
import com.google.inject.Injector;

import fr.aliasource.funambol.ConvertionException;
import fr.aliasource.funambol.OBMException;
import fr.aliasource.funambol.ObmFunambolGuiceInjector;
import fr.aliasource.obm.items.converter.ObmContactConverter;
import fr.aliasource.obm.items.manager.ContactSyncBean;

public final class ContactSyncSource extends ObmSyncSource implements
		SyncSource, Serializable, LazyInitBean {

	private static final Logger logger = LoggerFactory.getLogger(ContactSyncSource.class);
	private final BookClient bookClient;
	private final ObmContactConverter contactConverter;
	private final ContactConfiguration contactConfiguration;
	
	private ContactSyncBean currentSyncBean;

	public ContactSyncSource() {
		super();
		Injector injector = ObmFunambolGuiceInjector.getInjector();
		this.bookClient = injector.getProvider(BookClient.class).get();
		this.contactConverter = injector.getProvider(ObmContactConverter.class).get();
		this.contactConfiguration = injector.getProvider(ContactConfiguration.class).get();
	}

	@Override
	public void beginSync(SyncContext context) throws SyncSourceException {
		super.beginSync(context);

		logger.info("- Begin an OBM Contact sync -");
		this.currentSyncBean = new ContactSyncBean(loginService, bookClient, contactConfiguration, contactConverter);
		
		try {
			currentSyncBean.logIn(context.getPrincipal().getUser().getUsername(),
					context.getPrincipal().getUser().getPassword());

		} catch (OBMException e) {
			throw new SyncSourceException(e);
		}
		currentSyncBean.setDeviceTimeZone(deviceTimezone);
	}

	@Override
	public void setOperationStatus(String operation, int statusCode,
			SyncItemKey[] keys) {
		super.setOperationStatus(operation, statusCode, keys);
	}

	@Override
	public SyncItem addSyncItem(SyncItem syncItem) throws SyncSourceException {
		logger.info("addSyncItem(" + principal + " , "
				+ syncItem.getKey().getKeyAsString() + ")");
		try {
			Contact contact = syncItemConverter.getFunambolContactFromSyncItem(syncItem, getSourceType(), deviceTimezone, deviceCharset, isEncode());
			contact.setUid(null);
			Contact created = currentSyncBean.addItem(contact);
			
			logger.info(" created with id : " + created.getUid());
			return syncItemConverter.getSyncItemFromFunambolContact(this, contact, SyncItemState.SYNCHRONIZED, getSourceType(), deviceTimezone, deviceCharset, isEncode());
		} catch (OBMException e) {
			throw new SyncSourceException(e);
		} catch (ConvertionException e) {
			throw new SyncSourceException(e);
		}
	}

	@Override
	public SyncItemKey[] getAllSyncItemKeys() throws SyncSourceException {
		logger.info("getAllSyncItemKeys(" + principal + ")");
		List<String> keys = null;
		try {
			keys = currentSyncBean.getAllItemKeys();
		} catch (OBMException e) {
			throw new SyncSourceException(e);
		}
		SyncItemKey[] ret = getSyncItemKeysFromKeys(keys);
		logger.info(" returning " + ret.length + " key(s)");

		return ret;
	}

	@Override
	public SyncItemKey[] getDeletedSyncItemKeys(Timestamp since, Timestamp until)
			throws SyncSourceException {
		logger.info("getDeletedSyncItemKeys(" + principal + " , " + since
				+ " , " + until + ")");
		List<String> keys = null;
		try {
			keys = currentSyncBean.getDeletedItemKeys(since);
		} catch (OBMException e) {
			throw new SyncSourceException(e);
		}
		SyncItemKey[] ret = getSyncItemKeysFromKeys(keys);
		logger.info(" returning " + ret.length + " key(s)");

		return ret;
	}

	@Override
	public SyncItemKey[] getNewSyncItemKeys(Timestamp since, Timestamp until)
			throws SyncSourceException {
		return null;
	}

	@Override
	public SyncItemKey[] getSyncItemKeysFromTwin(SyncItem syncItem)
			throws SyncSourceException {
		logger.info("getSyncItemKeysFromTwin(" + principal + ")");

		Contact contact = null;

		List<String> keys = null;
		try {
			syncItem.getKey().setKeyValue("");
			contact = syncItemConverter.getFunambolContactFromSyncItem(syncItem, getSourceType(), deviceTimezone, deviceCharset, isEncode());
			keys = currentSyncBean.getContactTwinKeys(contact);
		} catch (ConvertionException e) {
			throw new SyncSourceException(e.getMessage(), e);
		}
		SyncItemKey[] ret = getSyncItemKeysFromKeys(keys);

		logger.info(" returning " + ret.length + " key(s)");

		return ret;
	}

	@Override
	public SyncItemKey[] getUpdatedSyncItemKeys(Timestamp since, Timestamp until)
			throws SyncSourceException {
		logger.info("getUpdatedSyncItemKeys(" + principal + " , " + since
				+ " , " + until + ")");

		List<String> keys = null;
		try {
			keys = currentSyncBean.getUpdatedItemKeys(since);
		} catch (OBMException e) {
			throw new SyncSourceException(e);
		}
		SyncItemKey[] ret = getSyncItemKeysFromKeys(keys);
		logger.info(" returning " + ret.length + " key(s)");

		return ret;
	}

	@Override
	public void removeSyncItem(SyncItemKey syncItemKey, Timestamp time,
			boolean softDelete) throws SyncSourceException {
		logger.info("removeSyncItem(" + principal + " , " + syncItemKey + " , "
				+ time + ")");
		String k = syncItemKey.getKeyAsString();
		if (k == null || k.length() == 0 || "null".equalsIgnoreCase(k)) {
			logger.warn("cannot remove null sync item key, skipping.");
			return;
		}
		try {
			currentSyncBean.removeItem(syncItemKey.getKeyAsString());
		} catch (OBMException e) {
			throw new SyncSourceException(e);
		}
	}

	@Override
	public SyncItem updateSyncItem(SyncItem syncItem)
			throws SyncSourceException {
		logger.info("updateSyncItem(" + principal + " , "
				+ syncItem.getKey().getKeyAsString() + "("+syncItem.getKey()+"))");
		try {
			Contact contact = syncItemConverter.getFunambolContactFromSyncItem(syncItem, getSourceType(), deviceTimezone, deviceCharset, isEncode());
			contact = currentSyncBean.updateItem(contact);
			return syncItemConverter.getSyncItemFromFunambolContact(this, contact, SyncItemState.SYNCHRONIZED, getSourceType(), deviceTimezone, deviceCharset, isEncode());
		} catch (OBMException e) {
			throw new SyncSourceException(e);
		} catch (ConvertionException e) {
			throw new SyncSourceException(e);
		}
	}

	@Override
	public SyncItem getSyncItemFromId(SyncItemKey syncItemKey)
			throws SyncSourceException {
		logger.info("syncItemFromId(" + principal + ", " + syncItemKey + ")");

		try {
			com.funambol.common.pim.contact.Contact contact = null;
			String key = syncItemKey.getKeyAsString();
			contact = currentSyncBean.getItemFromId(key);
			SyncItem ret = syncItemConverter.getSyncItemFromFunambolContact(this, contact, SyncItemState.UNKNOWN, getSourceType(), deviceTimezone, key, isEncode());
			return ret;
		} catch (OBMException e) {
			throw new SyncSourceException(e);
		} catch (ConvertionException e) {
			throw new SyncSourceException(e);
		}
	}

	@Override
	public void endSync() throws SyncSourceException {
		currentSyncBean.logout();
		super.endSync();
	}

}
