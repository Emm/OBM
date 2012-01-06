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
 *   MiniG.org project members
 *
 * ***** END LICENSE BLOCK ***** */

package org.minig.imap.command;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.minig.imap.Address;
import org.minig.imap.EncodedWord;
import org.minig.imap.Envelope;
import org.minig.imap.impl.DateParser;
import org.minig.imap.impl.IMAPResponse;
import org.minig.imap.impl.MessageSet;
import org.minig.imap.mime.impl.AtomHelper;
import org.minig.imap.mime.impl.ParenListParser;
import org.minig.imap.mime.impl.ParenListParser.TokenType;

public class UIDFetchEnvelopeCommand extends BatchCommand<Envelope> {

	private Collection<Long> uids;

	public UIDFetchEnvelopeCommand(Collection<Long> uid) {
		this.uids = uid;
	}

	@Override
	protected CommandArgument buildCommand() {
		StringBuilder sb = new StringBuilder();
		if (!uids.isEmpty()) {
			sb.append("UID FETCH ");
			sb.append(MessageSet.asString(uids));
			sb.append(" (UID ENVELOPE)");
		} else {
			sb.append("NOOP");
		}
		String cmd = sb.toString();
		if (logger.isDebugEnabled()) {
			logger.debug("cmd: " + cmd);
		}
		CommandArgument args = new CommandArgument(cmd, null);
		return args;
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) throws ImapException {
		if (uids.isEmpty()) {
			data = Collections.emptyList();
			return;
		}

		checkStatusResponse(rs);
		
		List<ImapReturn<Envelope>> tmp = new ArrayList<ImapReturn<Envelope>>(uids.size());
		for (IMAPResponse r: rs) {
			try {
				Envelope envelope = parseEnvelopeFromResponse(r);
				if (envelope != null) {
					tmp.add(value(envelope));
				}
			} catch (RuntimeException e) {
				tmp.add(error(e));
			} catch (ParseException e) {
				tmp.add(error(e));
			}
		}
		data = tmp;
	}

	private Envelope parseEnvelopeFromResponse(IMAPResponse r) throws ParseException {
		String payload = r.getPayload();
		if (!payload.contains(" FETCH")) {
			logger.warn("not a fetch: " + payload);
			return null;
		}
		int uidIdx = payload.indexOf("(UID ") + "(UID ".length();
		int endUid = payload.indexOf(' ', uidIdx);
		String uidStr = payload.substring(uidIdx, endUid);
		long uid = Long.parseLong(uidStr);

		String envel = payload.substring(
				endUid + " ENVELOPE (".length(), payload.length());

		String envelData = AtomHelper.getFullResponse(envel,
				r.getStreamData());

		Envelope envelope = parseEnvelope(envelData.substring(0, envelData.length() - 1).getBytes());
		envelope.setUid(uid);
		logger.info("uid: {} env.from: {}", uid, envelope.getFrom());
		return envelope;
	}
	
	/**
	 * <pre>
	 * (        
	 *          "Tue, 19 Jan 2010 09:11:54 +0100" 
	 * 		     "Pb =?ISO-8859-1?Q?r=E9plication_annuaire_ldap?=" 
	 * 		     ( // FROM
	 * 		         ("Raymond Barthe" NIL "raymond.barthe" "cg82.fr")
	 * 		     ) 
	 * 		     ( // SENDER
	 * 		         ("Raymond Barthe" NIL "raymond.barthe" "cg82.fr")
	 * 		     ) 
	 * 		     ( // REPLY TO
	 * 		         ("Raymond Barthe" NIL "raymond.barthe" "cg82.fr")
	 * 		     ) 
	 * 		     ( // TO
	 * 		         (NIL NIL "aliasource" "buffle.tlse.lng:support")
	 * 		         (NIL NIL "support" "aliasource.fr")
	 * 		     ) 
	 * 		     ( // CC
	 * 		         (NIL NIL "admin.info" "cg82.fr")
	 * 		         ("OLIVIER MOLINA" NIL "olivier.molina" "cg82.fr")
	 * 		         ("aliasource anthony prades" NIL "anthony.prades" "aliasource.fr")
	 * 		     ) 
	 * 		     NIL // BCC
	 * 		     NIL  // IN REPLY TO
	 * 		     "<4B55694A.3000106@cg82.fr>"
	 * 		)
	 * </pre>
	 **/
	private Envelope parseEnvelope(byte[] env) throws ParseException {
		ParenListParser parser = new ParenListParser();
		int pos = 0;

		pos = parser.consumeToken(pos, env);
		String date = new String(parser.getLastReadToken());
		Date d = DateParser.parse(date);

		pos = parser.consumeToken(pos, env);
		String subject = "[Empty subject]";
		if (parser.getLastTokenType() == TokenType.STRING
				|| parser.getLastTokenType() == TokenType.ATOM) {
			subject = EncodedWord.decode(new String(parser.getLastReadToken()))
					.toString();
		}

		// FROM
		pos = parser.consumeToken(pos, env); // (("Raymon" NIL "ra" "cg82.fr"))
		List<Address> from = null;
		if (parser.getLastTokenType() == TokenType.LIST) {
			from = parseList(parser.getLastReadToken(), parser);
		}

		pos = parser.consumeToken(pos, env); // sender
		pos = parser.consumeToken(pos, env); // reply to

		// TO
		pos = parser.consumeToken(pos, env); // (("Raymon" NIL "ra" "cg82.fr"))
		List<Address> to = parseList(parser.getLastReadToken(), parser);

		// CC
		pos = parser.consumeToken(pos, env); // (("Raymon" NIL "ra" "cg82.fr"))
		List<Address> cc = parseList(parser.getLastReadToken(), parser);

		pos = parser.consumeToken(pos, env); // (("Raymon" NIL "ra" "cg82.fr"))
		List<Address> bcc = parseList(parser.getLastReadToken(), parser);

		pos = parser.consumeToken(pos, env); // In-Reply-To
		String inReplyTo = null;
		if (parser.getLastTokenType() != TokenType.NIL) {
			inReplyTo = new String(parser.getLastReadToken());
		}

		parser.consumeToken(pos, env); // Message-ID
		String mid = new String(parser.getLastReadToken());
		
		Address address = null;
		if (from != null && from.size() > 0) {
			address = from.get(0);
		} else {
			address = new Address();
		}
		return new Envelope(d, subject, to, cc, bcc, address, mid, inReplyTo);
	}

	private List<Address> parseList(byte[] token, ParenListParser parser) {
		LinkedList<Address> ret = new LinkedList<Address>();

		if (parser.getLastTokenType() != TokenType.LIST) {
			return ret;
		}

		int pos = 0;
		do {
			pos = parser.consumeToken(pos, token);
			byte[] parts = parser.getLastReadToken();
			int p = 0;
			p = parser.consumeToken(p, parts);
			String displayName = null;
			if (parser.getLastTokenType() == TokenType.STRING) {
				displayName = EncodedWord.decode(
						new String(parser.getLastReadToken())).toString();
			}
			p = parser.consumeToken(p, parts);
			p = parser.consumeToken(p, parts);
			String left = new String(parser.getLastReadToken());
			p = parser.consumeToken(p, parts);
			String right = new String(parser.getLastReadToken());
			Address ad = new Address(displayName, left + "@" + right);
			ret.add(ad);
		} while (pos < token.length);
		return ret;
	}

}
