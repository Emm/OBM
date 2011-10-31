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

package org.minig.imap.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.minig.imap.SearchQuery;
import org.minig.imap.impl.IMAPResponse;

public class UIDSearchCommand extends Command<Collection<Long>> {

	// Mon, 7 Feb 1994 21:52:25 -0800

	private SearchQuery sq;

	public UIDSearchCommand(SearchQuery sq) {
		this.sq = sq;
	}

	@Override
	protected CommandArgument buildCommand() {
		String cmd = "UID SEARCH NOT DELETED";
		if (sq.getAfter() != null) {
			DateFormat df = new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH);
			cmd += " NOT BEFORE " + df.format(sq.getAfter());
		}
		if (sq.getBefore() != null) {
			DateFormat df = new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH);
			cmd += " BEFORE " + df.format(sq.getBefore());
		}
		
		// logger.info("cmd "+cmd);
		CommandArgument args = new CommandArgument(cmd, null);
		return args;
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		data = Collections.emptyList();

		IMAPResponse ok = rs.get(rs.size() - 1);
		if (ok.isOk()) {
			String uidString = null;
			Iterator<IMAPResponse> it = rs.iterator();
			for (int j = 0; j < rs.size() - 1; j++) {
				String resp = it.next().getPayload();
				if (resp.startsWith("* SEARCH ")) {
					uidString = resp;
					break;
				}
			}

			if (uidString != null) {
				// 9 => '* SEARCH '.length
				String[] splitted = uidString.substring(9).split(" ");
				final List<Long> result = new ArrayList<Long>(splitted.length);
				for (int i = 0; i < splitted.length; i++) {
					result.add(Long.parseLong(splitted[i]));
				}
				data = result;
			}
		}
	}

}
