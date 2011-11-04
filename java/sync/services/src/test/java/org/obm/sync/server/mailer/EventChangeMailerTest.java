package org.obm.sync.server.mailer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.commons.io.IOUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.hamcrest.core.IsInstanceOf;
import org.jsoup.Jsoup;
import org.junit.Assert;
import org.junit.Test;
import org.junit.internal.matchers.StringContains;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.calendar.Attendee;
import org.obm.sync.calendar.Event;
import org.obm.sync.calendar.EventExtId;
import org.obm.sync.calendar.EventObmId;
import org.obm.sync.calendar.ParticipationState;
import org.obm.sync.server.template.ITemplateLoader;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

import fr.aliacom.obm.ToolBox;
import fr.aliacom.obm.common.MailService;
import fr.aliacom.obm.common.calendar.EventChangeHandlerTestsTools;
import fr.aliacom.obm.services.constant.ConstantService;
import fr.aliacom.obm.utils.Ical4jHelper;
import freemarker.template.Configuration;
import freemarker.template.Template;

@RunWith(Suite.class)
@SuiteClasses({EventChangeMailerTest.AcceptedCreation.class, EventChangeMailerTest.NeedActionCreation.class, 
	EventChangeMailerTest.Cancelation.class, EventChangeMailerTest.NotifyAcceptedUpdateUsers.class, 
	EventChangeMailerTest.NeedActionUpdate.class, EventChangeMailerTest.NotifyAcceptedUpdateUsersCanWriteOnCalendar.class})
public class EventChangeMailerTest {

	private static final TimeZone TIMEZONE = TimeZone.getTimeZone("Europe/Paris");
	
	public abstract static class Common {
		
		
		private ITemplateLoader templateLoader;
		
		public Common(){
			templateLoader = new ITemplateLoader() {
				@Override
				public Template getTemplate(String templateName, Locale locale, TimeZone timezone)
						throws IOException {
					Configuration cfg = new Configuration();
					cfg.setClassForTemplateLoading(getClass(), "template");
					Template template = cfg.getTemplate(templateName, locale);
					template.setTimeZone(TIMEZONE);
					return cfg.getTemplate(templateName, locale);
				}
			};
			
		}
		
		protected ITemplateLoader getStubTemplateLoader(){
			return templateLoader;
		}

		
		protected AccessToken getStubAccessToken(){
			AccessToken at = new AccessToken(1, 1, "unitTest");
			at.setDomain("test.tlse.lng");
			return at;
		}
		
		
		protected MailService defineMailServiceExpectations(
				List<InternetAddress> expectedRecipients,
				Capture<MimeMessage> capturedMessage) throws MessagingException {
			
			MailService mailService = EasyMock.createMock(MailService.class);
			mailService.sendMessage(
					EasyMock.anyObject(Session.class),
					EventChangeHandlerTestsTools.compareCollections(expectedRecipients), 
					EasyMock.capture(capturedMessage));
			EasyMock.expectLastCall();
			EasyMock.replay(mailService);
			return mailService;
		}

		
		protected abstract List<InternetAddress> getExpectedRecipients() throws AddressException;


		protected Attendee createAttendee(String name, String email) {
			Attendee attendee = new Attendee();
			attendee.setEmail(email);
			attendee.setDisplayName(name);
			return attendee;
		}

		protected Date date(int year, int month, int day, int hour, int minute) {
			Calendar calendar = GregorianCalendar.getInstance();
			calendar.setTimeZone(TIMEZONE);
			calendar.set(year, month, day, hour, minute, 0);
			Date refDate = calendar.getTime();
			return refDate;
		}

		protected List<InternetAddress> createAddressList(String addresses) throws AddressException {
			return ImmutableList.copyOf(InternetAddress.parse(addresses));
		}

		public static class InvitationParts {
			public String rawMessage;
			public BodyPart plainText;
			public BodyPart htmlText;
			public BodyPart textCalendar;
			public BodyPart applicationIcs;
		}

		
		private String getRawMessage(MimeMessage actualMessage)
		throws IOException, MessagingException,	UnsupportedEncodingException {

			ByteArrayOutputStream output = new ByteArrayOutputStream();
			actualMessage.writeTo(output);
			String rawMessage = new String(output.toByteArray(), Charsets.UTF_8.displayName());
			return rawMessage;
		}


