<?php
/*
 +-------------------------------------------------------------------------+
 |  Copyright (c) 1997-2009 OBM.org project members team                   |
 |                                                                         |
 | This program is free software; you can redistribute it and/or           |
 | modify it under the terms of the GNU General Public License             |
 | as published by the Free Software Foundation; version 2                 |
 | of the License.                                                         |
 |                                                                         |
 | This program is distributed in the hope that it will be useful,         |
 | but WITHOUT ANY WARRANTY; without even the implied warranty of          |
 | MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           |
 | GNU General Public License for more details.                            |
 +-------------------------------------------------------------------------+
 | http://www.obm.org                                                      |
 +-------------------------------------------------------------------------+
*/
?>
This email was automatically sent by OBM
------------------------------------------------------------------
Share calendar
------------------------------------------------------------------

<?php echo "$firstname $name"; ?> now allows you to import their calendar OBM.

:: Import ical : 
  <?php echo $host; ?>calendar/calendar_render.php?action=ics_export&externalToken=<?php echo $token; ?>&lastname=<?php echo urlencode($name); ?>&firstname=<?php echo urlencode($firstname); ?>&email=<?php echo urlencode($email); ?>
