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
package org.obm.push.mail;

import static org.obm.configuration.EmailConfiguration.IMAP_INBOX_NAME;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.fest.assertions.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.obm.opush.env.JUnitGuiceRule;
import org.obm.push.bean.BackendSession;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Email;
import org.obm.push.bean.User;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.icegreen.greenmail.util.GreenMail;

public class ImapStoreAPITest {

	@Rule
	public JUnitGuiceRule guiceBerry = new JUnitGuiceRule(MailEnvModule.class);

	@Inject ImapMailboxService mailboxService;

	@Inject ImapMailBoxUtils mailboxUtils;
	@Inject GreenMail greenMail;
	private String mailbox;
	private String password;
	private BackendSession bs;

	@Before
	public void setUp() {
	    greenMail.start();
	    mailbox = "to@localhost.com";
	    password = "password";
	    greenMail.setUser(mailbox, password);
	    bs = new BackendSession(
				new Credentials(User.Factory.create()
						.createUser(mailbox, mailbox, null), password, null), null, null, null);
	}
	
	@After
	public void tearDown() {
		greenMail.stop();
	}
	
	@Test
	public void testStoreInInboxReadStatusAndUids() throws Exception {
		boolean[] emailsToSendReadStatus = {false, true, false, false, true};
		Date before = new Date();

		for (boolean emailToSendIsRead : emailsToSendReadStatus) {
			InputStream emailData = loadDataFile("plainText.eml");
			mailboxService.storeInInbox(bs, emailData, emailToSendIsRead);
		}
		
		Set<Email> emails = mailboxService.fetchEmails(bs, IMAP_INBOX_NAME, before);
		List<Email> orderedEmails = mailboxUtils.orderEmailByUid(emails);
		Assertions.assertThat(orderedEmails).isNotNull().hasSize(emailsToSendReadStatus.length);
		assertReadStatus(orderedEmails, emailsToSendReadStatus);
		assertUniqueUids(orderedEmails);
	}
	
	@Test
	public void testStoreInInboxInvitation() throws Exception {
		Date before = new Date();
		InputStream emailData = loadDataFile("androidInvit.eml");

		mailboxService.storeInInbox(bs, emailData, true);

		Set<Email> emails = mailboxService.fetchEmails(bs, IMAP_INBOX_NAME, before);
		Assertions.assertThat(emails).isNotNull().hasSize(1);
	}

	@Test
	public void testStoreInInboxNotAnEmail() throws Exception {
		Date before = new Date();
		InputStream notAnEmailData = new ByteArrayInputStream(new byte[]{'t','e','s','t'});

		mailboxService.storeInInbox(bs, notAnEmailData, true);

		Set<Email> emails = mailboxService.fetchEmails(bs, IMAP_INBOX_NAME, before);
		Assertions.assertThat(emails).isNotNull().hasSize(1);
	}

	@Test
	public void testStoreInInboxReadStatusAndUidsWithJM() throws Exception {
		boolean[] emailsToSendReadStatus = {false, true, false, false, true};
		Date before = new Date();

		for (boolean emailToSendIsRead : emailsToSendReadStatus) {
			InputStream emailData = loadDataFile("plainText.eml");
			mailboxService.storeInInboxWithJM(bs, emailData, emailData.available(), emailToSendIsRead);
		}
		
		Set<Email> emails = mailboxService.fetchEmails(bs, IMAP_INBOX_NAME, before);
		List<Email> orderedEmails = mailboxUtils.orderEmailByUid(emails);
		Assertions.assertThat(orderedEmails).isNotNull().hasSize(emailsToSendReadStatus.length);
		assertReadStatus(orderedEmails, emailsToSendReadStatus);
		assertUniqueUids(orderedEmails);
		for (Email email : orderedEmails) {
			InputStream fetchedMailStream = mailboxService.fetchMailStream(bs, IMAP_INBOX_NAME, email.getUid());
			InputStream expectedEmailData = StreamMailTestsUtils.getStreamBeginingByLineBreak(
					loadDataFile("plainText.eml"));
			Assertions.assertThat(fetchedMailStream).hasContentEqualTo(expectedEmailData);
		}
	}

	@Test
	public void testStoreInInboxInvitationWithJM() throws Exception {
		Date before = new Date();
		int emailSize = 3659;
		InputStream emailData = loadDataFile("androidInvit.eml");

		mailboxService.storeInInboxWithJM(bs, emailData, emailSize, true);

		Set<Email> emails = mailboxService.fetchEmails(bs, IMAP_INBOX_NAME, before);
		Assertions.assertThat(emails).isNotNull().hasSize(1);
	}