		protected Event buildTestEvent() {
			Event event = new Event();
			event.setTimeCreate(new Date(1244470973000L));
			event.setTimeUpdate(new Date(1244470995000L));
			event.addAttendee(createAttendee("Ronan LANORE", "rlanore@linagora.com"));
			event.addAttendee(createAttendee("Guillaume ALAUX", "galaux@linagora.com"));
			event.addAttendee(createAttendee("Matthieu BAECHLER", "mbaechler@linagora.com"));
			event.addAttendee(createAttendee("Blandine DESCAMPS", "blandine.descamps@linagora.com"));
			event.setTitle("Sprint planning OBM");
			event.setOwner("raphael");
			event.setOwnerDisplayName("Raphael ROUGERON");
			event.setOwnerEmail("rrougeron@linagora.com");
			event.setDate(date(2010, 10, 8, 11, 00));
			event.setExtId(new EventExtId("f1514f44bf39311568d64072c1fec10f47fe"));
			event.setDuration(2700);
			event.setUid(new EventObmId(1354));
			return event;
		}

		private MimeMessage test() throws MessagingException {
			ConstantService constantService = EasyMock.createMock(ConstantService.class);
			EasyMock.expect(constantService.getObmUIBaseUrl()).andReturn("baseUrl").once();
			EasyMock.expect(constantService.getResourceBundle(Locale.FRENCH)).andReturn(
					ResourceBundle.getBundle("Messages", Locale.FRENCH));
			Capture<MimeMessage> capturedMessage = new Capture<MimeMessage>();
			EasyMock.replay(constantService);
			List<InternetAddress> expectedRecipients = getExpectedRecipients();
			MailService mailService = defineMailServiceExpectations(expectedRecipients, capturedMessage);
			EventChangeMailer eventChangeMailer = new EventChangeMailer(mailService, constantService, getStubTemplateLoader());
			
			Ical4jHelper ical4jHelper = new Ical4jHelper();
			executeProcess(eventChangeMailer, ical4jHelper);

			EasyMock.verify(mailService, constantService);
			return  capturedMessage.getValue();
		}
		
		private void testInvitation() throws UnsupportedEncodingException, IOException, MessagingException {
			MimeMessage mimeMessage = test();
			InvitationParts parts = checkInvitationStructure(mimeMessage);
			checkContent(parts);
		}
		
		private void testNotification() throws UnsupportedEncodingException, IOException, MessagingException {
			MimeMessage mimeMessage = test();
			InvitationParts parts = checkStructure(mimeMessage);
			checkContent(parts);
		}

		protected abstract void checkContent(InvitationParts parts) throws IOException, MessagingException;

		protected abstract InvitationParts checkStructure(MimeMessage mimeMessage) throws UnsupportedEncodingException, IOException, MessagingException;
		
		protected InvitationParts checkInvitationStructure(MimeMessage mimeMessage) throws UnsupportedEncodingException, IOException, MessagingException {
			InvitationParts parts = new InvitationParts();
			parts.rawMessage = getRawMessage(mimeMessage);
			Assert.assertTrue(mimeMessage.getContentType().startsWith("multipart/mixed"));
			Assert.assertThat(mimeMessage.getContent(), IsInstanceOf.instanceOf(Multipart.class));
			Multipart mixed = (Multipart) mimeMessage.getContent();
			Assert.assertEquals(2, mixed.getCount());
			BodyPart firstPart = mixed.getBodyPart(0);
			Assert.assertTrue(firstPart.getContentType().startsWith("multipart/alternative"));
			Assert.assertThat(firstPart.getContent(), IsInstanceOf.instanceOf(Multipart.class));
			Multipart alternative = (Multipart) firstPart.getContent();
			Assert.assertEquals(3, alternative.getCount());
			parts.plainText = alternative.getBodyPart(0);
			Assert.assertTrue(parts.plainText.getContentType().startsWith("text/plain; charset=UTF-8"));
			parts.htmlText = alternative.getBodyPart(1);
			Assert.assertTrue(parts.htmlText.getContentType().startsWith("text/html; charset=UTF-8"));
			parts.textCalendar = alternative.getBodyPart(2);
			parts.applicationIcs = mixed.getBodyPart(1);
			Assert.assertEquals("application/ics; name=meeting.ics", parts.applicationIcs.getContentType());
			return parts;
		}
		
