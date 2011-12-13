package org.obm.push.calendar;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.obm.push.EventConverter;
import org.obm.push.backend.DataDelta;
import org.obm.push.bean.AttendeeStatus;
import org.obm.push.bean.BackendSession;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.IApplicationData;
import org.obm.push.bean.ItemChange;
import org.obm.push.bean.MSEmail;
import org.obm.push.bean.MSEvent;
import org.obm.push.bean.MSTask;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.SyncState;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.UnknownObmSyncServerException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.ServerItemNotFoundException;
import org.obm.push.impl.ObmSyncBackend;
import org.obm.push.service.EventService;
import org.obm.push.store.CollectionDao;
import org.obm.sync.NotAllowedException;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.auth.EventAlreadyExistException;
import org.obm.sync.auth.EventNotFoundException;
import org.obm.sync.auth.ServerFault;
import org.obm.sync.calendar.Attendee;
import org.obm.sync.calendar.CalendarInfo;
import org.obm.sync.calendar.Event;
import org.obm.sync.calendar.EventExtId;
import org.obm.sync.calendar.EventObmId;
import org.obm.sync.calendar.ParticipationState;
import org.obm.sync.client.book.BookClient;
import org.obm.sync.client.calendar.AbstractEventSyncClient;
import org.obm.sync.client.calendar.CalendarClient;
import org.obm.sync.client.calendar.TodoClient;
import org.obm.sync.items.EventChanges;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CalendarBackend extends ObmSyncBackend {

	private final EventConverter eventConverter;
	private final TodoConverter todoConverter;
	private final EventService eventService;

	@Inject
	private CalendarBackend(CollectionDao collectionDao, 
			BookClient bookClient, CalendarClient calendarClient, TodoClient todoClient,
			EventConverter eventConverter, TodoConverter todoConverter, EventService eventService) {
		super(collectionDao, bookClient, calendarClient, todoClient);
		this.eventConverter = eventConverter;
		this.todoConverter = todoConverter;
		this.eventService = eventService;
	}

	public List<ItemChange> getHierarchyChanges(BackendSession bs) 
			throws DaoException, CollectionNotFoundException, UnknownObmSyncServerException {

		if (!bs.checkHint("hint.multipleCalendars", false)) {
			return getDefaultCalendarItemChange(bs);
		} else {
			return getCalendarList(bs);
		}
	}

	private List<ItemChange> getCalendarList(BackendSession bs) throws DaoException, CollectionNotFoundException, UnknownObmSyncServerException {

		List<ItemChange> ret = new LinkedList<ItemChange>();
		AbstractEventSyncClient cc = getCalendarClient();
		AccessToken token = login(cc, bs);
		try {
			CalendarInfo[] cals = cc.listCalendars(token);
			String domain = bs.getUser().getDomain();
			for (CalendarInfo ci : cals) {
				ItemChange ic = new ItemChange();
				String col = "obm:\\\\" + bs.getUser().getLoginAtDomain()
						+ "\\calendar\\" + ci.getUid() + domain;
				Integer collectionId = getCollectionIdFor(bs.getDevice(), col);
				ic.setServerId(collectionIdToString(collectionId));
				ic.setParentId("0");
				ic.setDisplayName(ci.getMail() + " calendar");
				if (bs.getUser().getLoginAtDomain().equalsIgnoreCase(ci.getMail())) {
					ic.setItemType(FolderType.DEFAULT_CALENDAR_FOLDER);
				} else {
					ic.setItemType(FolderType.USER_CREATED_CALENDAR_FOLDER);
				}
				ret.add(ic);
			}
		} catch (ServerFault e) {
			throw new UnknownObmSyncServerException(e);
		} finally {
			cc.logout(token);
		}
		return ret;
	}

	private List<ItemChange> getDefaultCalendarItemChange(BackendSession bs) throws DaoException {
		
		ItemChange ic = new ItemChange();
		String col = getDefaultCalendarName(bs);
		String serverId = "";
		try {
			Integer collectionId = getCollectionIdFor(bs.getDevice(), col);
			serverId = collectionIdToString(collectionId);
		} catch (CollectionNotFoundException e) {
			serverId = createCollectionMapping(bs.getDevice(), col);
			ic.setIsNew(true);
		}
		ic.setServerId(serverId);
		ic.setParentId("0");
		ic.setDisplayName(bs.getUser().getLoginAtDomain() + " calendar");
		ic.setItemType(FolderType.DEFAULT_CALENDAR_FOLDER);
		return ImmutableList.of(ic);
	}

	public List<ItemChange> getHierarchyTaskChanges(BackendSession bs) throws DaoException {
		List<ItemChange> ret = new ArrayList<ItemChange>(1);
		ItemChange ic = new ItemChange();
		String col = "obm:\\\\" + bs.getUser().getLoginAtDomain() + "\\tasks\\"
				+ bs.getUser().getLoginAtDomain();
		String serverId;
		try {
			Integer collectionId = getCollectionIdFor(bs.getDevice(), col);
			serverId = collectionIdToString(collectionId);
		} catch (CollectionNotFoundException e) {
			serverId = createCollectionMapping(bs.getDevice(), col);
			ic.setIsNew(true);
		}
		ic.setServerId(serverId);
		ic.setParentId("0");
		ic.setDisplayName(bs.getUser().getLoginAtDomain() + " tasks");
		ic.setItemType(FolderType.DEFAULT_TASKS_FOLDER);
		ret.add(ic);
		return ret;
	}

	public DataDelta getContentChanges(BackendSession bs, SyncState state, Integer collectionId, FilterType filterType) 
			throws CollectionNotFoundException, DaoException, UnknownObmSyncServerException {
		
		AbstractEventSyncClient cc = getEventSyncClient(state.getDataType());
		AccessToken token = login(cc, bs);
		
		List<ItemChange> addUpd = new LinkedList<ItemChange>();
		List<ItemChange> deletions = new LinkedList<ItemChange>();
		Date syncDate = null;
		
		String collectionPath = getCollectionPathFor(collectionId);
		String calendar = parseCalendarName(collectionPath);

		state.updatingLastSync(filterType);
		try {
			
			EventChanges changes = null;
			if (state.isLastSyncFiltred()) {
				changes = cc.getSyncEventDate(token, calendar, state.getLastSync());
			} else {
				changes = cc.getSync(token, calendar, state.getLastSync());
			}
			
			logger.info("Event changes [ {} ]", changes.getUpdated().length);
			logger.info("Event changes LastSync [ {} ]", changes.getLastSync().toString());
			
			final String userEmail = cc.getUserEmail(token);
			addOrRemoveEventFilter(addUpd, deletions, changes, userEmail, collectionId, bs);
			syncDate = changes.getLastSync();
			
		} catch (ServerFault e) {
			throw new UnknownObmSyncServerException(e);
		} finally {
			cc.logout(token);
		}
		logger.info("getContentChanges( {}, {}, lastSync = {} ) => {} entries.",
				new Object[]{calendar, collectionPath, state.getLastSync(), addUpd.size()});
		return new DataDelta(addUpd, deletions, syncDate);
	}

	private void addOrUpdateEventFilter(List<ItemChange> addUpd, Event[] events, String userEmail, 
			Integer collectionId, BackendSession bs) throws DaoException {
		
		for (final Event event : events) {
			if (checkIfEventCanBeAdded(event, userEmail) && event.getRecurrenceId() == null) {
				String serverId = getServerIdFor(collectionId, event.getObmId());
				ItemChange change = createItemChangeToAddFromEvent(bs, event, serverId);
				addUpd.add(change);
			}			
		}
	}
	
	private void removeEventFilter(List<ItemChange> deletions, Event[] events, EventObmId[] eventsIdRemoved, 
			String userEmail, Integer collectionId) {
		
		for (final Event event : events) {
			if (!checkIfEventCanBeAdded(event, userEmail)) {
				deletions.add(getItemChange(collectionId, event.getObmId()));
			}			
		}
		
		for (final EventObmId eventIdRemove : eventsIdRemoved) {
			deletions.add(getItemChange(collectionId, eventIdRemove));
		}
	}
	
	private ItemChange getItemChange(Integer collectionId,	EventObmId eventIdRemove) {
		return getItemChange(collectionId, eventIdRemove.serializeToString());
	}

	private void addOrRemoveEventFilter(List<ItemChange> addUpd, List<ItemChange> deletions, 
			EventChanges eventChanges, String userEmail, Integer collectionId, BackendSession bs) throws DaoException {
		
		addOrUpdateEventFilter(addUpd, eventChanges.getUpdated(), userEmail, collectionId, bs);
		removeEventFilter(deletions, eventChanges.getUpdated(), eventChanges.getRemoved(), userEmail, collectionId);
	}

	private boolean checkIfEventCanBeAdded(Event event, String userEmail) {
		for (final Attendee attendee : event.getAttendees()) {
			if (userEmail.equals(attendee.getEmail()) && 
					ParticipationState.DECLINED.equals(attendee.getState())) {
				return false;
			}
		}
		return true;
	}
	
	private String parseCalendarName(String collectionPath) {
		// parse obm:\\thomas@zz.com\calendar\sylvaing@zz.com
		int slash = collectionPath.lastIndexOf("\\");
		int at = collectionPath.lastIndexOf("@");
		return collectionPath.substring(slash + 1, at);
	}

	private ItemChange createItemChangeToAddFromEvent(final BackendSession bs, final Event event, String serverId) throws DaoException {
		ItemChange ic = new ItemChange();
		ic.setServerId(serverId);
		
		IApplicationData ev = convertObmObjectToMSObject(bs, event);
		ic.setData(ev);
		return ic;
	}

	private String getServerIdFor(Integer collectionId, EventObmId uid) {
		return getServerIdFor(collectionId, uid.serializeToString());
	}

	public String createOrUpdate(BackendSession bs, Integer collectionId, String serverId, IApplicationData data) 
			throws CollectionNotFoundException, DaoException, UnknownObmSyncServerException, ServerItemNotFoundException {

		AbstractEventSyncClient cc = getEventSyncClient(data.getType());
		AccessToken token = login(cc, bs);
		
		String collectionPath = getCollectionPathFor(collectionId);
		logger.info("createOrUpdate( collectionPath = {}, serverId = {} )", new Object[]{collectionPath, serverId});
		
		EventObmId eventId = null;
		Event event = null;
		try {
			
			Event oldEvent = null;
			if (serverId != null) {
				eventId = convertServerIdToEventObmId(serverId);
				oldEvent = cc.getEventFromId(token, bs.getUser().getLoginAtDomain(), eventId);	
			}

			boolean isInternal = EventConverter.isInternalEvent(oldEvent, true);
			event = convertMSObjectToObmObject(bs, data, oldEvent, isInternal);

			if (eventId != null) {
				event.setUid(eventId);
				setSequence(oldEvent, event);
				updateCalendarEntity(cc, token, collectionPath, oldEvent, event);
			} else {
				eventId = createCalendarEntity(bs, cc, token, collectionPath, event, data);
			}
				
		} catch (ServerFault e) {
			throw new UnknownObmSyncServerException(e);
		} catch (EventAlreadyExistException e) {
			eventId = getEventIdFromExtId(token, collectionPath, cc, event);
		} catch (EventNotFoundException e) {
			throw new ServerItemNotFoundException(serverId);
		} finally {
			cc.logout(token);
		}
		
		return getServerIdFor(collectionId, eventId);
	}

	private void updateCalendarEntity(AbstractEventSyncClient cc, AccessToken token, String collectionPath, Event old, Event event) throws ServerFault {
		if (event.getExtId() == null || event.getExtId().getExtId() == null) {
			event.setExtId(old.getExtId());
		}
		cc.modifyEvent(token, parseCalendarName(collectionPath), event, true, true);
	}

	private void setSequence(Event oldEvent, Event event) {
		if (event.hasImportantChanges(oldEvent)) {
			event.setSequence(oldEvent.getSequence() + 1);
		} else {
			event.setSequence(oldEvent.getSequence());
		}
	}

	private EventObmId createCalendarEntity(BackendSession bs, AbstractEventSyncClient cc,
			AccessToken token, String collectionPath, Event event, IApplicationData data)
			throws ServerFault, EventAlreadyExistException, DaoException {
		switch (event.getType()) {
		case VEVENT:
			return createEvent(bs, cc, token, collectionPath, event, (MSEvent) data);
		case VTODO:
			return createTodo(cc, token, collectionPath, event);
		default:
			throw new InvalidParameterException("unsupported type " + event.getType());
		}
	}

	private EventObmId createTodo(AbstractEventSyncClient cc,
			AccessToken token, String collectionPath, Event event)
			throws ServerFault, EventAlreadyExistException {
		return cc.createEvent(token, parseCalendarName(collectionPath), event, true);
	}

	private EventObmId createEvent(BackendSession bs, AbstractEventSyncClient cc,
			AccessToken token, String collectionPath, Event event, MSEvent msEvent)
			throws ServerFault, EventAlreadyExistException, DaoException {
		EventExtId eventExtId = generateExtId();
		event.setExtId(eventExtId);
		EventObmId eventId = cc.createEvent(token, parseCalendarName(collectionPath), event, true);
		eventService.trackEventObmIdMSEventUidTranslation(eventId, msEvent.getUid(), bs.getDevice());
		return eventId;
	}

	private EventExtId generateExtId() {
		UUID uuid = UUID.randomUUID();
		return new EventExtId(uuid.toString());
	}
	
	private EventObmId convertServerIdToEventObmId(String serverId) {
		int idx = serverId.lastIndexOf(":");
		return new EventObmId(serverId.substring(idx + 1));
	}

	private Event convertMSObjectToObmObject(BackendSession bs,
			IApplicationData data, Event oldEvent, boolean isInternal) {
		switch (data.getType()) {
		case CALENDAR:
			return eventConverter.convert(bs, oldEvent, (MSEvent) data, isInternal);
		case TASKS:
			return todoConverter.convert(bs, oldEvent, (MSTask) data, isInternal);
		default:
			throw new InvalidParameterException("Can't convert type " + data.getType());
		}
	}
	
	private EventObmId getEventIdFromExtId(AccessToken token, String collectionPath, AbstractEventSyncClient cc, Event event)
			throws UnknownObmSyncServerException {
		
		try {
			return cc.getEventObmIdFromExtId(token, parseCalendarName(collectionPath), event.getExtId());
		} catch (ServerFault e) {
			throw new UnknownObmSyncServerException(e);
		} catch (EventNotFoundException e) {
			logger.info(e.getMessage());
		}
		return null;
	}

	public void delete(BackendSession bs, Integer collectionId, String serverId) 
			throws CollectionNotFoundException, DaoException, UnknownObmSyncServerException, ServerItemNotFoundException {
		
		String collectionPath = getCollectionPathFor(collectionId);
		if (serverId != null) {

			AbstractEventSyncClient bc = getEventSyncClient(PIMDataType.CALENDAR);
			AccessToken token = login(bc, bs);
			try {
				logger.info("Delete event serverId {}", serverId);
				//FIXME: not transactional
				String calendarName = parseCalendarName(collectionPath);
				Event evr = getEventFromServerId(bc, token, calendarName, serverId);
				bc.removeEventById(token, calendarName, evr.getObmId(), evr.getSequence(), true);
			} catch (ServerFault e) {
				throw new UnknownObmSyncServerException(e);
			} catch (EventNotFoundException e) {
				throw new ServerItemNotFoundException(serverId);
			} catch (NotAllowedException e) {
				logger.error(e.getMessage(), e);
			} finally {
				bc.logout(token);
			}
		}
	}

	public String handleMeetingResponse(BackendSession bs, MSEmail invitation, AttendeeStatus status) 
			throws UnknownObmSyncServerException, CollectionNotFoundException, DaoException, ServerItemNotFoundException {
		
		MSEvent event = invitation.getInvitation();
		AbstractEventSyncClient calCli = getEventSyncClient(event.getType());
		AccessToken at = calCli.login(bs.getUser().getLoginAtDomain(), bs.getPassword(), "o-push");
		try {
			logger.info("handleMeetingResponse = {}", event.getObmId());
			Event obmEvent = createOrModifyInvitationEvent(bs, event, calCli, at);
			event.setObmId(obmEvent.getObmId());
			event.setObmSequence(obmEvent.getSequence());
			return updateUserStatus(bs, event, status, calCli, at);
		} catch (UnknownObmSyncServerException e) {
			throw e;
		} catch (EventNotFoundException e) {
			throw new ServerItemNotFoundException(e);
		} finally {
			calCli.logout(at);
		}
	}

	private Event createOrModifyInvitationEvent(BackendSession bs, MSEvent event, AbstractEventSyncClient calCli, AccessToken at) 
			throws UnknownObmSyncServerException, EventNotFoundException {
		
		try {
			Event obmEvent = getEventFromExtId(bs, event, calCli, at);
			
			boolean isInternal = EventConverter.isInternalEvent(obmEvent, false);
			Event newEvent = convertMSObjectToObmObject(bs, event, null, isInternal);
			
			if (obmEvent == null) {
				
				EventObmId id = null;
				try {
					id = calCli.createEvent(at, bs.getUser().getLoginAtDomain(), newEvent, isInternal);
				} catch (EventAlreadyExistException e) {
					throw new UnknownObmSyncServerException("it's not possible because getEventFromExtId == null");
				}
				logger.info("createOrModifyInvitationEvent : create new event {}", event.getObmId());
				return calCli.getEventFromId(at, bs.getUser().getLoginAtDomain(), id);
				
			} else {
			
				newEvent.setUid(obmEvent.getObmId());
				newEvent.setSequence(obmEvent.getSequence());
				if(!obmEvent.isInternalEvent()){
					logger.info("createOrModifyInvitationEvent : update event {}", event.getObmId());
					obmEvent = calCli.modifyEvent(at, bs.getUser().getLoginAtDomain(), newEvent, true, false);
				}
				return obmEvent;
			}	
			
		} catch (ServerFault fault) {
			throw new UnknownObmSyncServerException(fault);
		}		
	}

	private Event getEventFromExtId(BackendSession bs, MSEvent event, AbstractEventSyncClient calCli, AccessToken at) 
			throws ServerFault {
		try {
			return calCli.getEventFromExtId(at, bs.getUser().getLoginAtDomain(), event.getExtId());
		} catch (EventNotFoundException e) {
			logger.info(e.getMessage());
		}
		return null;
	}

	private String updateUserStatus(BackendSession bs, MSEvent msEvent, AttendeeStatus status, AbstractEventSyncClient calCli,
			AccessToken at) throws CollectionNotFoundException, DaoException, UnknownObmSyncServerException {
		
		logger.info("update user status[ {} in calendar ]", status.toString());
		ParticipationState participationStatus = EventConverter.getParticipationState(null, status);
		try {
			calCli.changeParticipationState(at, bs.getUser().getLoginAtDomain(), msEvent.getExtId(), participationStatus, msEvent.getObmSequence(), true);
			Integer collectionId = getCollectionIdFor(bs.getDevice(), getDefaultCalendarName(bs));
			return getServerIdFor(collectionId, msEvent.getObmId());
		} catch (ServerFault e) {
			throw new UnknownObmSyncServerException(e);
		}
	}

	public List<ItemChange> fetchItems(BackendSession bs, List<String> fetchServerIds) throws DaoException {
		List<ItemChange> ret = new LinkedList<ItemChange>();
		AbstractEventSyncClient calCli = getEventSyncClient(PIMDataType.CALENDAR);
		AccessToken token = login(calCli, bs);
		for (String serverId : fetchServerIds) {
			try {
				Event event = getEventFromServerId(calCli, token, bs.getUser().getLoginAtDomain(), serverId);
				if (event != null) {
					ItemChange ic = createItemChangeToAddFromEvent(bs, event, serverId);
					ret.add(ic);
				}
			} catch (EventNotFoundException e) {
				logger.error("event from serverId {} not found.", serverId);
			} catch (ServerFault e1) {
				logger.error(e1.getMessage(), e1);
			}
		}
		calCli.logout(token);	
		return ret;
	}
	
	public List<ItemChange> fetchItems(BackendSession bs, Integer collectionId, Collection<EventObmId> uids) throws DaoException {
		List<ItemChange> ret = new LinkedList<ItemChange>();
		AbstractEventSyncClient calCli = getEventSyncClient(PIMDataType.CALENDAR);
		AccessToken token = login(calCli, bs);
		for (EventObmId itemId : uids) {
			try {
				Event e = calCli.getEventFromId(token, bs.getUser().getLoginAtDomain(), itemId);
				if (e != null) {
					String serverId = getServerIdFor(collectionId, e.getObmId());
					ret.add(createItemChangeToAddFromEvent(bs, e, serverId));
				}
			} catch (ServerFault e) {
				logger.error(e.getMessage(), e);
			} catch (EventNotFoundException e) {
				logger.error("fetchItems : event from extId {} not found", itemId);
			}
		}
		calCli.logout(token);
		return ret;
	}

	private IApplicationData convertObmObjectToMSObject(BackendSession bs, Event event) throws DaoException {
		switch (event.getType()) {
		case VEVENT:
			return eventService.convertEventToMSEvent(bs, event);
		case VTODO:
			return todoConverter.convert(event);
		default:
			throw new InvalidParameterException("can't convert type " + event.getType());
		}
	}
	
	public List<ItemChange> fetchDeletedItems(BackendSession bs, Integer collectionId, Collection<EventObmId> uids) {
		final List<ItemChange> ret = Lists.newArrayListWithCapacity(uids.size());
		final AbstractEventSyncClient calCli = getEventSyncClient(PIMDataType.CALENDAR);
		final AccessToken token = login(calCli, bs);
		for (final EventObmId eventUid : uids) {
			try {
				Event event = calCli.getEventFromId(token, bs.getUser().getLoginAtDomain(), eventUid);
				if (event != null) {
					ret.add(getItemChange(collectionId, event.getObmId()));
				}
			} catch (ServerFault e) {
				logger.error(e.getMessage(), e);
			} catch (EventNotFoundException e) {
				logger.error("fetchDeletedItems : event from extId {} not found", eventUid);
			}
		}
		calCli.logout(token);
		return ret;
	}
	
	
	public Integer getCollectionId (BackendSession bs) throws CollectionNotFoundException, DaoException {
		String calPath = getDefaultCalendarName(bs);
		return getCollectionIdFor(bs.getDevice(), calPath);
	}

	public Event getEventFromServerId(BackendSession bs, String serverId) {
		
		final AbstractEventSyncClient calCli = getEventSyncClient(PIMDataType.CALENDAR);
		final AccessToken token = login(calCli, bs);
		try {
			return getEventFromServerId(calCli, token, bs.getUser().getLoginAtDomain(), serverId);
		} catch (EventNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		} finally {
			calCli.logout(token);
		}
		return null;
	}


	private Event getEventFromServerId(AbstractEventSyncClient calCli, AccessToken token, String calendar, String serverId) throws ServerFault, EventNotFoundException {
		Integer itemId = getItemIdFor(serverId);
		if (itemId == null) {
			return null;
		}
		return calCli.getEventFromId(token, calendar, new EventObmId(itemId));
	}

	
	public EventExtId getEventExtIdFromServerId(BackendSession bs, String serverId) {
		Event event = getEventFromServerId(bs, serverId);
		if (event != null) {
			return event.getExtId();
		}
		return null;
	}

}
