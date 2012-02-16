package fr.aliasource.funambol.engine.source;

import java.io.Serializable;
import java.security.Principal;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.funambol.framework.engine.SyncItemKey;
import com.funambol.framework.engine.source.AbstractSyncSource;
import com.funambol.framework.engine.source.SyncContext;
import com.funambol.framework.engine.source.SyncSource;
import com.funambol.framework.engine.source.SyncSourceException;
import com.funambol.framework.engine.source.SyncSourceInfo;
import com.funambol.framework.server.Sync4jDevice;
import com.funambol.framework.server.store.PersistentStore;
import com.funambol.framework.server.store.PersistentStoreException;
import com.funambol.framework.tools.beans.BeanInitializationException;
import com.funambol.framework.tools.beans.LazyInitBean;
import com.funambol.server.config.Configuration;

/**
 */
public abstract class ObmSyncSource extends AbstractSyncSource implements
		SyncSource, Serializable, LazyInitBean {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8641196924967132469L;

	protected Principal principal = null;

	protected Sync4jDevice device = null;
	protected String deviceTimezoneDescr = null;
	protected TimeZone deviceTimezone = null;
	protected String deviceCharset = null;

	public static final String MSG_TYPE_VCARD = "text/x-vcard";
	public static final String MSG_TYPE_ICAL = "text/x-vcalendar";

	private boolean encode = true;

	private int restrictions = 1; // default private
	private String obmAddress = null;

	private static final Logger logger = LoggerFactory.getLogger(ObmSyncSource.class);

	/** Creates a new instance of AbstractSyncSource */
	public ObmSyncSource() {
		logger.info("obmSyncSource ctor");
	}

	/**
	 * Equivalent of deprecated method
	 * 
	 * @return syncsource type
	 */
	public String getSourceType() {
		if (getInfo() != null && getInfo().getPreferredType() != null) {
			return getInfo().getSupportedTypes()[0].getType();
		} else {
			return "";
		}
	}

	/**
	 * Equivalent of deprecated method
	 * 
	 * @param type
	 * @param version
	 */
	/*
	 * public void setSourceType(String type, String version) { ContentType[]
	 * contents = new ContentType[1]; ContentType content = new
	 * ContentType(type,version); contents[0] = content; setInfo(new
	 * SyncSourceInfo(contents, 0)); }
	 */

	public boolean isEncode() {
		return encode;
	}

	public void setEncode(boolean encode) {
		this.encode = encode;
	}

	/**
	 * Returns a string representation of this object.
	 * 
	 * @return a string representation of this object.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());

		sb.append(" - {name: ").append(getName());
		sb.append(" type: ").append(getSourceType());
		sb.append(" uri: ").append(getSourceURI());
		sb.append("}");
		return sb.toString();
	}

	/**
	 * SyncSource's beginSync()
	 * 
	 * @param context
	 *            the context of the sync
	 */
	public void beginSync(SyncContext context) throws SyncSourceException {
		super.beginSync(context);

		this.principal = context.getPrincipal();

		String deviceId = null;
		deviceId = context.getPrincipal().getDeviceId();
		try {
			device = getDevice(deviceId);
			String timezone = device.getTimeZone();
			// FIXME
			//if (device.getConvertDate()) {
				if (timezone != null && timezone.length() > 0) {
					deviceTimezoneDescr = timezone;
					deviceTimezone = TimeZone.getTimeZone(deviceTimezoneDescr);
				}
			//}

			deviceCharset = device.getCharset();
			logger.info("device charset : " + deviceCharset);
		} catch (PersistentStoreException e1) {
			logger.error("obm : error getting device");
		}

	}

	/**
	 * @see SyncSource
	 */
	public void setOperationStatus(String operation, int statusCode,
			SyncItemKey[] keys) {

		StringBuffer message = new StringBuffer("Received status code '");
		message.append(statusCode).append("' for a '").append(operation)
				.append("'").append(" for this items: ");

		for (int i = 0; i < keys.length; i++) {
			message.append("\n  - " + keys[i].getKeyAsString());
		}

		logger.info(message.toString());
	}

	public SyncItemKey[] getSyncItemKeysFromKeys(List<String> keys) {
		int nb = 0;
		SyncItemKey[] syncKeys = null;
		if (keys != null) {
			nb = keys.size();
			syncKeys = new SyncItemKey[nb];
			for (int i = 0; i < nb; i++) {
				syncKeys[i] = new SyncItemKey(keys.get(i));
			}
		}

		return syncKeys;
	}

	/**
	 * Return the device with the given deviceId
	 * 
	 * @param deviceId
	 *            String
	 * @return Sync4jDevice
	 * @throws PersistentStoreException
	 */
	private Sync4jDevice getDevice(String deviceId)
			throws PersistentStoreException {
		Sync4jDevice device = new Sync4jDevice(deviceId);
		PersistentStore store = Configuration.getConfiguration().getStore();
		store.read(device);
		return device;
	}

	public int getRestrictions() {
		if (logger.isTraceEnabled()) {
			logger.trace(" getRestrcitions:" + restrictions);
		}
		return restrictions;
	}

	public void setRestrictions(int restrictions) {
		if (logger.isTraceEnabled()) {
			logger.trace(" setRestrcitions:" + restrictions);
		}
		this.restrictions = restrictions;
	}

	public String getObmAddress() {
		return obmAddress;
	}

	public void setObmAddress(String obmAddress) {
		this.obmAddress = obmAddress;
	}

	@Override
	public void commitSync() throws SyncSourceException {
		super.commitSync();
		logger.info("commit sync");
	}

	@Override
	public SyncSourceInfo getInfo() {
		SyncSourceInfo info = super.getInfo();
		return info;
	}

	@Override
	public String getName() {
		String name = super.getName();
		logger.info("getName: " + name);
		return name;
	}

	@Override
	public String getSourceQuery() {
		String ret = super.getSourceQuery();
		logger.info("getsourcequery: " + ret);
		return ret;
	}

	@Override
	public String getSourceURI() {
		String ret = super.getSourceURI();
		logger.info("getsourceuri: " + ret);
		return ret;
	}
	@Override
	public void init() throws BeanInitializationException {
	}
}