		protected InvitationParts checkNotificationStructure(MimeMessage mimeMessage) throws UnsupportedEncodingException, IOException, MessagingException {
			InvitationParts parts = new InvitationParts();
			parts.rawMessage = getRawMessage(mimeMessage);
			Assert.assertTrue(mimeMessage.getContentType().startsWith("multipart/alternative"));
			Assert.assertThat(mimeMessage.getContent(), IsInstanceOf.instanceOf(Multipart.class));
			Multipart alternative = (Multipart) mimeMessage.getContent();
			Assert.assertEquals(2, alternative.getCount());
			parts.plainText = alternative.getBodyPart(0);
			Assert.assertTrue(parts.plainText.getContentType().startsWith("text/plain; charset=UTF-8"));
			parts.htmlText = alternative.getBodyPart(1);
			Assert.assertTrue(parts.htmlText.getContentType().startsWith("text/html; charset=UTF-8"));
			return parts;
		}

		protected void checkIcs(BodyPart textCalendar) throws IOException, MessagingException {
			Assert.assertThat(textCalendar.getContent(), IsInstanceOf.instanceOf(String.class));
			String text = (String) textCalendar.getContent();
			checkStringContains(text, getExpectedIcsStrings());
		}

		protected abstract String[] getExpectedIcsStrings();

		protected void checkApplicationIcs(BodyPart applicationIcs) throws IOException, MessagingException {
			Assert.assertThat(applicationIcs.getContent(), IsInstanceOf.instanceOf(SharedByteArrayInputStream.class));
			SharedByteArrayInputStream stream = (SharedByteArrayInputStream) applicationIcs.getContent();
			String decodedString = IOUtils.toString(stream, Charsets.US_ASCII.displayName());
			checkStringContains(decodedString, getExpectedIcsStrings());
		}

		protected void checkStringContains(String text, String... expected) {
			for (String s: expected) {
				Assert.assertThat(text, StringContains.containsString(s));
			}
		}

		protected void checkHtmlMessage(BodyPart htmlText) throws IOException, MessagingException {
			Assert.assertThat(htmlText.getContent(), IsInstanceOf.instanceOf(String.class));
			String text = Jsoup.parse((String)htmlText.getContent()).text();
			checkStringContains(text, getExpectedHtmlStrings());
		}

		protected abstract String[] getExpectedHtmlStrings();


		protected void checkPlainMessage(BodyPart plainText) throws IOException, MessagingException {
			Assert.assertThat(plainText.getContent(), IsInstanceOf.instanceOf(String.class));
			String text = (String) plainText.getContent();
			checkStringContains(text, getExpectedPlainStrings());
		}
		
		protected abstract String[] getExpectedPlainStrings();


		protected abstract void executeProcess(EventChangeMailer eventChangeMailer, Ical4jHelper ical4jHelper);
	}

	public static class NeedActionCreation extends Common {

		@Override
		protected Event buildTestEvent() {
			Event event = super.buildTestEvent();
			event.setSequence(5);
			return event;
		}
		
		@Override
		protected void executeProcess(EventChangeMailer eventChangeMailer, Ical4jHelper ical4jHelper) {
			Event event = buildTestEvent();
			String ics  = ical4jHelper.buildIcsInvitationRequest(ToolBox.getDefaultObmUser(), event);
			eventChangeMailer.notifyNeedActionNewUsers(event.getAttendees(), event, Locale.FRENCH, TIMEZONE, ics);
		}
		
		@Test
		public void invitationRequest() throws IOException, MessagingException {
			super.testInvitation();
		}

		@Override
		protected InvitationParts checkStructure(MimeMessage mimeMessage) throws UnsupportedEncodingException, IOException, MessagingException {
			return checkInvitationStructure(mimeMessage);
		}
		
		@Override
		protected void checkContent(InvitationParts parts) throws IOException, MessagingException {
			checkStringContains(parts.rawMessage, 
					"From: Raphael ROUGERON <rrougeron@linagora.com>",
					"To: Ronan LANORE <rlanore@linagora.com>, Guillaume",
					"Subject: =?UTF-8?Q?Nouvel_=C3=A9v=C3=A9nement_de_Raphael_ROUGE?=");
			checkPlainMessage(parts.plainText);
			checkHtmlMessage(parts.htmlText);
			Assert.assertEquals("text/calendar; charset=UTF-8; method=REQUEST;", parts.textCalendar.getContentType());
			checkIcs(parts.textCalendar);
			checkApplicationIcs(parts.applicationIcs);
		}
		
