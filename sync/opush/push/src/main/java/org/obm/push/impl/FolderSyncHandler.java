package org.obm.push.impl;

import java.sql.SQLException;
import java.util.List;

import org.obm.annotations.transactional.Transactional;
import org.obm.push.backend.BackendSession;
import org.obm.push.backend.IBackend;
import org.obm.push.backend.IContentsExporter;
import org.obm.push.backend.IContentsImporter;
import org.obm.push.backend.IContinuation;
import org.obm.push.backend.IHierarchyExporter;
import org.obm.push.bean.FolderSyncRequest;
import org.obm.push.bean.FolderSyncResponse;
import org.obm.push.bean.ItemChange;
import org.obm.push.bean.SyncState;
import org.obm.push.data.EncoderFactory;
import org.obm.push.exception.ActiveSyncException;
import org.obm.push.exception.CollectionNotFoundException;
import org.obm.push.exception.InvalidSyncKeyException;
import org.obm.push.exception.NoDocumentException;
import org.obm.push.protocol.FolderSyncProtocol;
import org.obm.push.state.StateMachine;
import org.obm.push.store.CollectionDao;
import org.w3c.dom.Document;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FolderSyncHandler extends WbxmlRequestHandler {

	private final IHierarchyExporter hierarchyExporter;
	private final FolderSyncProtocol protocol;
	
	@Inject
	protected FolderSyncHandler(IBackend backend, EncoderFactory encoderFactory,
			IContentsImporter contentsImporter, IHierarchyExporter hierarchyExporter,
			IContentsExporter contentsExporter, StateMachine stMachine,
			CollectionDao collectionDao, FolderSyncProtocol protocol) {
		
		super(backend, encoderFactory, contentsImporter,
				contentsExporter, stMachine, collectionDao);
		
		this.hierarchyExporter = hierarchyExporter;
		this.protocol = protocol;
	}

	@Override
	public void process(IContinuation continuation, BackendSession bs,
			Document doc, ActiveSyncRequest request, Responder responder) {
		
		try {
			FolderSyncRequest folderSyncRequest = protocol.getRequest(doc);
			FolderSyncResponse folderSyncResponse = doTheJob(bs, folderSyncRequest);
			Document ret = protocol.encodeResponse(folderSyncResponse);
			responder.sendResponse("FolderHierarchy", ret);
			
		} catch (CollectionNotFoundException e) {
			logger.error(e.getMessage(), e);
			protocol.sendError(responder, FolderSyncStatus.INVALID_SYNC_KEY);
		} catch (InvalidSyncKeyException e) {
			logger.error(e.getMessage(), e);
			protocol.sendError(responder, FolderSyncStatus.INVALID_SYNC_KEY);
		} catch (NoDocumentException e) {
			protocol.answerOpushIsAlive(responder);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	@Transactional
	private FolderSyncResponse doTheJob(BackendSession bs,
			FolderSyncRequest folderSyncRequest) throws SQLException,
			InvalidSyncKeyException, CollectionNotFoundException,
			ActiveSyncException {
		
		// FIXME we know that we do not monitor hierarchy, so just respond
		// that nothing changed
		
		SyncState state = stMachine.getFolderSyncState(bs.getLoginAtDomain(), bs.getDevId(),
				hierarchyExporter.getRootFolderUrl(bs), folderSyncRequest.getSyncKey());
		
		if (!state.isValid()) {
			throw new InvalidSyncKeyException();
		}
		hierarchyExporter.configure(state, null, null, 0, 0);
								
		List<ItemChange> changed = hierarchyExporter.getChanged(bs);
		
		if (!isSynckeyValid(folderSyncRequest.getSyncKey(), changed)) {
			throw new InvalidSyncKeyException();
		}
		
		String newSyncKey = stMachine.allocateNewSyncKey(bs,
				hierarchyExporter.getRootFolderId(bs), null);
		
		FolderSyncResponse folderSyncResponse = new FolderSyncResponse(changed, newSyncKey);
		return folderSyncResponse;
	}

	private boolean isSynckeyValid(String syncKey, List<ItemChange> changed) {
		if (!"0".equals(syncKey)) {
			for (ItemChange sf : changed) {
				if (sf.isNew()) {
					return false;
				}
			}
		}
		return true;
	}
	
}
