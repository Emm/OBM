/* ***** BEGIN LICENSE BLOCK *****
 *
 * %%
 * Copyright (C) 2000 - 2011 Linagora
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK ***** */
package org.obm.push.data;


import static org.obm.push.TestUtils.getXml;

import java.io.IOException;

import javax.xml.parsers.FactoryConfigurationError;

import org.fest.assertions.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.bean.AttendeeStatus;
import org.obm.push.bean.AttendeeType;
import org.obm.push.bean.IApplicationData;
import org.obm.push.bean.MSAttendee;
import org.obm.push.bean.MSEvent;
import org.obm.push.protocol.data.CalendarDecoder;
import org.obm.sync.calendar.EventExtId;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class CalendarDecoderTest {
	
	private CalendarDecoder decoder;
	
	@Before
	public void prepareEventConverter(){
		decoder = new CalendarDecoder();
	}
	
	@Test
	public void testDecodeAttendees() throws Exception{
		StringBuilder builder = new StringBuilder();
		builder.append("<ApplicationData>");
		builder.append("<TimeZone>xP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAFAAMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAFAAIAAAAAAAAAxP///w==</TimeZone>");
		builder.append("<AllDayEvent>0</AllDayEvent>");
		builder.append("<BusyStatus>2</BusyStatus>");
		builder.append("<DTStamp>20110228T144758Z</DTStamp>");
		builder.append("<EndTime>20110306T120000Z</EndTime>");
		builder.append("<Location>Toulouse</Location>");
		builder.append("<MeetingStatus>1</MeetingStatus>");
		builder.append("<Sensitivity>0</Sensitivity>");
		builder.append("<Subject>opush2cccttra</Subject>");
		builder.append("<StartTime>20110306T110000Z</StartTime>");
		builder.append("<UID>d68eb415</UID>");
		builder.append("<Attendees>");
		builder.append("<Attendee>");
		builder.append("<AttendeeName>Poupard Adrien</AttendeeName>");
		builder.append("<AttendeeEmail>adrien@test.tlse.lng</AttendeeEmail>");
		builder.append("<AttendeeStatus>3</AttendeeStatus>");
		builder.append("<AttendeeType>1</AttendeeType>");
		builder.append("</Attendee>");
		builder.append("<Attendee>");
		builder.append("<AttendeeName>Admin instrator</AttendeeName>");
		builder.append("<AttendeeEmail>administrator@test.tlse.lng</AttendeeEmail>");
		builder.append("<AttendeeStatus>5</AttendeeStatus>");
		builder.append("<AttendeeType>2</AttendeeType>");
		builder.append("</Attendee>");
		builder.append("<Attendee>");
		builder.append("<AttendeeName>Sara Connor</AttendeeName>");
		builder.append("<AttendeeEmail>sara@test.tlse.lng</AttendeeEmail>");
		builder.append("<AttendeeStatus>4</AttendeeStatus>");
		builder.append("<AttendeeType>1</AttendeeType>");
		builder.append("</Attendee>");
		builder.append("</Attendees>");
		builder.append("</ApplicationData>");
		Document doc = getXml(builder.toString());
		
		IApplicationData  data = decoder.decode(doc.getDocumentElement());
		Assert.assertTrue(data instanceof MSEvent);
		MSEvent event = (MSEvent)data;
		Assert.assertEquals(3, event.getAttendees().size());
		MSAttendee adrien = null;
		MSAttendee administrator = null;
		MSAttendee sara = null;
		for(MSAttendee att : event.getAttendees()){
			if("adrien@test.tlse.lng".equals(att.getEmail())){
				adrien = att;
			} else if("administrator@test.tlse.lng".equals(att.getEmail())){
				administrator = att;
			} else if("sara@test.tlse.lng".equals(att.getEmail())){
				sara = att;
			}
		}
		
		checkAttendee(adrien, "Poupard Adrien", "adrien@test.tlse.lng", AttendeeStatus.ACCEPT, AttendeeType.REQUIRED);
		checkAttendee(administrator, "Admin instrator", "administrator@test.tlse.lng", AttendeeStatus.NOT_RESPONDED, AttendeeType.OPTIONAL);
		checkAttendee(sara, "Sara Connor", "sara@test.tlse.lng", AttendeeStatus.DECLINE, AttendeeType.REQUIRED);
		
		
	}
	
	private void checkAttendee(MSAttendee att, String name,
			String email, AttendeeStatus status, AttendeeType type) {
		Assert.assertNotNull(att);
		Assert.assertEquals(name, att.getName());
		Assert.assertEquals(email, att.getEmail());
		Assert.assertEquals(status, att.getAttendeeStatus());
		Assert.assertEquals(type, att.getAttendeeType());
	}
	
	@Test
	public void testDecodeUID() throws SAXException, IOException, FactoryConfigurationError {
		String UID = "63666534363435652d343136382d313032662d626535652d303031353137366637393232";
		IApplicationData  data = decode(UID);
		Assertions.assertThat(data).isNotNull().isInstanceOf(MSEvent.class);
		MSEvent event = (MSEvent) data;
		Assertions.assertThat(event.getExtId())
			.isNotNull()
			.isInstanceOf(EventExtId.class)
			.isEqualTo(new EventExtId("cfe4645e-4168-102f-be5e-0015176f7922"));

	}
	
	@Test
	public void testDecodeHexUIDValue() throws SAXException, IOException, FactoryConfigurationError {
		String UID = "930865cb-f04e-447a-ab5a-3dac62695001";
		IApplicationData  data = decode(UID);
		Assertions.assertThat(data).isNotNull().isInstanceOf(MSEvent.class);
		MSEvent event = (MSEvent) data;
		Assertions.assertThat(event.getExtId())
			.isNotNull()
			.isInstanceOf(EventExtId.class)
			.isEqualTo(new EventExtId("930865cb-f04e-447a-ab5a-3dac62695001"));
	}
	
	@Test
	public void testDecodeBadShortHexStringUID() throws SAXException, IOException, FactoryConfigurationError {
		String badUID = "FFFFZ";
		IApplicationData  data = decode(badUID);
		Assertions.assertThat(data).isNotNull().isInstanceOf(MSEvent.class);
		MSEvent event = (MSEvent) data;
		Assertions.assertThat(event.getExtId()).isNull();
	}
	
	@Test
	public void testDecodeBadHexStringUID() throws SAXException, IOException, FactoryConfigurationError {
		String badUID = "84EE2F24CB8D46EB85824CE6754C0E2600000000000000000000000000000000";
		IApplicationData  data = decode(badUID);
		Assertions.assertThat(data).isNotNull().isInstanceOf(MSEvent.class);
		MSEvent event = (MSEvent) data;
		Assertions.assertThat(event.getExtId()).isNull();
	}
	
	@Test
	public void testDecodeBadASCIIStringUID() throws SAXException, IOException, FactoryConfigurationError {
		String badUID = "63666534363435652d343136382dé13032662d626535652d303031353137366637393232";
		IApplicationData  data = decode(badUID);
		Assertions.assertThat(data).isNotNull().isInstanceOf(MSEvent.class);
		MSEvent event = (MSEvent) data;
		Assertions.assertThat(event.getExtId()).isNull();
	}

	private IApplicationData decode(String uid) throws SAXException, IOException, FactoryConfigurationError {
		return decoder.decode( getDocument(uid).getDocumentElement() );
	}
	
	private Document getDocument(String uid) throws SAXException, IOException, FactoryConfigurationError {
		String xml = "<ApplicationData>" 
						+ "<AllDayEvent>0</AllDayEvent>"
						+ "<StartTime>20100217T120000Z</StartTime>"
						+ "<EndTime>20100217T130000Z</EndTime>"
						+ "<DTStamp>20111012T075834Z</DTStamp>"
						+ "<Subject>Déj Confort Inn</Subject>"
						+ "<Sensitivity>2</Sensitivity>"
						+ "<OrganizerEmail>xxx@linagora.com</OrganizerEmail>"
						+ "<UID>" + uid + "</UID>"
						+ "<BusyStatus>2</BusyStatus>"
						+ "<MeetingStatus>1</MeetingStatus>"
						+ "<OrganizerName>Xxx XXX</OrganizerName>"
						+ "</ApplicationData>";
		return getXml(xml);
	}
	
}