		@Override
		protected String[] getExpectedPlainStrings() {
			return new String[] {
				"NOUVEAU RENDEZ-VOUS",
				"du     : 8 nov. 2010 11:00", 
				"au     : 8 nov. 2010 11:45",
				"sujet  : Sprint planning OBM", 
				"lieu   : ","auteur : Raphael ROUGERON"
			};
		}
		
		@Override
		protected String[] getExpectedHtmlStrings() {
			return new String[] {
				"Invitation à un événement",
				"Du 8 nov. 2010 11:00", 
				"Au 8 nov. 2010 11:45", 
				"Sujet Sprint planning OBM", 
				"Lieu", 
				"Organisateur Raphael ROUGERON"
			};
		}
		
		@Override
		protected String[] getExpectedIcsStrings() {
			return new String[] {
					"BEGIN:VCALENDAR",
					"CALSCALE:GREGORIAN",
					"VERSION:2.0",
					"METHOD:REQUEST",
					"BEGIN:VEVENT",
					"DTSTART:20101108T100000Z",
					"DURATION:PT45M",
					"SUMMARY:Sprint planning OBM",
					"ORGANIZER;CN=Raphael ROUGERON:mailto:rrougeron@linagora.com",
					"UID:f1514f44bf39311568d64072c1fec10f47fe",
					"X-OBM-DOMAIN:test.tlse.lng",
					"CREATED:20090608T142253Z",
					"LAST-MODIFIED:20090608T142315Z",
					"SEQUENCE:5"
			};
		}
		@Override
		protected List<InternetAddress> getExpectedRecipients() throws AddressException {
			return createAddressList(
					"Ronan LANORE <rlanore@linagora.com>, Guillaume ALAUX " +
					"<galaux@linagora.com>, Matthieu BAECHLER <mbaechler@linagora.com>, Blandine " +
					"DESCAMPS <blandine.descamps@linagora.com>");
		}

	}
	
	public static class AcceptedCreation extends Common {

		@Override
		protected void executeProcess(EventChangeMailer eventChangeMailer, Ical4jHelper ical4jHelper) {
			Event event = buildTestEvent();
			eventChangeMailer.notifyAcceptedNewUsers(event.getAttendees(), event, Locale.FRENCH, TIMEZONE);
		}
		
		@Test
		public void invitationRequest() throws IOException, MessagingException {
			super.testNotification();
		}

		@Override
		protected InvitationParts checkStructure(MimeMessage mimeMessage)
				throws UnsupportedEncodingException, IOException,
				MessagingException {
			return checkNotificationStructure(mimeMessage);
		}
		
		@Override
		protected void checkContent(InvitationParts parts) throws IOException, MessagingException {
			checkStringContains(parts.rawMessage, 
					"From: Raphael ROUGERON <rrougeron@linagora.com>",
					"To: Ronan LANORE <rlanore@linagora.com>, Guillaume",
					"Subject: =?UTF-8?Q?Nouvel_=C3=A9v=C3=A9nement_de_Raphael_ROUGE?=");
			checkPlainMessage(parts.plainText);
			checkHtmlMessage(parts.htmlText);
		}
		
		@Override
		protected String[] getExpectedPlainStrings() {
			return new String[] {
				"NOUVEAU RENDEZ-VOUS",
				"du     : 8 nov. 2010 11:00", 
				"au     : 8 nov. 2010 11:45",
				"sujet  : Sprint planning OBM", 
				"lieu   : ","auteur : Raphael ROUGERON",
				"::NB : Si vous êtes utilisateur du connecteur Thunderbird ou de la synchronisation ActiveSync, vous devez synchroniser pour visualiser ce nouveau rendez-vous."
			};
		}
		
