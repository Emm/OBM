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

package org.obm.caldav.server.methodHandler;

import javax.servlet.http.HttpServletResponse;

import org.obm.caldav.server.IBackend;
import org.obm.caldav.server.StatusCodeConstant;
import org.obm.caldav.server.exception.CalDavException;
import org.obm.caldav.server.impl.DavRequest;
import org.obm.caldav.server.share.CalDavToken;

public class UnlockHandler extends DavMethodHandler {

	public UnlockHandler() {
	}

	@Override
	public void process(CalDavToken t, IBackend proxy, DavRequest req, HttpServletResponse resp) throws CalDavException {
		logger.info("process(req, resp)");
		throw new CalDavException(StatusCodeConstant.SC_NOT_IMPLEMENTED);
	}

}
