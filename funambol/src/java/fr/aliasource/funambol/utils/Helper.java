package fr.aliasource.funambol.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.funambol.common.pim.calendar.Calendar;
import com.funambol.common.pim.calendar.Event;
import com.funambol.framework.engine.SyncItem;

/**
 * @author Lascombes
 *
 */
public class Helper {

	public static final int RESTRICT_PRIVATE = 1;
	public static final int RESTRICT_OWNER 	= 2;
	public static final int RESTRICT_REFUSED = 4;
	
	public Helper() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	
	/**
	 * Repalce a null or "null" string by "" or return the same string
	 * @param str
	 * @return
	 */
	public static String nullToEmptyString(String str) {

	    String result = null;

	    if ((str == null) || (str.equalsIgnoreCase("null"))) {
	        result = "";
	    } else {
	        result = str;
	    }

	    return result;
	}


	public static String getContentOfSyncItem(SyncItem item, boolean encode) {
		
		String result = null;
		
	    // get content of the item
	
	    byte[] itemContent = item.getContent();
	    if (itemContent == null) {
	        itemContent = new byte[0];
	    }
	    result = new String(itemContent);
	    
	    if (encode &&
	    	result != null &&
	        result.length() > 0) {
	    	result = new String(com.funambol.framework.tools.Base64.decode(result));
	    }
		return result;
	}


	public static String getPriority(int priority) {
		return ""+(priority-1);
	}

	public static Integer getPriorityFromFoundation(String priority) {
		if ( !nullToEmptyString(priority).equals("") ) {
			Integer value = new Integer(priority + 1);
			return value;
		} else {
			return new Integer(1);
		}
		
	}


	public static Short nullToZero(Short value) {
		if ( ! (value == null) ) {			
			return value;
		} else {
			return new Short((short) 0);
		}
	}

	public static String[] listToTab(List list) {
		String[] result = (String[]) list.toArray(new String[0]);
		return result;
	}


	public static String[] setToTab(Set set) {
		String[] result = (String[]) set.toArray(new String[0]);
		return result;
	}

}
