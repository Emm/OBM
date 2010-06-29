/* ***** BEGIN LICENSE BLOCK *****
 * Version: GPL 2.0
 *
 * The contents of this file are subject to the GNU General Public
 * License Version 2 or later (the "GPL").
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Initial Developer of the Original Code is
 *   obm.org project members
 *
 * ***** END LICENSE BLOCK ***** */

package org.obm.caldav.server.resultBuilder;

import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.obm.caldav.server.IBackend;
import org.obm.caldav.server.impl.DavRequest;
import org.obm.caldav.server.propertyHandler.PropfindPropertyHandler;
import org.obm.caldav.server.share.DavComponent;
import org.obm.caldav.server.share.CalDavToken;
import org.obm.caldav.utils.DOMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class PropertyListBuilder extends ResultBuilder {

	private Log logger = LogFactory.getLog(PropertyListBuilder.class);

	public Document build(CalDavToken t, DavRequest req, List<DavComponent> comps,
			Set<PropfindPropertyHandler> toLoad, Set<Element> notUsed,
			IBackend proxy) {
		try {

			Document ret = createMultiStatusDocument();
			Element r = ret.getDocumentElement();
			for (DavComponent comp : comps) {
				Element response = DOMUtils.createElement(r, "D:response");
				DOMUtils.createElementAndText(response, "D:href", comp.getURL());

				if (toLoad.size() > 0) {
					Element pStat = DOMUtils.createElement(response,
							"D:propstat");
					Element p = DOMUtils.createElement(pStat, "D:prop");
					for (PropfindPropertyHandler dph : toLoad) {
						dph.appendPropertyValue(p, t, req, proxy, comp);
					}
					DOMUtils.createElementAndText(pStat, "D:status",
							"HTTP/1.1 200 OK");
				}

				if (notUsed.size() > 0) {
					Element pStatNotUsed = DOMUtils.createElement(response,
							"D:propstat");
					Element pNotUsed = DOMUtils.createElement(pStatNotUsed,
							"D:prop");
					for (Element enu : notUsed) {
						DOMUtils.createElement(pNotUsed, enu.getNamespaceURI(),
								enu.getTagName());
					}
					DOMUtils.createElementAndText(pStatNotUsed, "D:status",
							"HTTP/1.1 404 Not Found");
				}
			}

			return ret;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}

	}

}
