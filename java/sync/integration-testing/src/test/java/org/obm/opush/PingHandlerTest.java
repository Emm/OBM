package org.obm.opush;


import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.transform.TransformerException;

import org.easymock.EasyMock;
import org.fest.assertions.Assertions;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.obm.opush.ActiveSyncServletModule.OpushServer;
import org.obm.opush.SingleUserFixture.OpushUser;
import org.obm.opush.env.JUnitGuiceRule;
import org.obm.push.bean.ChangedCollections;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.SyncCollection;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.store.CollectionDao;
import org.obm.push.store.DeviceDao;
import org.obm.push.store.HearbeatDao;
import org.obm.push.store.MonitoredCollectionDao;
import org.obm.push.utils.DOMUtils;
import org.obm.push.utils.DateUtils;
import org.obm.push.utils.collection.ClassToInstanceAgregateView;
import org.obm.sync.auth.ServerFault;
import org.obm.sync.client.login.LoginService;
import org.obm.sync.items.EventChanges;
import org.obm.sync.push.client.OPClient;
import org.obm.sync.services.ICalendar;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class PingHandlerTest {

	@Rule
	public JUnitGuiceRule guiceBerry = new JUnitGuiceRule(PingHandlerTestModule.class);

	@Inject @PortNumber int port;
	@Inject SingleUserFixture singleUserFixture;
	@Inject OpushServer opushServer;
	@Inject ClassToInstanceAgregateView<Object> classToInstanceMap;
	
	public void testHeartbeatInterval(int heartbeatInterval, int delta, int expected) throws Exception {
		mockForNoChangePing();

		opushServer.start();
		
		OPClient opClient = buildOpushClient(singleUserFixture.jaures);
		Document document = buildPingCommand(heartbeatInterval);
		Stopwatch stopwatch = new Stopwatch().start();
		
		Document response = opClient.postXml("Ping", document, "Ping");
		
		checkExecutionTime(delta, expected, stopwatch);
		checkNoChangeResponse(response);
	}

	private void checkExecutionTime(int delta, int expected,
			Stopwatch stopwatch) {
		stopwatch.stop();
		long elapsedTime = stopwatch.elapsedTime(TimeUnit.SECONDS);
		Assertions.assertThat(elapsedTime)
			.isGreaterThanOrEqualTo(expected)
			.isLessThan(expected + delta);
	}

	private void checkNoChangeResponse(Document response)
			throws TransformerException {
		Assertions.assertThat(DOMUtils.serialise(response))
			.isEqualTo(
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
					"<Ping><Status>1</Status><Folders/></Ping>");
	}

	private void mockForNoChangePing() throws DaoException, CollectionNotFoundException, ServerFault {
		List<OpushUser> opushUsers = Arrays.asList(singleUserFixture.jaures);

		LoginService loginService = classToInstanceMap.get(LoginService.class);
		mockLoginService(loginService, opushUsers);
		
		DeviceDao deviceDao = classToInstanceMap.get(DeviceDao.class);
		mockDeviceDao(deviceDao, opushUsers);

		ICalendar iCalendar = classToInstanceMap.get(ICalendar.class);
		mockCalendar(iCalendar, opushUsers);
		
		CollectionDao collectionDao = classToInstanceMap.get(CollectionDao.class);
		mockCollectionDao(collectionDao, opushUsers);

		MonitoredCollectionDao monitoredCollectionDao = classToInstanceMap.get(MonitoredCollectionDao.class);
		mockMonitoredCollectionDao(monitoredCollectionDao);

		HearbeatDao heartbeatDao = classToInstanceMap.get(HearbeatDao.class);
		mockHeartbeatDao(heartbeatDao);
		
		EasyMock.replay(Lists.newArrayList(classToInstanceMap).toArray());
	}

	private void mockCalendar(ICalendar iCalendar, List<OpushUser> opushUsers) throws ServerFault {
		for (OpushUser user : opushUsers) {
			Date dateFirstSync = DateUtils.getEpochPlusOneSecondCalendar().getTime();
			EventChanges eventChanges = new EventChanges();
			eventChanges.setLastSync(new Date());
			expect(iCalendar.getSync(user.accessToken, user.user.getLogin(), dateFirstSync)).andReturn(eventChanges).anyTimes();
			expect(iCalendar.getUserEmail(user.accessToken)).andReturn(user.user.getEmail()).anyTimes();
		}
	}

	private void mockLoginService(LoginService loginService, List<OpushUser> list) {
		for (OpushUser user : list) {
			expect(loginService.login(user.user.getLoginAtDomain(), user.password, "o-push")).andReturn(user.accessToken).anyTimes();
			loginService.logout(user.accessToken);
			expectLastCall().anyTimes();
		}
	}

	@Test
	public void testInterval() throws Exception {
		testHeartbeatInterval(5, 5, 5);
	}

	
	@Test
	public void testMinInterval() throws Exception {
		testHeartbeatInterval(1, 5, 5);
	}
	
	@Test
	public void test3BlockingClient() throws Exception {
		mockForNoChangePing();

		opushServer.start();
		
		final OPClient opClient = buildOpushClient(singleUserFixture.jaures);

		ThreadPoolExecutor threadPoolExecutor = 
				new ThreadPoolExecutor(20, 20, 1,TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());

		Stopwatch stopwatch = new Stopwatch().start();
		
		List<Future<Document>> futures = new ArrayList<Future<Document>>();
		for (int i = 0; i < 4; ++i) {
			 futures.add(queuePingCommand(opClient, threadPoolExecutor));	
		}
		
		for (Future<Document> f: futures) {
			Document response = f.get();
			checkNoChangeResponse(response);
		}
		
		checkExecutionTime(2, 5, stopwatch);
	}
	
	private Future<Document> queuePingCommand(final OPClient opClient,
			ThreadPoolExecutor threadPoolExecutor) {
		return threadPoolExecutor.submit(new Callable<Document>() {
			@Override
			public Document call() throws Exception {
				Document document = buildPingCommand(5);
				return opClient.postXml("Ping", document, "Ping");
			}
		});
	}
	
	@After
	public void shutdown() throws Exception {
		opushServer.stop();
	}

	private void mockDeviceDao(DeviceDao deviceDao, Iterable<OpushUser> users) throws DaoException {
		int i = 0;
		for (OpushUser opushUser: users) {
			expect(deviceDao.getDevice(opushUser.user, 
					opushUser.deviceId, 
					opushUser.userAgent))
					.andReturn(
							new Device(i++, opushUser.deviceType, opushUser.deviceId, new Properties()))
							.anyTimes();
		}
	}

	private void mockCollectionDao(CollectionDao collectionDao, Iterable<OpushUser> users) throws CollectionNotFoundException, DaoException {
		ChangedCollections changed = new ChangedCollections(new Date(), ImmutableSet.<SyncCollection>of());
		expect(collectionDao.getContactChangedCollections(anyObject(Date.class))).andReturn(changed).anyTimes();
		expect(collectionDao.getCalendarChangedCollections(anyObject(Date.class))).andReturn(changed).anyTimes();

		for (OpushUser opushUser: users) {
			String collectionPath = opushUser.user.getLoginAtDomain() + "\\calendar\\" + opushUser.user.getLoginAtDomain();  
			expect(collectionDao.getCollectionPath(anyInt())).andReturn(collectionPath).anyTimes();
		}
	}

	private void mockMonitoredCollectionDao(MonitoredCollectionDao monitoredCollectionDao) {
		monitoredCollectionDao.put(
				anyObject(Credentials.class), 
				anyObject(Device.class), 
				anyObject(Set.class));
		expectLastCall().anyTimes();
	}

	private void mockHeartbeatDao(HearbeatDao heartbeatDao) throws DaoException {
		heartbeatDao.updateLastHearbeat(anyObject(Device.class), anyLong());
		expectLastCall().anyTimes();
	}

	private Document buildPingCommand(int heartbeatInterval)
			throws SAXException, IOException {
		return DOMUtils.parse("<Ping>"
				+ "<HeartbeatInterval>"
				+ heartbeatInterval
				+ "</HeartbeatInterval>"
				+ "<Folders>"
				+ "<Folder>"
				+ "<Id>1432</Id>"
				+ "</Folder>"
				+ "</Folders>"
				+ "</Ping>");
	}

	private OPClient buildOpushClient(OpushUser user) {
		String url = buildServiceUrl();
		return new OPClient(
				user.user.getLoginAtDomain(), 
				user.password, 
				user.deviceId, 
				user.deviceType, 
				user.userAgent, url);
	}

	private String buildServiceUrl() {
		return "http://localhost:" + port + "/";
	}

}