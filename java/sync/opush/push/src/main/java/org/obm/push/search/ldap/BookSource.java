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
package org.obm.push.search.ldap;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.obm.push.bean.BackendSession;
import org.obm.push.bean.SearchResult;
import org.obm.push.bean.StoreName;
import org.obm.push.search.ISearchSource;
import org.obm.push.utils.LdapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BookSource implements ISearchSource {

	private final static Logger logger = LoggerFactory.getLogger(BookSource.class);
	private final Configuration conf;
	
	@Inject
	private BookSource(Configuration configuration) {
		this.conf = configuration;
	}

	private String uniqueAttribute(String string, Map<String, List<String>> m) {
		List<String> cnl = m.get(string);
		if (cnl == null || cnl.isEmpty()) {
			return "";
		} else {
			return cnl.get(0);
		}
	}

	@Override
	public StoreName getStoreName() {
		return StoreName.GAL;
	}

	@Override
	public List<SearchResult> search(BackendSession bs, String query,
			Integer limit) {
		List<SearchResult> ret = new LinkedList<SearchResult>();
		if (conf.isValid()) {
			DirContext ctx = null;
			String domain = "";
			int idx = bs.getLoginAtDomain().indexOf("@");
			if (idx > 0) {
				domain = bs.getLoginAtDomain().substring(idx + 1);
			}
			try {
				ctx = conf.getConnection();
				LdapUtils u = new LdapUtils(ctx, conf.getBaseDn().replaceAll(
						"%d", domain));
				List<Map<String, List<String>>> l = u.getAttributes(
						conf.getFilter(), query, new String[] { "displayName",
								"cn", "sn", "givenName", "mail",
								"telephoneNumber", "mobile" });
				l = l.subList(0, Math.min(limit, l.size()));
				for (Map<String, List<String>> m : l) {
					String sn = uniqueAttribute("sn", m);
					String givenName = uniqueAttribute("givenName", m);
					String cn = uniqueAttribute("cn", m);
					String display = uniqueAttribute("displayName", m);
					List<String> phones = m.get("telephoneNumber");
					if (sn.length() == 0 || givenName.length() == 0) {
						sn = cn;
						givenName = "";
					}
					SearchResult sr = new SearchResult();
					if (display != null && display.length() > 0) {
						sr.setDisplayName(display);
					} else {
						sr.setDisplayName(givenName + " " + sn);
					}
					sr.setLastName(sn);
					sr.setFirstName(givenName);
					if (phones != null) {
						if (phones.size() > 0) {
							sr.setPhone(phones.get(0));
						}
						if (phones.size() > 1) {
							sr.setHomePhone(phones.get(1));
						}
					}
					sr.setMobilePhone(uniqueAttribute("mobile", m));
					List<String> mails = m.get("mail");
					if (mails != null && mails.iterator().hasNext()) {
						sr.setEmailAddress(mails.iterator().next());
					}
					ret.add(sr);
				}
			} catch (NamingException e) {
				logger.error("findAll error", e);
			} finally {
				conf.cleanup(ctx);
			}
		}
		return ret;
	}
}
