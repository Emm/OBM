package org.obm.push;

import java.math.BigDecimal;

import org.obm.push.bean.BackendSession;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.exception.DaoException;
import org.obm.push.protocol.request.ActiveSyncRequest;
import org.obm.push.service.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SessionService {

	private static final Logger logger = LoggerFactory.getLogger(SessionService.class);
	private final DeviceService deviceService;
	
	@Inject
	private SessionService(DeviceService deviceService) {
		this.deviceService = deviceService;
	}
	
	public BackendSession getSession(
			Credentials credentials, String devId, ActiveSyncRequest request) throws DaoException {

		String sessionId = credentials.getUser().getLoginAtDomain() + "/" + devId;
		return createSession(credentials, request, sessionId);
	}

	private BackendSession createSession(Credentials credentials,
			ActiveSyncRequest r, String sessionId) throws DaoException {
		
		String userAgent = r.getUserAgent();
		String devId = r.getDeviceId();
		
		Device device = deviceService.getDevice(credentials.getUser(), devId, userAgent);
		
		BackendSession bs = new BackendSession(credentials, 
				r.getCommand(), device, getProtocolVersion(r));
		
		
		logger.debug("New session = {}", sessionId);
		return bs;
	}

	private BigDecimal getProtocolVersion(ActiveSyncRequest request) {
		final String proto = request.getMSASProtocolVersion();
		if (proto != null) {
			try {
				BigDecimal protocolVersion = new BigDecimal(proto);
				logger.debug("Client supports protocol = {}", protocolVersion);
				return protocolVersion;
			} catch (NumberFormatException nfe) {
				logger.warn("invalid MS-ASProtocolVersion = {}", proto);
			}
		}
		return new BigDecimal("12.1");
	}

}
