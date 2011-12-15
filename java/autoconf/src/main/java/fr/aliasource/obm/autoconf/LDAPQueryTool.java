package fr.aliasource.obm.autoconf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;

public class LDAPQueryTool {
	private static final Logger logger = LoggerFactory.getLogger(LDAPQueryTool.class);

	private DirectoryConfig dc;

	public LDAPQueryTool(DirectoryConfig dc) {
		this.dc = dc;
	}

	LDAPAttributeSet getLDAPInformations() throws LDAPException {
		LDAPConnection ld = new LDAPConnection();
		LDAPSearchResults searchResults;
		try {
			ld.connect(dc.getLdapHost(), dc.getLdapPort());
			searchResults = ld.search(dc.getLdapSearchBase(),
					LDAPConnection.SCOPE_SUB, dc.getLdapFilter(), dc
							.getLdapAtts(), false);
			if (searchResults.hasMore()) {
			LDAPEntry nextEntry = searchResults.next();
			LDAPAttributeSet attributeSet = nextEntry.getAttributeSet();
			return attributeSet;
			} else {
				return null;
			}
		} catch (LDAPException e) {
			logger.error("Error finding user info", e);
			throw e;
		} finally {
			try {
				ld.disconnect();
			} catch (LDAPException e) {
			}
		}
	}

}
