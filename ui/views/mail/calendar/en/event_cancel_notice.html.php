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
<table style="width:80%; border:1px solid #000; border-collapse:collapse;background:#EFF0F2;font-size:12px;">
    <tr>
        <th style="text-align:center; background-color: #509CBC; color:#FFF; font-size:14px" colspan="2">
          Appoitment Deleted !
        </th>
    </tr>
    <tr>
        <td colspan="2">The appointment <?php echo $location; ?>, initially scheduled on <?php echo $start; ?> to <?php echo $end; ?> (location: <?php echo $location; ?>),
has been deleted.</td>
    </tr>
    <tr>
        <td colspan="2">
          <strong>NB : </strong>If you're using the Thunderbird extension or ActiveSync, you must synchronize to view this deletion.
        </td>
    </tr>
</table>
