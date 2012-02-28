package fr.aliacom.obm.common.calendar;

import static fr.aliacom.obm.common.calendar.EventNotificationServiceTestTools.after;

import java.util.Date;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Test;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.calendar.Attendee;
import org.obm.sync.calendar.Event;
import org.obm.sync.calendar.ParticipationState;

import fr.aliacom.obm.ToolBox;

public class EventChangeHandlerTest {

	@Test
	public void testNegativeException() {
		Attendee attendee = ToolBox.getFakeAttendee("james.jesus.angleton@cia.gov");
		attendee.setState(ParticipationState.ACCEPTED);

		int previousSequence = 0;
		Date eventDate = after();
		AccessToken token = ToolBox.mockAccessToken();
		Event previousEvent = ToolBox.getFakeDailyRecurrentEvent(eventDate, previousSequence,
				attendee);

		int currentSequence = previousSequence + 1;
		Event currentEvent = ToolBox.getFakeDailyRecurrentEvent(eventDate, currentSequence,
				attendee);
		Date exceptionDate = new DateTime(eventDate).plusMonths(1).toDate();
		currentEvent.addException(exceptionDate);

		Event negativeExceptionEvent = ToolBox.getFakeNegativeExceptionEvent(currentEvent, exceptionDate);

		JMSService jmsService = EasyMock.createMock(JMSService.class);
		jmsService.writeIcsInvitationCancel(token, negativeExceptionEvent);

		EventNotificationService notifService = EasyMock.createMock(EventNotificationService.class);
		notifService.notifyDeletedEvent(negativeExceptionEvent, token);

		EasyMock.replay(token, jmsService, notifService);

		boolean notification = true;

		EventChangeHandler handler = new EventChangeHandler(jmsService, notifService);
		handler.update(previousEvent, currentEvent, notification, token);
	}
}