		@Override
		protected String[] getExpectedHtmlStrings() {
			return new String[] {
				"Invitation à un événement",
				"Du 8 nov. 2010 11:00", 
				"Au 8 nov. 2010 11:45", 
				"Sujet Sprint planning OBM", 
				"Lieu", 
				"Organisateur Raphael ROUGERON",
			    "Si vous êtes utilisateur du connecteur Thunderbird ou de la synchronisation ActiveSync, vous devez synchroniser pour visualiser ce nouveau rendez-vous."
			};
		}
		
		@Override
		protected String[] getExpectedIcsStrings() {
			return new String[] {	};
		}
		@Override
		protected List<InternetAddress> getExpectedRecipients() throws AddressException {
			return createAddressList(
					"Ronan LANORE <rlanore@linagora.com>, Guillaume ALAUX " +
					"<galaux@linagora.com>, Matthieu BAECHLER <mbaechler@linagora.com>, Blandine " +
					"DESCAMPS <blandine.descamps@linagora.com>");
		}

	}

	public static class NotifyAcceptedUpdateUsersCanWriteOnCalendar extends NotifyAcceptedUpdateUsers {
		
		@Override
		protected void notifyAcceptedUpdateUsers(EventChangeMailer eventChangeMailer, Event before, Event after) {
			eventChangeMailer.notifyAcceptedUpdateUsersCanWriteOnCalendar(before.getAttendees(), 
					before, after, Locale.FRENCH, TIMEZONE);
		}
		
		@Override
		protected InvitationParts checkStructure(MimeMessage mimeMessage) 
				throws UnsupportedEncodingException, IOException, MessagingException {
			return checkNotificationStructure(mimeMessage);
		}
		
		@Override
		protected void checkContent(InvitationParts parts) throws IOException, MessagingException { 
			checkStringContains(parts.rawMessage, 
					"From: Raphael ROUGERON <rrougeron@linagora.com>",
					"To: Ronan LANORE <rlanore@linagora.com>, Guillaume",
					"Subject: =?UTF-8?Q?Mise_=C3=A0_jour_d'un_=C3=A9v=C3=A9nement_par_Raphael");
			checkPlainMessage(parts.plainText);
			checkHtmlMessage(parts.htmlText);
			Assert.assertNull(parts.applicationIcs);
		}
		
	}
	
	public static class NotifyAcceptedUpdateUsers extends Common {

		@Test
		public void invitationUpdate() throws IOException, MessagingException {
			super.testNotification();
		}
		
		protected void notifyAcceptedUpdateUsers(EventChangeMailer eventChangeMailer, Event before, Event after) {
			eventChangeMailer.notifyAcceptedUpdateUsers(before.getAttendees(), before, after, Locale.FRENCH, TIMEZONE, "");
		}
		
		@Override
		protected void executeProcess(EventChangeMailer eventChangeMailer, Ical4jHelper ical4jHelper) {
			Event before = buildTestEvent();
			Event after = before.clone();
			after.setDate(date(2010, 10, 8, 12, 00));
			after.setDuration(3600);
			for (Attendee att: before.getAttendees()) {
				att.setState(ParticipationState.ACCEPTED);
			}
			notifyAcceptedUpdateUsers(eventChangeMailer, before, after);
		}
		
		@Override
		protected String[] getExpectedHtmlStrings() {
			return new String[] {
					"Invitation à un évènement : mise à jour",
					"du 8 nov. 2010 11:00", 
					"au 8 nov. 2010 11:45",
					"Du 8 nov. 2010 12:00", 
					"Au 8 nov. 2010 13:00",
					"Sujet Sprint planning OBM", 
					"Lieu", 
					"Organisateur Raphael ROUGERON",
					"Si vous êtes utilisateur du connecteur Thunderbird ou de la synchronisation ActiveSync, vous devez synchroniser pour visualiser ces modifications."
			};
		}
		
		@Override
		protected String[] getExpectedIcsStrings() {
			return new String[] {};
		}
		
		@Override
		protected String[] getExpectedPlainStrings() {
			return new String[] {
					"RENDEZ-VOUS MODIFIÉ !",
					"du 8 nov. 2010 11:00", 
					"au 8 nov. 2010 11:45",
					"Le rendez-vous Sprint planning OBM", 
					"lieu : ",
					"::NB : Si vous êtes utilisateur du connecteur Thunderbird ou de la synchronisation ActiveSync, vous devez synchroniser pour visualiser ces modifications."
				};
		}
		
