<?php
///////////////////////////////////////////////////////////////////////////////
// OBM - File  : contact_js.inc                                              //
//     - Desc  : Contact javascript functions File                           //
// 2000-01-13 Pierre Baudracco                                               //
///////////////////////////////////////////////////////////////////////////////
// $Id$ //
///////////////////////////////////////////////////////////////////////////////


require("$obminclude/javascript/check_js.inc");

// to avoid a javascript error when ext_widget or ext_widget_label not defined
if (($params["ext_widget"] != "") || ($params["ext_widget_text"] != "")) {
  $extra_js .= "

function check_contact_get_id(valeur,text) {
  if ((valeur < 1) || (valeur == null)) {
    alert (\"$l_j_select_contact\");
    return false;
  } else {
    window.opener.document.$params[ext_widget].value=valeur;
    window.opener.document.$params[ext_widget_text].value=text;
    window.close();
    return true;
  }
}";
}

$extra_js .= "

function confirm_contact_del() {
  if (confirm (\"$l_delete_confirm\"))
    return true;
  else
    return false;
}

function check_contact(form) {
  // MANDATORY : Check that Contact name is not empty
  if (trim(form.tf_lname.value) == \"\") {
    alert (\"$l_fill_lastname\");
    return false;
  }

  return true;
}


///////////////////////////////////////////////////////////////////////////////
// Check if the kind check or deletion is possible                           //
///////////////////////////////////////////////////////////////////////////////
function check_contact_kind_checkdel(sel)
{
  if (sel.selectedIndex >= 0) {
    return true;
  } else {
    alert(\"$l_j_kind_delete\");
    return false;
  }
}

///////////////////////////////////////////////////////////////////////////////
// Check if the kind update is possible
///////////////////////////////////////////////////////////////////////////////
function check_contact_kind_upd(form, sel_del)
{
  kind_pos = sel_del.selectedIndex;
  if ((kind_pos >= 0) && (form.tf_label.value != \"\")) {
    form.sel_kind.value=sel_del.options[kind_pos].value;
    return true;
  } else {
    alert(\"$l_j_kind_update\");
    return false;
  }
}

///////////////////////////////////////////////////////////////////////////////
// Check if the kind creation is possible
///////////////////////////////////////////////////////////////////////////////
function check_contact_kind_new(form)
{
   if ( (trim(form.tf_kind_lang.value) == \"\")
        || (trim(form.tf_kind_label.value) == \"\")
        || (trim(form.tf_kind_header.value) == \"\") ) {
      alert(\"$l_j_kind_create\");
      return false;
   } else {
      return true;
   }
}

///////////////////////////////////////////////////////////////////////////////
// Fill the Kind's mini label select.
///////////////////////////////////////////////////////////////////////////////
var lang_array;

function fill_contact_select_label(source) 
{
  destination = source.form.sel_kind_label;
  kind = source.form.sel_kind;
  destIndex = source.options[source.selectedIndex].value;
  for (i = destination.options.length-1; i >= 0; i--) {
    destination.options[i] = null; 
  }  
  for (i = kind.options.length-1; i >= 0; i--) {
    kind.options[i] = null; 
  }
  kind.options[0]=new Option('$l_undef');
  destination.options[0]=new Option('$l_undef');
  i=1;
  for (var destValue in lang_array[destIndex]) {
    destination.options[i]= new Option(destValue,destValue);
    i++;
  }
  destination.selectedIndex=0;
  kind.selectedIndex=0;
}


///////////////////////////////////////////////////////////////////////////////
// Fill the Kind's header select.
///////////////////////////////////////////////////////////////////////////////
function fill_contact_select_header(source) 
{
  destination = source.form.sel_kind;
  lang = source.form.sel_kind_lang;
  destIndex = source.options[source.selectedIndex].value;
  for (i = destination.options.length-1; i >= 0; i--) {
    destination.options[i] = null; 
  }  
  lang.options[0]=new Option('$l_undef');
  destination.options[0]=new Option('$l_undef');
  i=1;
  for (var destValue in lang_array[lang.options[lang.selectedIndex].value][destIndex]) {
    destination.options[i]= new Option(destValue,lang_array[lang.options[lang.selectedIndex].value][destIndex][destValue]);
    i++;
  }
  destination.selectedIndex=0;
  
}
";

if (isset($params["ext_target"]) && $params["ext_target"]!="") {
  $extra_js .= "

  function fill_ext_form(dir_path) {
    ext_field = window.opener.document.$params[ext_target];
    ext_field.value = dir_path;
    window.close();
  } 
  
";
}

$extra_js .= "
function contact_copy_clip(text)
{

  if (window.clipboardData) 
  {

    window.clipboardData.setData(\"Text\", text);
  }
  else if (window.netscape) 
  { 
    try {
      netscape.security.PrivilegeManager.enablePrivilege('UniversalXPConnect');
    } catch (err) {   
      alert('$l_j_copy_privilege_alert');
      return false;
    }
    
    var clip = Components.classes['@mozilla.org/widget/clipboard;1'].createInstance(Components.interfaces.nsIClipboard);
    if (!clip) return;

    var trans = Components.classes['@mozilla.org/widget/transferable;1'].createInstance(Components.interfaces.nsITransferable);
    if (!trans) return;

    trans.addDataFlavor('text/unicode');

    var str = new Object();
    var len = new Object();

    var str = Components.classes[\"@mozilla.org/supports-string;1\"].createInstance(Components.interfaces.nsISupportsString);

    var copytext=text;

    str.data=copytext;

    trans.setTransferData(\"text/unicode\",str,copytext.length*2);

    var clipid=Components.interfaces.nsIClipboard;

    if (!clip) return false;

    clip.setData(trans,null,clipid.kGlobalClipboard);

  }
  return false;
}
";

// Some messages and images we got from inside js code
$l_js_sync = phpStringToJsString($l_sync);
$l_js_desync = phpStringToJsString($l_desync);

  $extra_js .= "
obm.vars.labels.sync   = '$l_js_sync';
obm.vars.labels.desync = '$l_js_desync';
obm.vars.images.sync   = '$ico_sync';
obm.vars.images.desync = '$ico_desync';
";
?>
