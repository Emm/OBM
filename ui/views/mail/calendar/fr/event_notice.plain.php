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
NOUVEAU RENDEZ-VOUS !
------------------------------------------------------------------

Vous êtes invité(e) à participer à ce rendez-vous

du     : <?php echo $start; ?>

au     : <?php echo $end; ?>

sujet  : <?php echo $title; ?>

lieu   : <?php echo $location; ?>

auteur : <?php echo $auteur; ?>

participant(s) : <?php echo $attendees; ?>

::NB : Si vous êtes utilisateur du connecteur Thunderbird ou de la synchronisation ActiveSync, vous devez synchroniser pour visualiser ce nouveau rendez-vous.

:: Pour plus de détails : 
<?php echo $this->host; ?>calendar/calendar_index.php?action=detailconsult&calendar_id=<?php echo $id; ?>