		@Override
		protected List<InternetAddress> getExpectedRecipients()
				throws AddressException {
			return createAddressList(
					"Ronan LANORE <rlanore@linagora.com>, Guillaume ALAUX " +
					"<galaux@linagora.com>, Matthieu BAECHLER <mbaechler@linagora.com>, Blandine " +
					"DESCAMPS <blandine.descamps@linagora.com>");
		}
		
		@Override
		protected InvitationParts checkStructure(MimeMessage mimeMessage)
				throws UnsupportedEncodingException, IOException,
				MessagingException {
			return checkInvitationStructure(mimeMessage);
		}
		
		@Override
		protected void checkContent(InvitationParts parts) throws IOException,
				MessagingException {
			checkStringContains(parts.rawMessage, 
					"From: Raphael ROUGERON <rrougeron@linagora.com>",
					"To: Ronan LANORE <rlanore@linagora.com>, Guillaume",
					"Subject: =?UTF-8?Q?Mise_=C3=A0_jour_d'un_=C3=A9v=C3=A9nement_par_Raphael");

			checkPlainMessage(parts.plainText);
			checkHtmlMessage(parts.htmlText);
		}
	}
	
	public static class NeedActionUpdate extends Common {

		@Test
		public void invitationUpdate() throws IOException, MessagingException {
			super.testInvitation();
		}
		
		@Override
		protected void executeProcess(EventChangeMailer eventChangeMailer, Ical4jHelper ical4jHelper) {
			Event before = buildTestEvent();
			Event after = before.clone();
			after.setDate(date(2010, 10, 8, 12, 00));
			after.setDuration(3600);
			for (Attendee att : before.getAttendees()) {
				att.setState(ParticipationState.NEEDSACTION);
			}
			after.setSequence(4);
			String ics = ical4jHelper.buildIcsInvitationRequest(ToolBox.getDefaultObmUser(), after);			
			eventChangeMailer.notifyNeedActionUpdateUsers(before.getAttendees(), before, after, Locale.FRENCH, TIMEZONE, ics);
		}
		
		@Override
		protected String[] getExpectedHtmlStrings() {
			return new String[] {
					"Invitation à un évènement : mise à jour",
					"du 8 nov. 2010 11:00", 
					"au 8 nov. 2010 11:45",
					"Du 8 nov. 2010 12:00", 
					"Au 8 nov. 2010 13:00",
					"Sujet Sprint planning OBM", 
					"Lieu", 
					"Organisateur Raphael ROUGERON"
			};
		}
		
		@Override
		protected String[] getExpectedIcsStrings() {
			return new String[] {
					"BEGIN:VCALENDAR",
					"CALSCALE:GREGORIAN",
					"VERSION:2.0",
					"METHOD:REQUEST",
					"BEGIN:VEVENT",
					"DTSTART:20101108T110000Z",
					"DURATION:PT1H",
					"SUMMARY:Sprint planning OBM",
					"ORGANIZER;CN=Raphael ROUGERON:mailto:rrougeron@linagora.com",
					"UID:f1514f44bf39311568d64072c1fec10f47fe",
					"X-OBM-DOMAIN:test.tlse.lng",
					"CREATED:20090608T142253Z",
					"LAST-MODIFIED:20090608T142315Z",
					"SEQUENCE:4"
				};
		}
		
		@Override
		protected String[] getExpectedPlainStrings() {
			return new String[] {
					"RENDEZ-VOUS MODIFIÉ !",
					"du 8 nov. 2010 11:00", 
					"au 8 nov. 2010 11:45",
					"Le rendez-vous Sprint planning OBM", 
					"lieu : "
				};
		}
		
		@Override
		protected List<InternetAddress> getExpectedRecipients()
				throws AddressException {
			return createAddressList(
					"Ronan LANORE <rlanore@linagora.com>, Guillaume ALAUX " +
					"<galaux@linagora.com>, Matthieu BAECHLER <mbaechler@linagora.com>, Blandine " +
					"DESCAMPS <blandine.descamps@linagora.com>");
		}
		
		@Override
		protected InvitationParts checkStructure(MimeMessage mimeMessage)
				throws UnsupportedEncodingException, IOException,
				MessagingException {
			return checkInvitationStructure(mimeMessage);
		}
		
