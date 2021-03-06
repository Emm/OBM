<?php
///////////////////////////////////////////////////////////////////////////////
// OBM - File : admin_pref_query.inc                                         //
//     - Desc : Pref admin Query File                                        //
// 2002-08-10 Pierre Baudracco                                               //
///////////////////////////////////////////////////////////////////////////////
// $Id$ //
///////////////////////////////////////////////////////////////////////////////


///////////////////////////////////////////////////////////////////////////////
// Get the default user preferences
// Returns:
//   - $obm_q : DB object of default User Preferences 
///////////////////////////////////////////////////////////////////////////////
function get_admin_pref_default_user_pref() {
  global $cdg_sql;

  $query = "SELECT *
            FROM UserObmPref
            WHERE userobmpref_user_id IS NULL
            ORDER BY userobmpref_option";

  display_debug_msg($query, $cdg_sql, "get_admin_pref_default_user_pref()");
  $obm_q = new DB_OBM;
  $obm_q->query($query);
  
  return $obm_q;
}


///////////////////////////////////////////////////////////////////////////////
// Get the user preference value of the option given
// Parameters:
//   - $option : preference to get
// Returns:
//   - $value : of the preference
///////////////////////////////////////////////////////////////////////////////
function get_admin_pref_userpref_value($option) {
  global $obm, $cdg_sql;

  $uid = sql_parse_id( $obm["uid"], true);
  $query = "SELECT userobmpref_value
            FROM UserObmPref
            WHERE userobmpref_user_id  $uid 
              AND userobmpref_option = '$option'";

  display_debug_msg($query, $cdg_sql, "get_admin_pref_userpref_value()");
  $obm_q = new DB_OBM;
  $obm_q->query($query);
  $obm_q->next_record();
  
  return $obm_q->f("userobmpref_value");
}


///////////////////////////////////////////////////////////////////////////////
// Admin Preferences Form Data checking and formatting
// Parameters:
//   - $pref[]   : values checked
// Returns:
//   - (true | false) : true if data are ok, else false 
///////////////////////////////////////////////////////////////////////////////
function check_admin_pref_data_form($pref) {

  // Future use to check if user parameter valid ? XXXXX

  return true; 
}

?>
