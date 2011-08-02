package org.obm.push.store;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.obm.push.bean.BodyPreference;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.MSAddress;
import org.obm.push.bean.MSAttachement;
import org.obm.push.bean.MSAttendee;
import org.obm.push.bean.MSContact;
import org.obm.push.bean.MSEmail;
import org.obm.push.bean.MSEmailBody;
import org.obm.push.bean.MSEvent;
import org.obm.push.bean.MSTask;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.Recurrence;
import org.obm.push.bean.SyncCollection;
import org.obm.push.bean.SyncCollectionChange;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncState;

import com.google.common.collect.ImmutableSet;

public class SerializableTest {

	private ObjectOutputStream objectOutputStream;


	@Before
	public void buildOutputStream() throws IOException {
		objectOutputStream = new ObjectOutputStream(new ByteArrayOutputStream());	
	}

	@Test
	public void testSyncCollectionOptions() throws IOException {
		SyncCollectionOptions obj = new SyncCollectionOptions();
		obj.addBodyPreference(new BodyPreference());
		objectOutputStream.writeObject(obj);
	}
	
	@Test
	public void testSyncCollection() throws IOException {
		SyncCollection syncCollection = new SyncCollection();
		syncCollection.addChange(new SyncCollectionChange("serverId", "clientId", "modType", new MSContact(), PIMDataType.CALENDAR));
		syncCollection.setSyncState(new SyncState("path", "key", new Date()));
		objectOutputStream.writeObject(syncCollection);
	}
	
	@Test
	public void testMSEmail() throws IOException {
		MSEmail msEmail = new MSEmail();
		msEmail.setBody(new MSEmailBody());
		msEmail.setFrom(new MSAddress("toto@titi.com"));
		msEmail.setAttachements(ImmutableSet.of(new MSAttachement()));
		objectOutputStream.writeObject(msEmail);
	}

	@Test
	public void testMSEvent() throws IOException {
		MSEvent msEvent = new MSEvent();
		msEvent.addAttendee(new MSAttendee());
		msEvent.setRecurrence(new Recurrence());
		objectOutputStream.writeObject(msEvent);
	}
	
	@Test
	public void testMSTask() throws IOException {
		MSTask msTask = new MSTask();
		msTask.setRecurrence(new Recurrence());
		objectOutputStream.writeObject(msTask);
	}
	
	@Test
	public void testDevice() throws IOException {
		Device obj = new Device("toto", "toto", new Properties());
		objectOutputStream.writeObject(obj);
	}

	@Test
	public void testCredentials() throws IOException {
		Credentials obj = new Credentials("titi", "tata");
		objectOutputStream.writeObject(obj);
	}
	
}
