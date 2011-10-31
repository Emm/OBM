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
package org.obm.push.protocol;

import java.util.ArrayList;

import javax.xml.parsers.FactoryConfigurationError;

import org.obm.push.bean.FilterType;
import org.obm.push.bean.GetItemEstimateStatus;
import org.obm.push.bean.SyncCollection;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.protocol.bean.GetItemEstimateRequest;
import org.obm.push.protocol.bean.GetItemEstimateResponse;
import org.obm.push.protocol.bean.GetItemEstimateResponse.Estimate;
import org.obm.push.utils.DOMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GetItemEstimateProtocol {

	public GetItemEstimateRequest getRequest(Document doc) throws CollectionNotFoundException {
		final NodeList collections = doc.getDocumentElement().getElementsByTagName("Collection");
		int nbElements = collections.getLength();
		final ArrayList<SyncCollection> syncCollections = new ArrayList<SyncCollection>(nbElements);
		for (int i = 0; i < nbElements; i++) {
	
			final Element ce = (Element) collections.item(i);
			final String dataClass = DOMUtils.getElementText(ce, "Class");
			final String filterType = DOMUtils.getElementText(ce, "FilterType");
			final String syncKey = DOMUtils.getElementText(ce, "SyncKey");
			final Element fid = DOMUtils.getUniqueElement(ce, "CollectionId");
			final String collectionId = fid.getTextContent();
	
			final SyncCollection sc = new SyncCollection();
			sc.setDataClass(dataClass);
			sc.setSyncKey(syncKey);
			SyncCollectionOptions options = new SyncCollectionOptions();
			options.setFilterType(FilterType.getFilterType(filterType));
			sc.setOptions(options);
			try {
				sc.setCollectionId(Integer.valueOf(collectionId));
			} catch (NumberFormatException e) {
				throw new CollectionNotFoundException(e);
			}
			syncCollections.add(sc);
		}
		return new GetItemEstimateRequest(syncCollections);
	}

	public Document encodeResponse(GetItemEstimateResponse response) {
		final Document document = createDocument();
		for (Estimate estimate: response.getEstimates()) {
			final Element responseElement = createResponseNode(document);
			DOMUtils.createElementAndText(responseElement, "Status", GetItemEstimateStatus.OK.asXmlValue());
			final Element collectionElement = DOMUtils.createElement(responseElement, "Collection");
			createCollectionIdElement(estimate.getCollection(), collectionElement);
			createEstimateElement(estimate.getEstimate(), collectionElement);
		}
		return document;
	}

	private Document createDocument() throws FactoryConfigurationError {
		return DOMUtils.createDoc(null, "GetItemEstimate");
	}

	private Element createResponseNode(final Document document) {
		final Element rootElement = document.getDocumentElement();
		final Element responseElement = DOMUtils.createElement(rootElement, "Response");
		return responseElement;
	}

	
	private void createCollectionIdElement(SyncCollection syncCollection, Element collectionElement) {
		DOMUtils.createElementAndText(collectionElement, "CollectionId",
				syncCollection.getCollectionId().toString());
	}

	private void createEstimateElement(int estimate, Element collectionElement) {
		Element estim = DOMUtils.createElement(collectionElement, "Estimate");
		estim.setTextContent(String.valueOf(estimate));
	}

	public Document buildError(GetItemEstimateStatus status, Integer collectionId) {
		Document document = createDocument();
		Element responseNode = createResponseNode(document);
		DOMUtils.createElementAndText(responseNode, "Status", status.asXmlValue());
		if (collectionId != null) {
			Element ce = DOMUtils.createElement(responseNode, "Collection");
			DOMUtils.createElementAndText(ce, "CollectionId", String.valueOf(collectionId));
		}
		return document;
	}

	
}