	@Test
	public void testStoreInInboxNotAnEmailWithJM() throws Exception {
		Date before = new Date();
		InputStream notAnEmailData = new ByteArrayInputStream(new byte[]{'t','e','s','t'});

		mailboxService.storeInInboxWithJM(bs, notAnEmailData, notAnEmailData.available(), true);

		Set<Email> emails = mailboxService.fetchEmails(bs, IMAP_INBOX_NAME, before);
		Assertions.assertThat(emails).isNotNull().hasSize(1);
	}

	@Test(expected=MailException.class)
	public void testStoreInInboxThrowExceptionWhenGivenMessageSizeIsShorter() throws Exception {
		int emailGivenSize = 20;
		InputStream emailStream = getStreamOfFortyChars();

		mailboxService.storeInInboxWithJM(bs, emailStream, emailGivenSize, true);
	}
	
	@Test
	public void testStoreInInboxRollbackWhenGivenMessageSizeIsShorter() throws Exception {
		Date before = new Date();
		int emailGivenSize = 20;
		InputStream emailStream = getStreamOfFortyChars();

		try {
			mailboxService.storeInInboxWithJM(bs, emailStream, emailGivenSize, true);
		} catch (MailException e) {
			// We know that this storing fails
		}
		
		Set<Email> emails = mailboxService.fetchEmails(bs, IMAP_INBOX_NAME, before);
		Assertions.assertThat(emails).isNotNull().hasSize(0);
	}

	@Test
	public void testStoreInInboxThrowExceptionWhenGivenMessageSizeIsLonger() {
		int emailGivenSize = 60;
		InputStream emailStream = getStreamOfFortyChars();
		MailException exceptionGotten = null;
		
		try {
			mailboxService.storeInInboxWithJM(bs, emailStream, emailGivenSize, true);
		} catch (MailException e) {
			exceptionGotten = e;
		}
		MailTestsUtils.assertThatIsJavaSocketTimeoutException(exceptionGotten);
	}
	
	@Test
	public void testStoreInInboxRollbackWhenGivenMessageSizeIsLonger() throws Exception {
		Date before = new Date();
		int emailGivenSize = 60;
		InputStream emailStream = getStreamOfFortyChars();

		try {
			mailboxService.storeInInboxWithJM(bs, emailStream, emailGivenSize, true);
		} catch (MailException e) {
			// We know that this storing fails
		}
		
		Set<Email> emails = mailboxService.fetchEmails(bs, IMAP_INBOX_NAME, before);
		Assertions.assertThat(emails).isNotNull().hasSize(0);
	}

	@Test(expected=MailException.class)
	public void testStoreInInboxThrowExceptionWhenStreamFail() throws Exception {
		InputStream failingEmailStream = getStreamFailing();

		mailboxService.storeInInboxWithJM(bs, failingEmailStream, failingEmailStream.available(), true);
	}

	@Test
	public void testStoreInInboxRollbackWhenStreamFail() throws Exception {
		Date before = new Date();
		InputStream failingEmailStream = getStreamFailing();

		try {
			mailboxService.storeInInboxWithJM(bs, failingEmailStream, failingEmailStream.available(), true);
		} catch (MailException e) {
			// We know that this storing fails
		}

		Set<Email> emails = mailboxService.fetchEmails(bs, IMAP_INBOX_NAME, before);
		Assertions.assertThat(emails).isNotNull().hasSize(0);
	}
	
	private void assertReadStatus(List<Email> emails, boolean[] emailsExpectedStatus) {
		int indexEmailExpectedStatus = 0;
		for (Email storedEmail : emails) {
			boolean expectedRead = emailsExpectedStatus[indexEmailExpectedStatus++];
			Assertions.assertThat(storedEmail.isRead()).isEqualTo(expectedRead);
		}
	}

	private void assertUniqueUids(List<Email> emails) {
		Set<Long> uids = Sets.newHashSet();
		for (Email storedEmail : emails) {
			uids.add(storedEmail.getUid());
		}
		Assertions.assertThat(uids).hasSize(emails.size());
	}

	private InputStream getStreamOfFortyChars() {
		return new ByteArrayInputStream(
				new String("This message is forty characters long...").getBytes());
	}
	
	private InputStream getStreamFailing() {
		return new InputStream() {

			int count = 0;
			int shouldWriteByteCount = 20;
			int willFailAtByteIndex = 10;

			@Override
			public int available() throws IOException {
				return shouldWriteByteCount;
			}
			
			@Override
			public int read() throws IOException {
				
				if (count < shouldWriteByteCount) {
					if (count != willFailAtByteIndex) {
						count++;
						return 'C';
					} else {
						throw new IOException("Stream has failed");
					}
				} else {
					return -1;
				}
			}
		};
	}
	
	protected InputStream loadDataFile(String name) {
		return getClass().getClassLoader().getResourceAsStream("eml/" + name);
	}
}
