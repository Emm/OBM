package org.obm.push.bean;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.obm.sync.calendar.EventExtId;
import org.obm.sync.calendar.EventObmId;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;

public class MSEvent implements IApplicationData, Serializable {
	
	private final Set<MSAttendee> attendees;
	private String organizerName;
	private String organizerEmail;
	private String location;
	private String subject;
	private EventObmId obmId;
	private EventExtId extId;
	private MSEventUid uid;
	private String description;
	private Date created;
	private Date lastUpdate;
	private Date dtStamp;
	private Date endTime;
	private Date startTime;
	private Boolean allDayEvent;
	private CalendarBusyStatus busyStatus;
	private CalendarSensitivity sensitivity;
	private CalendarMeetingStatus meetingStatus;
	private Integer reminder;
	private List<String> categories;
	private Recurrence recurrence;
	private List<MSEvent> exceptions;
	private TimeZone timeZone;
	private Date exceptionStartTime;
	private boolean deletedException;
	private Integer obmSequence;
	private transient Set<String> attendeeEmails;
	
	public MSEvent(){
		this.attendees = new HashSet<MSAttendee>();
		this.attendeeEmails = new HashSet<String>();
	}

	public TimeZone getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	public String getOrganizerName() {
		return organizerName;
	}

	public void setOrganizerName(String organizerName) {
		this.organizerName = organizerName;
	}

	public String getOrganizerEmail() {
		return organizerEmail;
	}

