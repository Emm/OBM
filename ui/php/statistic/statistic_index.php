<script language="php">
///////////////////////////////////////////////////////////////////////////////
// OBM - File : statistic_index.php                                          //
//     - Desc : Statistic Index File                                         //
// 2004-04-19 Rande Mehdi                                                    //
///////////////////////////////////////////////////////////////////////////////
// $Id$ //
///////////////////////////////////////////////////////////////////////////////
// Actions :
// - index (default)    -- search fields  -- show the statistic search form
///////////////////////////////////////////////////////////////////////////////

$path = "..";
$module = "statistic";
$obminclude = getenv("OBM_INCLUDE_VAR");
if ($obminclude == "") $obminclude = "obminclude";
include("$obminclude/global.inc");
page_open(array("sess" => "OBM_Session", "auth" => $auth_class_name, "perm" => "OBM_Perm"));
include("$obminclude/global_pref.inc");
require("statistic_display.inc");
require("statistic_query.inc");
 

$uid = $auth->auth["uid"];

if ($action == "") $action = "index";
$statistic = get_param_statistic();
get_statistic_action();
$perm->check_permissions($module, $action);

page_close();

if (! $statistic["popup"]) {
  $display["header"] = display_menu($module);
}

///////////////////////////////////////////////////////////////////////////////
// Main Program                                                              //
///////////////////////////////////////////////////////////////////////////////

if ($action == "index" || $action == "") {
///////////////////////////////////////////////////////////////////////////////
  $display["features"] = dis_menu_stats(); 

} elseif ($action == "responsible") {
///////////////////////////////////////////////////////////////////////////////
  $cont_q = run_query_contact_per_resp();
  $comp_q = run_query_company_per_resp();
  $display["title"] = display_title($l_header_resp_stats);
  $display["detail"] = dis_resp_stats($cont_q,$comp_q);
  $display["features"] = dis_menu_stats(); 

} elseif ($action == "company") {
///////////////////////////////////////////////////////////////////////////////
  $list_q = run_query_get_lists();
  $display["detail"] = dis_list_select($list_q);
  $display["features"] = dis_menu_stats();
  $display["title"] = display_title($l_header_comp_stats);
} elseif ($action == "company_statistic") {
///////////////////////////////////////////////////////////////////////////////  
  require("statistic_js.inc");
  if($statistic["list"] == $c_all) {
    $cat_q = run_query_company_per_country_per_cat();
    $nb_comp = run_query_nb_company();
    $display["title"] = display_title($l_header_comp_stats);
  }
  else {
    $obm_q = run_query_get_list($statistic["list"]);
    $query = stripslashes($obm_q->f("list_query"));
    $com_q = run_query_get_selected_company($query,$statistic["list"]);
    $cat_q = run_query_selected_company_per_country_per_cat($com_q);
    $nb_comp = $com_q->nf();
    $display["title"] = display_title("$l_header_comp_stats : ".$obm_q->f("list_name"));
  }
  $display["detail"] = dis_cat_stats($cat_q, $nb_comp);
  $display["features"] = dis_menu_stats(); 
} elseif ($action == "company_statistic_export") {
///////////////////////////////////////////////////////////////////////////////  
  if($statistic["list"] == $c_all) {
    $cat_q = run_query_company_per_country_per_cat();
    $nb_comp = run_query_nb_company();
  }
  else {
    $obm_q = run_query_get_list($statistic["list"]);
    $query = $obm_q->f("list_query");
    $com_q = run_query_get_selected_company($query,$statistic["list"]);
    $cat_q = run_query_selected_company_per_country_per_cat($com_q);
    $nb_comp = $com_q->nf();
  }
  export_cat_stats($cat_q, $nb_comp);
}
///////////////////////////////////////////////////////////////////////////////
// Display
///////////////////////////////////////////////////////////////////////////////
$display["head"] = display_head($l_statistic);
$display["end"] = display_end();

display_page($display);


///////////////////////////////////////////////////////////////////////////////
// Stores Statistic parameters transmited in $statistic hash
// returns : $statistic hash with parameters set
///////////////////////////////////////////////////////////////////////////////
function get_param_statistic() {
  global $action,$sel_list;
  global $HTTP_POST_VARS,$HTTP_GET_VARS;

  if (isset ($sel_list)) $statistic["list"] = $sel_list;  

  return $statistic;
}


///////////////////////////////////////////////////////////////////////////////
//  Statistic Action 
///////////////////////////////////////////////////////////////////////////////
function get_statistic_action() {
  global $cright_read, $cright_write,$cright_admin_read,$cright_admin_write;
  global $path,$actions,$statistic;
  global $ico_contact,$ico_company;
  global $l_header_resp_stats,$l_header_comp_stats,$l_header_index,$l_header_export;

// Index
  $actions["statistic"]["index"] = array (
    'Name'     => $l_header_index,
    'Url'      => "$path/statistic/statistic_index.php?action=index",
    'Right'    => $cright_read,
    'Condition'=> array ('all') 
                                        );
// Index
  $actions["statistic"]["responsible"] = array (
    'Name'     => $l_header_resp_stats,
    'Url'      => "$path/statistic/statistic_index.php?action=responsible",
    'Ico'      => $ico_contact,
    'Right'    => $cright_read,
    'Condition'=> array ('content') 
                                        );
					
// 
  $actions["statistic"]["company"] = array (
    'Name'     => $l_header_comp_stats,
    'Url'      => "$path/statistic/statistic_index.php?action=company",
    'Ico'      => $ico_company,
    'Right'    => $cright_read,
    'Condition'=> array ('content') 
                                        );

// 
  $actions["statistic"]["company_statistic"] = array (
    'Url'      => "$path/statistic/statistic_index.php?action=company_statistic",
    'Right'    => $cright_read,
    'Condition'=> array ('None') 
                                        );
					
  $actions["statistic"]["company_statistic_export"] = array (
    'Name'     => $l_header_export,    
    'Url'      => "$path/statistic/statistic_index.php?action=company_statistic_export&amp;popup=1&amp;sel_list=".$statistic["list"]."",
    'Right'    => $cright_read,
    'Popup'    => 1,   
    'Target'   => $l_statistic,
    'Condition'=> array ('company_statistic') 
                                        );					
					
}
</script>
