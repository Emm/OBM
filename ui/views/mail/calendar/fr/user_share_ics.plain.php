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
Message automatique envoyé par OBM
------------------------------------------------------------------
Partage d'agenda
------------------------------------------------------------------

Vous pouvez importer au format ics l'agenda OBM de <?php echo "$firstname $name"; ?>

:: Pour importer : 
  <?php echo $host; ?>calendar/calendar_render.php?action=ics_export&externalToken=<?php echo $token; ?>
