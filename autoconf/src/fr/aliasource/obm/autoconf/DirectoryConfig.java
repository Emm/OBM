package fr.aliasource.obm.autoconf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fr.aliasource.obm.utils.ConstantService;

/**
 * Loads configuration informations from the {@link ConstantService}
 * 
 * @author tom
 * 
 */
public class DirectoryConfig {

	private Integer ldapPort;
	private String ldapSearchBase;
	private String[] ldapAtts;
	private String ldapFilter;
	private String searchWithDomain ;
	private String ldapHost;
	private String configXml;

	private static final Log logger = LogFactory.getLog(DirectoryConfig.class);
	
	public DirectoryConfig(String login, String domain ,ConstantService cs) {
		searchWithDomain = cs.getStringValue("searchWithDomain");
		ldapHost = cs.getStringValue("ldapHost");
		ldapPort = cs.getIntValue("ldapPort");
		ldapSearchBase = cs.getStringValue("ldapSearchBase");
		ldapAtts = cs.getStringValue("ldapAtts").split(",");
		if ( "true".equals(searchWithDomain)) {
			logger.info("DirectoryConfig : search with domain'");
			login = login + "@" + domain ;
		}
		ldapFilter = "(" + cs.getStringValue("ldapFilter") + "=" + login + ")";
		configXml = "/usr/share/obm-autoconf/config.xml";
	}

	public int getLdapPort() {
		return ldapPort;
	}

	public String getLdapSearchBase() {
		return ldapSearchBase;
	}

	public String[] getLdapAtts() {
		return ldapAtts;
	}

	public String getLdapFilter() {
		return ldapFilter;
	}

	public String getLdapHost() {
		return ldapHost;
	}

	public String getConfigXml() {
		return configXml;
	}

	public void setConfigXml(String configXml) {
		this.configXml = configXml;
	}

}
