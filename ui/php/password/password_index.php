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
<script language="php">
///////////////////////////////////////////////////////////////////////////////
// OBM - File : password.php                                                 //
//     - Desc : OBM Password Index File                                      //
// 2004-03-25 Pierre Baudracco                                               //
///////////////////////////////////////////////////////////////////////////////
// $Id$
///////////////////////////////////////////////////////////////////////////////
// Actions :
// - index        --           -- show the Password form
// - update       -- $password -- update the User Password
///////////////////////////////////////////////////////////////////////////////

$path = "..";
$module = "password";
$obminclude = getenv("OBM_INCLUDE_VAR");
if ($obminclude == "") $obminclude = "obminclude";
include("$obminclude/global.inc");
$params = get_password_params();
page_open(array("sess" => "OBM_Session", "auth" => $auth_class_name, "perm" => "OBM_Perm"));
include("$obminclude/global_pref.inc");
require("password_display.inc");
require("password_query.inc");

if ($action == "") $action = "index";
get_password_action();
$perm->check_permissions($module, $action);


if (($action == "index") || ($action == "")) {
///////////////////////////////////////////////////////////////////////////////
  $obm_q = run_query_password($obm["uid"]);
  if ($obm_q->num_rows() == 1) {
    $display["detail"] = html_password_form($obm_q, $params);
  } else {
    $display["msg"] .= display_err_msg($l_err_reference);
  }

} elseif ($action == "update")  {
///////////////////////////////////////////////////////////////////////////////
  $obm_q = run_query_password($obm["uid"]);
  if (check_user_defined_rules() && check_data_form($obm_q, $params)) {
    $retour = run_query_password_update($params, $obm["uid"], $obm_q);
    if ($retour) {
      $display["msg"] .= display_ok_msg("$l_password : $l_update_ok");
      $obm_q = run_query_password($obm["uid"]);
      $display["detail"] = html_password_form($obm_q, $params);
    } else {
      $display["msg"] = display_err_msg("$l_password : $l_update_error");
      $display["detail"] = html_password_form($obm_q, $params);
    }
  } else {
    $display["msg"] = display_err_msg("$l_password : $err[msg]");
    $display["detail"] = html_password_form($obm_q, $params, $err["field"]);
  }
}


///////////////////////////////////////////////////////////////////////////////
// Display page
///////////////////////////////////////////////////////////////////////////////
$display["head"] = display_head($l_password);
if (! $params["popup"]) {
  $display["header"] = display_menu($module);
}
$display["end"] = display_end();

display_page($display);


///////////////////////////////////////////////////////////////////////////////
// Stores Password parameters transmited in $params hash
// returns : $params hash with parameters set
///////////////////////////////////////////////////////////////////////////////
function get_password_params() {

  // Get global params
  $params = get_global_params("Password");

  return $params;
}


///////////////////////////////////////////////////////////////////////////////
// Password Action 
///////////////////////////////////////////////////////////////////////////////
function get_password_action() {
  global $params, $actions, $path;
  global $cright_read, $cright_write, $cright_read_admin, $cright_write_admin;
  global $l_module_password;

  // Password Index
  $actions["password"]["index"] = array (
    'Name'     => $l_module_password,
    'Url'      => "$path/password/password_index.php?action=index",
    'Right'    => $cright_read,
    'Condition'=> array ('all') 
                                    );

  // Password Update
  $actions["password"]["update"] = array (
    'Right'    => $cright_read,
    'Condition'=> array ('none') 
                                    );

}

</script>
