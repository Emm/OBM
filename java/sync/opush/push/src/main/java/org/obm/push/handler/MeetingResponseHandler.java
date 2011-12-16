package org.obm.push.handler;

import java.util.ArrayList;
import java.util.List;

import org.obm.push.backend.IBackend;
import org.obm.push.backend.IContentsExporter;
import org.obm.push.backend.IContentsImporter;
import org.obm.push.backend.IContinuation;
import org.obm.push.bean.AttendeeStatus;
import org.obm.push.bean.BackendSession;
import org.obm.push.bean.MSEmail;
import org.obm.push.bean.MeetingResponse;
import org.obm.push.bean.MeetingResponseStatus;
import org.obm.push.bean.PIMDataType;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.UnknownObmSyncServerException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.NoDocumentException;
import org.obm.push.exception.activesync.ProcessingEmailException;
import org.obm.push.exception.activesync.ServerItemNotFoundException;
import org.obm.push.impl.Responder;
import org.obm.push.mail.MailBackend;
import org.obm.push.protocol.MeetingProtocol;
import org.obm.push.protocol.bean.MeetingHandlerRequest;
import org.obm.push.protocol.bean.MeetingHandlerResponse;
import org.obm.push.protocol.bean.MeetingHandlerResponse.ItemChangeMeetingResponse;
import org.obm.push.protocol.data.EncoderFactory;
import org.obm.push.protocol.request.ActiveSyncRequest;
import org.obm.push.state.StateMachine;
import org.obm.push.store.CollectionDao;
import org.obm.push.wbxml.WBXMLTools;
import org.w3c.dom.Document;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Handles the MeetingResponse cmd
 */
@Singleton
public class MeetingResponseHandler extends WbxmlRequestHandler {

	private final MeetingProtocol meetingProtocol;
	private final MailBackend mailBackend;
	
	@Inject
	protected MeetingResponseHandler(IBackend backend,
			EncoderFactory encoderFactory, IContentsImporter contentsImporter,
			IContentsExporter contentsExporter,	StateMachine stMachine, 
			MeetingProtocol meetingProtocol, CollectionDao collectionDao,
			MailBackend mailBackend, WBXMLTools wbxmlTools) {
		
		super(backend, encoderFactory, contentsImporter,
				contentsExporter, stMachine, collectionDao, wbxmlTools);
		this.meetingProtocol = meetingProtocol;
		this.mailBackend = mailBackend;
	}

	// <?xml version="1.0" encoding="UTF-8"?>
	// <MeetingResponse>
	// <Request>
	// <UserResponse>1</UserResponse>
	// <CollectionId>62</CollectionId>
	// <ReqId>62:379</ReqId>
	// </Request>
	// </MeetingResponse>

	@Override
	protected void process(IContinuation continuation, BackendSession bs,
			Document doc, ActiveSyncRequest request, Responder responder) {
		
		MeetingHandlerRequest meetingRequest;
		try {
			
			meetingRequest = meetingProtocol.getRequest(doc);
			MeetingHandlerResponse meetingResponse = doTheJob(meetingRequest, bs);
			Document document = meetingProtocol.encodeResponses(meetingResponse);
			sendResponse(responder, document);
			
		} catch (NoDocumentException e) {
			sendErrorResponse(responder, MeetingResponseStatus.INVALID_MEETING_RREQUEST);
		} catch (DaoException e) {
			logger.error(e.getMessage(), e);
			sendErrorResponse(responder, MeetingResponseStatus.SERVER_ERROR);
		} catch (CollectionNotFoundException e) {
			sendErrorResponse(responder, MeetingResponseStatus.INVALID_MEETING_RREQUEST);
		} catch (ProcessingEmailException e) {
			logger.error(e.getMessage(), e);
			sendErrorResponse(responder, MeetingResponseStatus.SERVER_ERROR);
		}
	}
	
	private void sendErrorResponse(Responder responder, MeetingResponseStatus status) {
		sendResponse(responder, meetingProtocol.encodeErrorResponse(status));
	}
	
	private void sendResponse(Responder responder, Document document) {
		responder.sendResponse("MeetingResponse", document);
	}

	private MeetingHandlerResponse doTheJob(MeetingHandlerRequest meetingRequest, BackendSession bs) 
			throws DaoException, CollectionNotFoundException, ProcessingEmailException {
		
		List<ItemChangeMeetingResponse> meetingResponses =  new ArrayList<ItemChangeMeetingResponse>();
		for (MeetingResponse item : meetingRequest.getMeetingResponses()) {
			ItemChangeMeetingResponse meetingResponse = handleSingleResponse(bs, item);	
			meetingResponses.add(meetingResponse);
		}
		return new MeetingHandlerResponse(meetingResponses);
	}

	private ItemChangeMeetingResponse handleSingleResponse(BackendSession bs,
			MeetingResponse item) throws DaoException,
			CollectionNotFoundException, ProcessingEmailException {
		
		MSEmail email = retrieveMailWithMeetingRequest(bs, item);
		
		ItemChangeMeetingResponse meetingResponse = new ItemChangeMeetingResponse();
		
		if (email != null) {
			meetingResponse.setStatus(MeetingResponseStatus.SUCCESS);
			try {
				AttendeeStatus userResponse = item.getUserResponse();
				String calId = contentsImporter.importCalendarUserStatus(bs, item.getCollectionId(), email, 
						userResponse);
				contentsImporter.importMessageDeletion(bs, PIMDataType.EMAIL, item.getCollectionId(), item.getReqId(), false);
				if (!AttendeeStatus.DECLINE.equals(userResponse)) {
					meetingResponse.setCalId(calId);
				}
			} catch (ServerItemNotFoundException e) {
				logger.error(e.getMessage(), e);
				meetingResponse.setStatus(MeetingResponseStatus.SERVER_ERROR);
			} catch (UnknownObmSyncServerException e) {
				logger.error(e.getMessage(), e);
				meetingResponse.setStatus(MeetingResponseStatus.SERVER_ERROR);
			}
		} else {
			meetingResponse.setStatus(MeetingResponseStatus.INVALID_MEETING_RREQUEST);
		}
		
		meetingResponse.setReqId(item.getReqId());
		return meetingResponse;
	}
	
	private MSEmail retrieveMailWithMeetingRequest(BackendSession bs, MeetingResponse item)
		throws CollectionNotFoundException, ProcessingEmailException {

		MSEmail email = mailBackend.getEmail(bs, item.getCollectionId(), item.getReqId());
		return email;
	}
	
}