		@Override
		protected void checkContent(InvitationParts parts) throws IOException,
				MessagingException {
			checkStringContains(parts.rawMessage, 
					"From: Raphael ROUGERON <rrougeron@linagora.com>",
					"To: Ronan LANORE <rlanore@linagora.com>, Guillaume",
					"Subject: =?UTF-8?Q?Mise_=C3=A0_jour_d'un_=C3=A9v=C3=A9nement_par_Raphael");

			checkPlainMessage(parts.plainText);
			checkHtmlMessage(parts.htmlText);
			Assert.assertEquals("text/calendar; charset=UTF-8; method=REQUEST;", parts.textCalendar.getContentType());
			checkIcs(parts.textCalendar);
			checkApplicationIcs(parts.applicationIcs);
		}

	}
	
	public static class Cancelation extends Common {

		@Test
		public void invitationCancel() throws IOException, MessagingException {
			super.testInvitation();
		}

		@Override
		protected Event buildTestEvent() {
			Event event = super.buildTestEvent();
			event.setSequence(2);
			return event;
		}
		
		@Override
		protected void executeProcess(EventChangeMailer eventChangeMailer, Ical4jHelper ical4jHelper) {
			Event event = buildTestEvent();
			String ics = ical4jHelper.buildIcsInvitationCancel(ToolBox.getDefaultObmUser(), event);
			eventChangeMailer.notifyRemovedUsers(event.getAttendees(), event, Locale.FRENCH, TIMEZONE, ics);
		}
		
		@Override
		protected String[] getExpectedHtmlStrings() {
			return new String[] {
					"Annulation d'un événement",
					"Du 8 nov. 2010 11:00", 
					"Au 8 nov. 2010 11:45", 
					"Sujet Sprint planning OBM", 
					"Lieu", 
					"Organisateur Raphael ROUGERON"
			};
		}
		
		@Override
		protected String[] getExpectedIcsStrings() {
			return new String[] {
					"BEGIN:VCALENDAR",
					"CALSCALE:GREGORIAN",
					"VERSION:2.0",
					"METHOD:CANCEL",
					"BEGIN:VEVENT",
					"DTSTART:20101108T100000Z",
					"SUMMARY:Sprint planning OBM",
					"ORGANIZER;CN=Raphael ROUGERON:mailto:rrougeron@linagora.com",
					"UID:f1514f44bf39311568d64072c1fec10f47fe",
					"X-OBM-DOMAIN:test.tlse.lng",
					"CREATED:20090608T142253Z",
					"LAST-MODIFIED:20090608T142315Z",
					"SEQUENCE:2"
				};
		}
		
		@Override
		protected String[] getExpectedPlainStrings() {
			return new String[] {
					"RENDEZ-VOUS ANNULÉ",
					"du     : 8 nov. 2010 11:00", 
					"au     : 8 nov. 2010 11:45",
					"sujet  : Sprint planning OBM", 
					"lieu   : ",
					"auteur : Raphael ROUGERON"
				};
		}
		
		@Override
		protected List<InternetAddress> getExpectedRecipients()
				throws AddressException {
			return createAddressList(
					"Ronan LANORE <rlanore@linagora.com>, Guillaume ALAUX " +
					"<galaux@linagora.com>, Matthieu BAECHLER <mbaechler@linagora.com>, Blandine " +
					"DESCAMPS <blandine.descamps@linagora.com>");
		}
		
		@Override
		protected InvitationParts checkStructure(MimeMessage mimeMessage)
				throws UnsupportedEncodingException, IOException,
				MessagingException {
			return checkInvitationStructure(mimeMessage);
		}
		
		@Override
		protected void checkContent(InvitationParts parts) throws IOException,
				MessagingException {
			checkStringContains(parts.rawMessage, 
					"From: Raphael ROUGERON <rrougeron@linagora.com>",
					"To: Ronan LANORE <rlanore@linagora.com>, Guillaume",
					"Subject: =?UTF-8?Q?Ev=C3=A9nement_annul=C3=A9_par_Raphael");

			checkPlainMessage(parts.plainText);
			checkHtmlMessage(parts.htmlText);
			Assert.assertEquals("text/calendar; charset=UTF-8; method=CANCEL;", parts.textCalendar.getContentType());
			checkIcs(parts.textCalendar);
			checkApplicationIcs(parts.applicationIcs);
		}

	}

}