	public void setOrganizerEmail(String organizerEmail) {
		this.organizerEmail = organizerEmail;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public EventObmId getObmId() {
		return obmId;
	}

	public void setObmId(EventObmId obmId) {
		this.obmId = obmId;
	}
	
	public EventExtId getExtId() {
		return extId;
	}
	
	public void setExtId(EventExtId extId) {
		this.extId = extId;
	}

	public Boolean getAllDayEvent() {
		return allDayEvent;
	}

	public void setAllDayEvent(Boolean allDayEvent) {
		this.allDayEvent = allDayEvent;
	}

	public CalendarBusyStatus getBusyStatus() {
		return busyStatus;
	}

	public void setBusyStatus(CalendarBusyStatus busyStatus) {
		this.busyStatus = busyStatus;
	}

	public CalendarSensitivity getSensitivity() {
		return sensitivity;
	}

	public void setSensitivity(CalendarSensitivity sensitivity) {
		this.sensitivity = sensitivity;
	}

	public CalendarMeetingStatus getMeetingStatus() {
		return meetingStatus;
	}

	public void setMeetingStatus(CalendarMeetingStatus meetingStatus) {
		this.meetingStatus = meetingStatus;
	}

	public Integer getReminder() {
		return reminder;
	}

	public void setReminder(Integer reminder) {
		this.reminder = reminder;
	}

	public Set<MSAttendee> getAttendees() {
		return attendees;
	}
	
	public void addAttendee(MSAttendee att) {
		if(!attendeeEmails.contains(att.getEmail())){
			attendees.add(att);
			attendeeEmails.add(att.getEmail());
		}
	}

	public List<String> getCategories() {
		return categories;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}

	public Recurrence getRecurrence() {
		return recurrence;
	}

	public void setRecurrence(Recurrence recurrence) {
		this.recurrence = recurrence;
	}

	public List<MSEvent> getExceptions() {
		return exceptions;
	}

	public void setExceptions(List<MSEvent> exceptions) {
		this.exceptions = exceptions;
	}

	public void setDeleted(boolean deleted) {
		this.deletedException = deleted;
	}

	public boolean isDeletedException() {
		return deletedException;
	}

	@Override
	public PIMDataType getType() {
		return PIMDataType.CALENDAR;
	}

	public Date getDtStamp() {
		if(dtStamp != null){
			return dtStamp;
		}
		return new Date(0);
	}

	public void setDtStamp(Date dtStamp) {
		this.dtStamp = dtStamp;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getExceptionStartTime() {
		return exceptionStartTime;
	}

	public void setExceptionStartTime(Date exceptionStartTime) {
		this.exceptionStartTime = exceptionStartTime;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public Integer getObmSequence() {
		return obmSequence;
	}

	public void setObmSequence(Integer obmSequence) {
		this.obmSequence = obmSequence;
	}
	
	public MSEventUid getUid() {
		return uid;
	}
	
	public void setUid(MSEventUid uid) {
		this.uid = uid;
	}
	
	public MessageClass findReplyType() {
		//a reply must contain a single attendee
		if (attendees.size() != 1) {
			return null;
		}
		MSAttendee attendee = Iterables.getFirst(getAttendees(), null);
		switch (attendee.getAttendeeStatus()) {
		case TENTATIVE:
			return MessageClass.ScheduleMeetingRespTent;
		case ACCEPT:
			return MessageClass.ScheduleMeetingRespPos;
		case DECLINE:
			return MessageClass.ScheduleMeetingRespNeg;
		default:
			return null;
		}
	}

	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("attendees", attendees)
			.add("organizerName", organizerName)
			.add("organizerEmail", organizerEmail)
			.add("location", location)
			.add("subject", subject)
			.add("obmId", obmId)
			.add("extId", extId)
			.add("description", description)
			.add("created", created)
			.add("lastUpdate", lastUpdate)
			.add("dtStamp", dtStamp)
			.add("endTime", endTime)
			.add("startTime", startTime)
			.add("allDayEvent", allDayEvent)
			.add("busyStatus", busyStatus)
			.add("sensitivity", sensitivity)
			.add("meetingStatus", meetingStatus)
			.add("reminder", reminder)
			.add("categories", categories)
			.add("recurrence", recurrence)
			.add("exceptions", exceptions)
			.add("timeZone", timeZone)
			.add("exceptionStartTime", exceptionStartTime)
			.add("deletedException", deletedException)
			.add("obmSequence", obmSequence)
			.add("uid", uid)
			.toString();
	}

	@Override
	public final int hashCode() {
		return Objects.hashCode(attendees, organizerName, organizerEmail, location, 
				subject, obmId, extId, description, created, lastUpdate, dtStamp, 
				endTime, startTime, allDayEvent, busyStatus, sensitivity, meetingStatus, 
				reminder, categories, recurrence, exceptions, timeZone, exceptionStartTime, 
				deletedException, obmSequence, uid);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof MSEvent) {
			MSEvent that = (MSEvent) object;
			return Objects.equal(this.attendees, that.attendees)
				&& Objects.equal(this.organizerName, that.organizerName)
				&& Objects.equal(this.organizerEmail, that.organizerEmail)
				&& Objects.equal(this.location, that.location)
				&& Objects.equal(this.subject, that.subject)
				&& Objects.equal(this.obmId, that.obmId)
				&& Objects.equal(this.extId, that.extId)
				&& Objects.equal(this.description, that.description)
				&& Objects.equal(this.created, that.created)
				&& Objects.equal(this.lastUpdate, that.lastUpdate)
				&& Objects.equal(this.dtStamp, that.dtStamp)
				&& Objects.equal(this.endTime, that.endTime)
				&& Objects.equal(this.startTime, that.startTime)
				&& Objects.equal(this.allDayEvent, that.allDayEvent)
				&& Objects.equal(this.busyStatus, that.busyStatus)
				&& Objects.equal(this.sensitivity, that.sensitivity)
				&& Objects.equal(this.meetingStatus, that.meetingStatus)
				&& Objects.equal(this.reminder, that.reminder)
				&& Objects.equal(this.categories, that.categories)
				&& Objects.equal(this.recurrence, that.recurrence)
				&& Objects.equal(this.exceptions, that.exceptions)
				&& Objects.equal(this.timeZone, that.timeZone)
				&& Objects.equal(this.exceptionStartTime, that.exceptionStartTime)
				&& Objects.equal(this.deletedException, that.deletedException)
				&& Objects.equal(this.uid, that.uid)
				&& Objects.equal(this.obmSequence, that.obmSequence);
		}
		return false;
	}
	
	
	
}
