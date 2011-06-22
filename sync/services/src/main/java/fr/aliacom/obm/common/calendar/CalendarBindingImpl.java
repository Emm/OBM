/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (c) 1997-2008 Aliasource - Groupe LINAGORA
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 2 of the
 *  License, (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 * 
 *  http://www.obm.org/                                              
 * 
 * ***** END LICENSE BLOCK ***** */
package fr.aliacom.obm.common.calendar;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import net.fortuna.ical4j.data.ParserException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.auth.AuthFault;
import org.obm.sync.auth.EventAlreadyExistException;
import org.obm.sync.auth.ServerFault;
import org.obm.sync.base.Category;
import org.obm.sync.base.KeyList;
import org.obm.sync.calendar.Attendee;
import org.obm.sync.calendar.CalendarInfo;
import org.obm.sync.calendar.Event;
import org.obm.sync.calendar.EventParticipationState;
import org.obm.sync.calendar.EventTimeUpdate;
import org.obm.sync.calendar.EventType;
import org.obm.sync.calendar.FreeBusy;
import org.obm.sync.calendar.FreeBusyRequest;
import org.obm.sync.calendar.ParticipationState;
import org.obm.sync.calendar.SyncRange;
import org.obm.sync.items.EventChanges;
import org.obm.sync.items.ParticipationChanges;
import org.obm.sync.server.transactional.Transactional;
import org.obm.sync.services.ICalendar;
import org.obm.sync.services.ImportICalendarException;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import fr.aliacom.obm.common.FindException;
import fr.aliacom.obm.common.domain.DomainService;
import fr.aliacom.obm.common.domain.ObmDomain;
import fr.aliacom.obm.common.user.ObmUser;
import fr.aliacom.obm.common.user.UserService;
import fr.aliacom.obm.utils.Helper;
import fr.aliacom.obm.utils.Ical4jHelper;
import fr.aliacom.obm.utils.LogUtils;

public class CalendarBindingImpl implements ICalendar {

	private static final Log logger = LogFactory.getLog(CalendarBindingImpl.class);

	private EventType type;
	
	private final CalendarDao calendarDao;
	private final CategoryDao categoryDao;
	
	private final DomainService domainService;
	private final UserService userService;
	private final EventChangeHandler eventChangeHandler;
	
	private final Helper helper;
	private final Ical4jHelper ical4jHelper;
	

	private CalendarInfo makeOwnCalendarInfo(ObmUser user) {
		CalendarInfo myself = new CalendarInfo();
		myself.setMail(helper.constructEmailFromList(user.getEmail(), user
				.getDomain().getName()));
		myself.setUid(user.getLogin());
		myself.setFirstname(user.getFirstName());
		myself.setLastname(user.getLastName());
		myself.setRead(true);
		myself.setWrite(true);
		return myself;
	}
	
	@Inject
	protected CalendarBindingImpl(EventChangeHandler eventChangeHandler,
			DomainService domainService, UserService userService,
			CalendarDao calendarDao,
			CategoryDao categoryDao, Helper helper, 
			Ical4jHelper ical4jHelper) {
		this.eventChangeHandler = eventChangeHandler;
		this.domainService = domainService;
		this.userService = userService;
		this.calendarDao = calendarDao;
		this.categoryDao = categoryDao;
		this.helper = helper;
		this.ical4jHelper = ical4jHelper;
	}

	@Override
	@Transactional
	public CalendarInfo[] listCalendars(AccessToken token) throws ServerFault,
			AuthFault {
		try {
			Collection<CalendarInfo> calendarInfos = getRights(token);
			CalendarInfo[] ret = calendarInfos.toArray(new CalendarInfo[0]);
			logger.info(LogUtils.prefix(token) + "Returning " + ret.length
					+ " calendar infos.");
			return ret;
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}
	
	@Override
	@Transactional
	public CalendarInfo[] getCalendarMetadata(AccessToken token, String[] calendars) throws ServerFault,
			AuthFault {
		try {
			ObmUser user = userService.getUserFromAccessToken(token);

			// Since multidomain emails won't have the @domain part after them in the database,
			// add the stripped version of the calendar emails (minus the @domain part) to the query
			
			// Create a new list using Arrays.asList(calendars), since asList returns an unmodifiable list
			List<String> calendarEmails = new ArrayList<String>();
			boolean hasUserEmail = false;
			String userEmail = user.getEmail();
			for (String calendarEmail : calendars) {
				// We'll add the user manually later
				if (calendarEmail.equals(userEmail)) {
					hasUserEmail = true;
					continue;
				}
				int atPosition = calendarEmail.indexOf('@');
				if (atPosition > 0) {
					String strippedCalendarEmail = calendarEmail.substring(0, atPosition);
					calendarEmails.add(calendarEmail);
					calendarEmails.add(strippedCalendarEmail);
				}
				else {
					logger.warn(LogUtils.prefix(token) + "Got an invalid email address: " + calendarEmail);
				}
			}


			Collection<CalendarInfo> calendarInfos;
			if (calendarEmails.size() > 0) {
				calendarInfos = calendarDao.getCalendarMetadata(user, calendarEmails);
            }
			else {
				calendarInfos = new HashSet<CalendarInfo>();
            }

			if (hasUserEmail) {
				// Add the calendar of the current user if needed, since the user's permissions over her own calendars
				// will not be listed in the database
				CalendarInfo myself = makeOwnCalendarInfo(user);
				calendarInfos.add(myself);
			}
			CalendarInfo[] ret = calendarInfos.toArray(new CalendarInfo[0]);
			logger.info(LogUtils.prefix(token) + "Returning " + ret.length
					+ " calendar infos.");
			return ret;
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	private Collection<CalendarInfo> getRights(AccessToken t) throws FindException {
		Collection<CalendarInfo> rights = t.getCalendarRights();
		if (rights == null) {
			rights = listCalendarsImpl(t);
			t.setCalendarRights(rights);
		}
		return rights;
	}

	private Collection<CalendarInfo> listCalendarsImpl(AccessToken token)
			throws FindException {
		ObmUser user = userService.getUserFromAccessToken(token);
		Collection<CalendarInfo> calendarInfos = calendarDao.listCalendars(user);
		CalendarInfo myself = makeOwnCalendarInfo(user);
		calendarInfos.add(myself);
		return calendarInfos;
	}

	@Override
	@Transactional
	public Event removeEvent(AccessToken token, String calendar, String eventId, int sequence, boolean notification)
			throws AuthFault, ServerFault {
		try {
			int uid = Integer.valueOf(eventId);
			Event ev = calendarDao.findEvent(token, uid);

			if (ev == null) {
				logger.info(LogUtils.prefix(token) + "event with id : "
						+ eventId + "not found");
				return null;
			}

			ObmUser calendarUser = userService.getUserFromCalendar(calendar, token.getDomain());
			ObmUser owner = userService.getUserFromLogin(ev.getOwner(), token.getDomain());
			if (owner != null) {
				if (helper.canWriteOnCalendar(token, calendar)) {
					if (owner.getEmail().equals(calendarUser.getEmail())) {
						return cancelEvent(token, calendar, notification, uid, ev);
					} else {
						changeParticipationStateInternal(token, calendar, ev.getExtId(), ParticipationState.DECLINED, sequence, notification);
						return calendarDao.findEvent(token, uid);
					}
				}
				
				logger.info(LogUtils.prefix(token) + "remove not allowed of " + ev.getTitle());
				return ev;
			} else {
				logger.info(LogUtils.prefix(token)
						+ "try to remove an event without owner "
						+ ev.getTitle());
			}

		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e);
		}
		return null;
	}

	private Event cancelEvent(AccessToken token, String calendar,
			boolean notification, int uid, Event ev) throws SQLException,
			FindException {
		Event removed = calendarDao.removeEvent(token, uid, ev.getType(), ev.getSequence() + 1);
		logger.info(LogUtils.prefix(token) + "Calendar : event[" + uid + "] removed");
		notifyOnRemoveEvent(token, calendar, removed, notification);
		return removed;
	}

	private Event cancelEventByExtId(AccessToken token, ObmUser calendar, Event event, boolean notification) throws SQLException, FindException {
		String extId = event.getExtId();
		Event removed = calendarDao.removeEventByExtId(token, calendar, extId, event.getSequence() + 1);
		logger.info(LogUtils.prefix(token) + "Calendar : event[" + extId + "] removed");
		notifyOnRemoveEvent(token, calendar.getEmail(), removed, notification);
		return removed;
	}

	
	private void notifyOnRemoveEvent(AccessToken token, String calendar, Event ev, boolean notification) throws FindException {
		if (ev.isInternalEvent()) {
			ObmUser user = userService.getUserFromAccessToken(token);
			eventChangeHandler.delete(user, ev, notification);
		} else {
			notifyOrganizerForExternalEvent(token, calendar, ev, ParticipationState.DECLINED, notification);
		}
	}

	@Override
	@Transactional
	public Event removeEventByExtId(AccessToken token, String calendar,
			String extId, int sequence, boolean notification) throws AuthFault, ServerFault {
		try {
			ObmUser calendarUser = userService.getUserFromCalendar(calendar, token.getDomain());
			final Event ev = calendarDao.findEventByExtId(token, calendarUser, extId);

			if (ev == null) {
				logger.info(LogUtils.prefix(token) + "Calendar : event[" + extId + "] not removed, it doesn't exist");
				return ev;
			} else {

				if (!helper.canWriteOnCalendar(token, calendar)) {
					logger.info(LogUtils.prefix(token) + "remove not allowed of " + ev.getTitle());
					return ev;
				}

				ObmUser owner = userService.getUserFromLogin(ev.getOwner(), token.getDomain());
				if (owner == null) {
					logger.info(LogUtils.prefix(token) + "error, trying to remove an event without any owner : " + ev.getTitle());
					return ev;
				}
				
				if (owner.getEmail().equals(calendarUser.getEmail())) {
					return cancelEventByExtId(token, calendarUser, ev, notification);
				} else {
					changeParticipationStateInternal(token, calendar, ev.getExtId(), ParticipationState.DECLINED, sequence, notification);
					return calendarDao.findEventByExtId(token, calendarUser, extId);
				}
			}
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public Event modifyEvent(AccessToken token, String calendar, Event event, boolean updateAttendees, boolean notification) 
		throws AuthFault, ServerFault {

		if (event == null) {
			logger.warn(LogUtils.prefix(token) + "Modify on NULL event: doing nothing");
			return null;
		}
		
		try {
			
			final ObmUser calendarUser = userService.getUserFromCalendar(calendar, token.getDomain());
			final Event before = loadCurrentEvent(token, calendarUser, event);

			if (before == null) {
				logger.warn(LogUtils.prefix(token) + "Event[uid:"+ event.getUid() + "extId:" + event.getExtId() +
						"] doesn't exist in database: : doing nothing");
				return null;
			}
			
			if (before.getOwner() != null && !helper.canWriteOnCalendar(token, before.getOwner())) {
				logger.info(LogUtils.prefix(token) + "Calendar : "
						+ token.getUser() + " cannot modify event["
						+ before.getTitle() + "] because not owner"
						+ " or no write right on owner " + before.getOwner()+". ParticipationState will be updated.");
				return before;
				
			} else {
				
				if (before.isInternalEvent()) {
					return modifyInternalEvent(token, calendar, before, event, updateAttendees, notification);
				} else {
					return modifyExternalEvent(token, calendar, event, updateAttendees, notification);
				}
			}

		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e);
		}

	}
	
	private Event loadCurrentEvent(AccessToken token, ObmUser calendarUser, Event event) {
		if (Strings.isNullOrEmpty(event.getUid())) {
			final Event currentEvent = calendarDao.findEventByExtId(token, calendarUser, event.getExtId());
			if (currentEvent != null) {
				event.setUid(currentEvent.getUid());
			}
			return currentEvent;
		} else {
			int uid = Integer.valueOf(event.getUid());
			return calendarDao.findEvent(token, uid);
		}
	}

	private Event modifyInternalEvent(AccessToken token, String calendar, Event before,  Event event,
			boolean updateAttendees, boolean notification) throws ServerFault {
		try{
			prepareParticipationStateForNewEventAttendees(token, before, event);
			
	     final Event after = calendarDao.modifyEventForcingSequence(
	    		 token, calendar, event, updateAttendees, before.getSequence() + 1, true);
	     
			if (after != null) {
				logger.info(LogUtils.prefix(token) + "Calendar : internal event[" + after.getTitle() + "] modified");
			}
			
			ObmUser user = userService.getUserFromAccessToken(token);
			eventChangeHandler.update(user, before, after, notification);
			
			return after;
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	private Event modifyExternalEvent(AccessToken token, String calendar, 
			Event event, boolean updateAttendees, boolean notification) throws ServerFault {
		try {
			Attendee attendee = calendarOwnerAsAttendee(token, calendar, event);
			if (isEventDeclinedForCalendarOwner(attendee)) {
				ObmUser calendarOwner = userService.getUserFromCalendar(calendar, token.getDomain());
				calendarDao.removeEventByExtId(token, calendarOwner, event.getExtId(), event.getSequence());
				notifyOrganizerForExternalEvent(token, calendar, event, notification);
				logger.info(LogUtils.prefix(token) + "Calendar : External event[" + event.getTitle() + 
						"] removed, calendar owner won't attende to it");
				return event;
			} else {
				Event after = calendarDao.modifyEvent(token,  calendar, event, updateAttendees, false);
				if (after != null) {
					logger.info(LogUtils.prefix(token) + "Calendar : External event[" + after.getTitle() + "] modified");
				}
				notifyOrganizerForExternalEvent(token, calendar, after, notification);
				return after;
			}
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public String createEvent(AccessToken token, String calendar, Event event, boolean notification)
			throws AuthFault, ServerFault {

		try {
			if (event == null) {
				logger.warn(LogUtils.prefix(token)
						+ "creating NULL event, returning fake id 0");
				return "0";
			}
			if (!Strings.isNullOrEmpty(event.getUid())) {
				logger.error(LogUtils.prefix(token)
						+ "event creation with an event coming from OBM");
				throw new ServerFault(
						"event creation with an event coming from OBM");
			}

			if (isEventExists(token, calendar, event)) {
				final String message = String
				.format("Calendar : duplicate with same extId found for event [%s, %s, %d, %s]",
						event.getTitle(), event.getDate()
						.toString(), event.getDuration(),
						event.getExtId());
				logger.info(LogUtils.prefix(token) + message);
				throw new EventAlreadyExistException(message);
			}

			if (!helper.canWriteOnCalendar(token, calendar)) {
				String message = "[" + token.getUser() + "] Calendar : "
						+ token.getUser() + " cannot create event on "
						+ calendar + "calendar : no write right";
				logger.info(LogUtils.prefix(token) + message);
				throw new ServerFault(message);
			}
			Event ev = null;
			if (event.isInternalEvent()) {
				ev = createInternalEvent(token, calendar, event, notification);
			} else {
				ev = createExternalEvent(token, calendar, event, notification);
			}
			return (String.valueOf(ev.getDatabaseId()));
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	private Event createExternalEvent(AccessToken token, String calendar, Event event, boolean notification) throws ServerFault {
		try {
			Attendee attendee = calendarOwnerAsAttendee(token, calendar, event);
			if (attendee == null) {
				String message = "Calendar : external event["+ event.getTitle() + "] doesn't involve calendar owner, ignoring creation";
				logger.info(LogUtils.prefix(token) + message);
				throw new ServerFault(message);
			}
			
			if (isEventDeclinedForCalendarOwner(attendee)) {
				logger.info(LogUtils.prefix(token) + "Calendar : external event["+ event.getTitle() + "] refused, mark event as deleted");
				calendarDao.removeEvent(token, event, event.getType(), event.getSequence());
				notifyOrganizerForExternalEvent(token, calendar, event, notification);
				return event;
			} else {
				Event ev = calendarDao.createEvent(token, calendar, event, false);
				logger.info(LogUtils.prefix(token) + "Calendar : external event["+ ev.getTitle() + "] created");
				notifyOrganizerForExternalEvent(token, calendar, ev, notification);
				return ev;
			}
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}
	
	private Attendee calendarOwnerAsAttendee(AccessToken token, String calendar, Event event) 
		throws FindException {
		ObmUser calendarOwner = userService.getUserFromCalendar(calendar, token.getDomain());
		Attendee userAsAttendee = event.findAttendeeForUser(calendarOwner.getEmail());
		return userAsAttendee;
	}

	private boolean isEventDeclinedForCalendarOwner(Attendee userAsAttendee) {
		return userAsAttendee != null && userAsAttendee.getState() == ParticipationState.DECLINED;
	}

	private void notifyOrganizerForExternalEvent(AccessToken token,
			String calendar, Event ev, ParticipationState state, boolean notification) throws FindException {
		logger.info(LogUtils.prefix(token) + 
				"Calendar : sending participation notification to organizer of event ["+ ev.getTitle() + "]");
		ObmUser calendarOwner = userService.getUserFromCalendar(calendar, token.getDomain());
		eventChangeHandler.updateParticipationState(ev, calendarOwner, state, notification);
	}

	
	private void notifyOrganizerForExternalEvent(AccessToken token, String calendar, Event ev,
			boolean notification) throws FindException {
		Attendee calendarOwnerAsAttendee = calendarOwnerAsAttendee(token, calendar, ev);
		notifyOrganizerForExternalEvent(token, calendar, ev, calendarOwnerAsAttendee.getState(), notification);
	}

	private Event createInternalEvent(AccessToken token, String calendar, Event event, boolean notification) throws ServerFault {
		try{
			changePartipationStateOnWritableCalendar(token, event);
			Event ev = calendarDao.createEvent(token, calendar, event, true);
			ev = calendarDao.findEvent(token, ev.getDatabaseId());
			ObmUser user = userService.getUserFromAccessToken(token);
			eventChangeHandler.create(user, ev, notification);
			logger.info(LogUtils.prefix(token) + "Calendar : internal event["
				+ ev.getTitle() + "] created");
			return ev;
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}
	
	private void changePartipationStateOnWritableCalendar(AccessToken token, Event event){
		for(Attendee att : event.getAttendees()){
			if(ParticipationState.NEEDSACTION.equals(att.getState()) && !StringUtils.isEmpty(att.getEmail())) {
				try {
					acceptePartipationStateIfCanWriteOnCalendar(token, att);
				} catch (Exception e) {
					logger.error("Error while checks right on calendar: "+att.getEmail(), e);
				}
			}
		}
	}
	
	private void prepareParticipationStateForNewEventAttendees(final AccessToken token,  final Event before, final Event event) {
		for (final Attendee currentAtt : event.getAttendees()) {
			for (final Attendee beforeAtt: before.getAttendees()) {
				if (currentAtt.equals(beforeAtt)) {
					prepareParticipationStateForAttendee(token, currentAtt);
					break;
				}	
			}
		}
	}
	
	private void prepareParticipationStateForAttendee(final AccessToken token, final Attendee att) {
		try {
			if (helper.canWriteOnCalendar(token,  att.getEmail())) {
				att.setState(ParticipationState.ACCEPTED);
			} else {
				att.setState(ParticipationState.NEEDSACTION);
			}
		} catch (Exception e) {
			logger.error("Error while checks right on calendar: "+ att.getEmail(), e);
		}
	}
	
	private void acceptePartipationStateIfCanWriteOnCalendar(AccessToken token, Attendee att){
		try {
			if (helper.canWriteOnCalendar(token,  att.getEmail())) {
				att.setState(ParticipationState.ACCEPTED);
			}
		} catch (Exception e) {
			logger.error("Error while checks right on calendar: "+att.getEmail(), e);
		}
	}
	
	@Override
	@Transactional
	public EventChanges getSync(AccessToken token, String calendar,
			Date lastSync) throws AuthFault, ServerFault {
		return getSync(token, calendar, lastSync, null, false);
	}
	
	@Override
	@Transactional
	public EventChanges getSyncInRange(AccessToken token, String calendar,
			Date lastSync, SyncRange syncRange) throws AuthFault, ServerFault {
		return getSync(token, calendar, lastSync, syncRange, false);
	}

	@Override
	@Transactional
	public EventChanges getSyncWithSortedChanges(AccessToken token,
			String calendar, Date lastSync) throws AuthFault, ServerFault {

		EventChanges changes = getSync(token, calendar, lastSync, null, false);
		
		//sort between update and participation update based on event timestamp
		sortUpdatedEvents(changes, lastSync);
		
		return changes;
	}
	
	private void sortUpdatedEvents(EventChanges changes, Date lastSync) {
		List<Event> updated = new ArrayList<Event>();
		List<Event> participationChanged = new ArrayList<Event>();
		
		for (Event event: changes.getUpdated()) {
			if (event.modifiedSince(lastSync) || event.exceptionModifiedSince(lastSync)) {
				updated.add(event);
			} else {
				//means that only participation changed
				participationChanged.add(event);
			}
			
		}
		changes.setParticipationUpdated(eventsToParticipationUpdateArray(participationChanged));
		changes.setUpdated(updated.toArray(new Event[0]));
	}

	private ParticipationChanges[] eventsToParticipationUpdateArray(List<Event> participationChanged) {
		return Lists.transform(participationChanged, new Function<Event, ParticipationChanges>() {
			@Override
			public ParticipationChanges apply(Event event) {
				ParticipationChanges participationChanges = new ParticipationChanges(); 
				participationChanges.setAttendees(event.getAttendees());
				participationChanges.setEventExtId(event.getExtId());
				participationChanges.setEventId(event.getDatabaseId());
				return participationChanges;
			}
		}).toArray(new ParticipationChanges[0]);
	}

	
	@Override
	@Transactional
	public EventChanges getSyncEventDate(AccessToken token, String calendar,
			Date lastSync) throws AuthFault, ServerFault {
		return getSync(token, calendar, lastSync, null, true);
	}

	private EventChanges getSync(AccessToken token, String calendar,
			Date lastSync, SyncRange syncRange, boolean onEventDate) throws ServerFault {

		logger.info(LogUtils.prefix(token) + "Calendar : getSync(" + calendar
				+ ", " + lastSync + ")");

		ObmUser calendarUser = null;
		try {
			calendarUser = userService.getUserFromCalendar(calendar, token.getDomain());
		} catch (FindException e) {
			throw new ServerFault(e.getMessage());
		}

		if (!helper.canReadCalendar(token, calendar)) {
			logger.error(LogUtils.prefix(token) + "user " + token.getUser()
					+ " tried to sync calendar " + calendar
					+ " => permission denied");
			throw new ServerFault("Read permission denied for "
					+ token.getUser() + " on " + calendar);
		}

		try {
			EventChanges ret = calendarDao.getSync(token, calendarUser,
					lastSync, syncRange, type, onEventDate);
			logger.info(LogUtils.prefix(token) + "Calendar : getSync("
					+ calendar + ") => " + ret.getUpdated().length + " upd, "
					+ ret.getRemoved().length + " rmed.");
			return ret;
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public Event getEventFromId(AccessToken token, String calendar,
			String eventId) throws AuthFault, ServerFault {
		try {
			int uid = Integer.valueOf(eventId);
			Event evt = calendarDao.findEvent(token, uid);
			if(evt == null){
				return null;
			}
			String owner = evt.getOwner();
			if (owner == null) {
				logger.info(LogUtils.prefix(token)
						+ "try to get an event without owner " + evt.getTitle());
				return null;
			}
			if (helper.canReadCalendar(token, owner)
					|| helper.attendeesContainsUser(evt.getAttendees(), token)) {
				return evt;
			}
			logger.info(LogUtils.prefix(token) + "read not allowed for "
					+ evt.getTitle());
			return null;

		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public KeyList getEventTwinKeys(AccessToken token, String calendar,
			Event event) throws AuthFault, ServerFault {
		if (!helper.canReadCalendar(token, calendar)) {
			throw new ServerFault("user has no read rights on calendar "
					+ calendar);
		}
		try {
			ObmDomain domain = domainService
					.findDomainByName(token.getDomain());
			List<String> keys = calendarDao.findEventTwinKeys(calendar,
					event, domain);
			logger.info(LogUtils.prefix(token) + "found " + keys.size()
					+ " twinkeys ");
			return new KeyList(keys);
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public KeyList getRefusedKeys(AccessToken token, String calendar, Date since)
			throws AuthFault, ServerFault {
		if (!helper.canReadCalendar(token, calendar)) {
			throw new ServerFault("user has no read rights on calendar "
					+ calendar);
		}
		try {
			ObmUser user = userService.getUserFromCalendar(calendar, token.getDomain());
			List<String> keys = calendarDao.findRefusedEventsKeys(user, since);
			return new KeyList(keys);
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public List<Category> listCategories(AccessToken token) throws ServerFault,
			AuthFault {
		try {
			List<Category> c = categoryDao.getCategories(token);
			return c;
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public String getUserEmail(AccessToken token) throws AuthFault, ServerFault {
		try {
			ObmUser obmuser = userService.getUserFromAccessToken(token);
			if (obmuser != null) {
				return obmuser.getEmail();
			}
			return "";

		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public Integer getEventObmIdFromExtId(AccessToken token, String calendar,
			String extId) throws ServerFault {

		Event event = getEventFromExtId(token, calendar, extId);
		if (event != null) {
			return event.getDatabaseId();
		}
		return null;
	}

	@Override
	@Transactional
	public Event getEventFromExtId(AccessToken token, String calendar,
			String extId) throws ServerFault {
		if (!helper.canReadCalendar(token, calendar)) {
			throw new ServerFault("user has no read rights on calendar "
					+ calendar);
		}
		try {
			ObmUser calendarUser = userService.getUserFromCalendar(calendar, token.getDomain());
			return calendarDao.findEventByExtId(token, calendarUser, extId);
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public List<Event> getListEventsFromIntervalDate(AccessToken token,
			String calendar, Date start, Date end) throws AuthFault,
			ServerFault {
		if (!helper.canReadCalendar(token, calendar)) {
			throw new ServerFault("user has no read rights on calendar "
					+ calendar);
		}
		ObmUser calendarUser = null;
		try {
			calendarUser = userService.getUserFromCalendar(calendar, token.getDomain());
		} catch (FindException e1) {
			throw new ServerFault(e1.getMessage());
		}
		try {
			return calendarDao.listEventsByIntervalDate(token,
					calendarUser, start, end, type);
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public List<Event> getAllEvents(AccessToken token, String calendar,
			EventType eventType) throws AuthFault, ServerFault {
		try {
			if (helper.canReadCalendar(token, calendar)) {
				ObmUser calendarUser = userService.getUserFromCalendar(calendar,
						token.getDomain());
				return calendarDao.findAllEvents(token, calendarUser,
						eventType);
			}
			throw new ServerFault("user has no read rights on calendar "
					+ calendar);
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public String parseEvent(AccessToken token, Event event)
			throws ServerFault, AuthFault {
		ObmUser user = userService.getUserFromAccessToken(token);
		return ical4jHelper.parseEvent(event, user);
	}

	@Override
	@Transactional
	public String parseEvents(AccessToken token, List<Event> events)
			throws ServerFault, AuthFault {
		ObmUser user = userService.getUserFromAccessToken(token);
		return ical4jHelper.parseEvents(user, events);
	}

	@Override
	@Transactional
	public List<Event> parseICS(AccessToken token, String ics)
			throws Exception, ServerFault {
		String fixedIcs = fixIcsAttendees(ics);
		ObmUser user = userService.getUserFromAccessToken(token);
		return ical4jHelper.parseICSEvent(fixedIcs, user);
	}

	private String fixIcsAttendees(String ics) {
		// Used to fix a bug in ical4j
		// Error parsing ATTENDEES in delegated VTODOS - ID: 2833134
		String modifiedIcs = ics;
		int i = ics.indexOf("RECEIVED-SEQUENCE");
		while (i > 0) {
			int ie = ics.indexOf(";", i) + 1;
			if (ie <= 0) {
				ie = ics.indexOf(":", i);
			}
			modifiedIcs = ics.substring(0, i) + ics.substring(ie);
			i = ics.indexOf("RECEIVED-SEQUENCE");
		}
		i = ics.indexOf("RECEIVED-DTSTAMP");
		while (i > 0) {
			int ie = ics.indexOf(";", i + 1);
			if (ie <= 0) {
				ie = ics.indexOf(":", i);
			}
			modifiedIcs = ics.substring(0, i - 1) + ics.substring(ie);
			i = ics.indexOf("RECEIVED-DTSTAMP");
		}
		return modifiedIcs;
	}

	@Override
	@Transactional
	public FreeBusyRequest parseICSFreeBusy(AccessToken token, String ics)
			throws ServerFault, AuthFault {
		try {
			return ical4jHelper.parseICSFreeBusy(ics);
		} catch (Exception e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}

	}

	@Override
	@Transactional
	public List<EventParticipationState> getEventParticipationStateWithAlertFromIntervalDate(
			AccessToken token, String specificCalendar, Date start, Date end)
			throws ServerFault, AuthFault {

		try {
			String calendar = getCalendarOrDefault(token, specificCalendar);
			if (helper.canReadCalendar(token, calendar)) {
				ObmUser calendarUser = userService.getUserFromCalendar(calendar,
						token.getDomain());
				return calendarDao
						.getEventParticipationStateWithAlertFromIntervalDate(
								token, calendarUser, start, end, type);
			}
			throw new ServerFault("user has no read rights on calendar "
					+ calendar);
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	private String getCalendarOrDefault(AccessToken token, String calendar) {
		if (StringUtils.isEmpty(calendar)) {
			return token.getUser();
		}
		return calendar;
	}

	@Override
	@Transactional
	public List<EventTimeUpdate> getEventTimeUpdateNotRefusedFromIntervalDate(
			AccessToken token, String calendar, Date start, Date end)
			throws ServerFault, AuthFault {
		try {
			if (helper.canReadCalendar(token, calendar)) {
				ObmUser calendarUser = userService.getUserFromCalendar(calendar,
						token.getDomain());
				return calendarDao
						.getEventTimeUpdateNotRefusedFromIntervalDate(token,
								calendarUser, start, end, type);
			}
			throw new ServerFault("user has no read rights on calendar "
					+ calendar);
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public Date getLastUpdate(AccessToken token, String calendar)
			throws ServerFault, AuthFault {
		try {
			if (helper.canReadCalendar(token, calendar)) {
				return calendarDao.findLastUpdate(token, calendar);
			}
			throw new ServerFault("user has no read rights on calendar "
					+ calendar);
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public boolean isWritableCalendar(AccessToken token, String calendar)
			throws AuthFault, ServerFault {
		try {
			return helper.canWriteOnCalendar(token, calendar);
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}
	
	@Override
	@Transactional
	public List<FreeBusy> getFreeBusy(AccessToken token, FreeBusyRequest fb)
			throws AuthFault, ServerFault {
		try {
			ObmDomain domain = domainService.findDomainByName(token.getDomain());
			return calendarDao.getFreeBusy(domain, fb);
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	@Transactional
	public String parseFreeBusyToICS(AccessToken token, FreeBusy fbr)
			throws ServerFault, AuthFault {
		try {
			return ical4jHelper.parseFreeBusy(fbr);
		} catch (Exception e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}
	}

	public void setEventType(EventType type) {
		this.type = type;
	}

	@Override
	@Transactional
	public boolean changeParticipationState(AccessToken token, String calendar,
			String extId, ParticipationState participationState, int sequence, boolean notification) throws ServerFault {
		if (helper.canWriteOnCalendar(token, calendar)) {
			try {
				return changeParticipationStateInternal(token, calendar, extId, participationState, sequence, notification);
			} catch (Exception e) {
				throw new ServerFault("no user found with calendar " + calendar);
			}
		}
		throw new ServerFault("user has no write rights on calendar " + calendar);
	}

	private boolean changeParticipationStateInternal(AccessToken token,
			String calendar, String extId,
			ParticipationState participationState, int sequence, boolean notification)
			throws FindException, SQLException {
		
		ObmUser calendarOwner = userService.getUserFromCalendar(calendar, token.getDomain());
		Event currentEvent = calendarDao.findEventByExtId(token, calendarOwner, extId);
		boolean changed = false;
		if (currentEvent != null) {
			 changed = applyParticipationChange(token, extId, participationState, 
					sequence, calendarOwner, currentEvent);
		}
		
		Event newEvent = calendarDao.findEventByExtId(token, calendarOwner, extId);
		if (newEvent != null) {
			eventChangeHandler.updateParticipationState(newEvent, calendarOwner, participationState, notification);
		} else {
			logger.error("event with extId : "+ extId + " is no longer in database, ignoring notification");
		}
		return changed;
	}

	private boolean applyParticipationChange(AccessToken token, String extId,
			ParticipationState participationState, int sequence,
			ObmUser calendarOwner, Event currentEvent) throws SQLException {
		
		if (currentEvent.getSequence() == sequence) {
			boolean changed = calendarDao.changeParticipationState(token, calendarOwner, extId, participationState);
			logger.info(LogUtils.prefix(token) + 
					"Calendar : event[extId:" + extId + "] change participation state for user " + 
					calendarOwner.getEmail() + " new state : " + participationState);
			return changed;
		} else {
			logger.info(LogUtils.prefix(token) + 
					"Calendar : event[extId:" + extId + "] ignoring new participation state for user " + 
					calendarOwner.getEmail() + " as sequence number is older than current event");
			return false;
		}
	}

	@Override
	@Transactional
	public int importICalendar(final AccessToken token, final String calendar, final String ics) 
		throws ImportICalendarException, AuthFault, ServerFault {

		if (!helper.canWriteOnCalendar(token, calendar)) {
			String message = "[" + token.getUser() + "] Calendar : "
					+ token.getUser() + " cannot create event on "
					+ calendar + "calendar : no write right";
			logger.info(LogUtils.prefix(token) + message);
			throw new ServerFault(message);
		}
		
		final List<Event> events = parseICSEvent(token, ics);
		int countEvent = 0;
		for (final Event event: events) {

			removeAttendeeWithNoEmail(event);
			if (!isAttendeeExistForCalendarOwner(token, calendar, event.getAttendees())) {
				addAttendeeForCalendarOwner(token, calendar, event);
			}
			
			if(event.isEventInThePast()){
				changeCalendarOwnerAttendeeParticipationStateToAccepted(token, calendar, event);
			}

			if (createEventIfNotExists(token, calendar, event)) {
				countEvent += 1;
			}
		}
		return countEvent;
	}

	private void changeCalendarOwnerAttendeeParticipationStateToAccepted(
			AccessToken at, String calendar, Event event) {
		for (final Attendee attendee: event.getAttendees()) {
			if (isAttendeeExistForCalendarOwner(at, calendar, attendee)) {
				attendee.setState(ParticipationState.ACCEPTED);
			}	
		}
	}

	private void removeAttendeeWithNoEmail(Event event) { 
		final List<Attendee> newAttendees = new ArrayList<Attendee>();
		for (final Attendee attendee: event.getAttendees()) {
			if (attendee.getEmail() != null) {
				newAttendees.add(attendee);
			}
		}
		event.setAttendees(newAttendees);
	}

	private boolean createEventIfNotExists(final AccessToken token, final String calendar, final Event event)
			throws ImportICalendarException {
		try {
			if (!isEventExists(token, calendar, event)) {
				final Event newEvent = calendarDao.createEvent(token, calendar, event, false);
				if (newEvent != null) {
					return true;
				}
			}
		} catch (FindException e) {
			throw new ImportICalendarException(e);
		} catch (SQLException e) {
			throw new ImportICalendarException(e);
		}
		return false;
	}

	private boolean isEventExists(final AccessToken token, final String calendar, final Event event) throws FindException {
		final ObmUser calendarUser = userService.getUserFromCalendar(calendar, token.getDomain());
		if (StringUtils.isNotEmpty(event.getExtId())) {
			final Event eventExist = calendarDao.findEventByExtId(token, calendarUser, event.getExtId());
			if (eventExist != null) {
				return true;
			}
		}
		return false;
	}
	
	private List<Event> parseICSEvent(final AccessToken token, final String icsToString) throws ImportICalendarException {
		try {
			ObmUser user = userService.getUserFromAccessToken(token);
			return ical4jHelper.parseICSEvent(icsToString, user);
		} catch (IOException e) {
			throw new ImportICalendarException(e);
		} catch (ParserException e) {
			throw new ImportICalendarException(e);
		}
	}
	
	private void addAttendeeForCalendarOwner(final AccessToken token, final String calendar, final Event event) throws ImportICalendarException {
		try {
			final ObmUser obmUser = userService.getUserFromCalendar(calendar, token.getDomain());
			final Attendee attendee = new Attendee();
			attendee.setEmail(obmUser.getEmail());
			event.getAttendees().add(attendee);
		} catch (FindException e) {
			throw new ImportICalendarException("user " + calendar + " not found");
		}
	}

	private boolean isAttendeeExistForCalendarOwner(final AccessToken at, final String calendar, final List<Attendee> attendees) {
		for (final Attendee attendee: attendees) {
			if (isAttendeeExistForCalendarOwner(at, calendar, attendee)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isAttendeeExistForCalendarOwner(final AccessToken at, final String calendar, final Attendee attendee) {
		final ObmUser obmUser = userService.getUserFromAttendee(attendee,
				at.getDomain());
		if (obmUser != null) {
			if (obmUser.getLogin().equals(calendar)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void purge(final AccessToken token, final String calendar) throws ServerFault {
		if (!helper.canReadCalendar(token, calendar)) {
			throw new ServerFault("user has no read rights on calendar " + calendar);
		}
		try {
			final ObmUser obmUser = userService.getUserFromCalendar(calendar, token.getDomain());
			
			final Calendar endDate = Calendar.getInstance();
			endDate.add(Calendar.MONTH, -6);
		
			final List<Event> events = calendarDao.listEventsByIntervalDate(token, obmUser, new Date(0), endDate.getTime(), type);
			for (final Event event: events) {
				removeEvent(token, calendar, event.getUid(), event.getSequence() + 1, false);
			}
			
		} catch (Throwable e) {
			logger.error(LogUtils.prefix(token) + e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}		
	}
	
}
