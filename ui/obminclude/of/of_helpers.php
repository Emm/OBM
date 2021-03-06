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

/**
 * Generates a form containing a single button that submits to the $url...
 * 
 * ... because THOU SHALT NOT USE LINKS FOR ACTIONS THAT TRIGGER DATA CHANGES !!!
 * 
 * The generated form element has a class name of buttonTo to allow 
 * styling of the form itself and its children.
 * 
 * @param $label button label
 * @param $url form action url
 * @param $btn_class CSS class given to the button itself
 * @param $confirm_msg if set to a string, a JS confirm popup will be displayed
 * @access public
 * @return string
 */
function button_to($label, $url, $btn_class = false, $confirm_msg = false)
{
    if (is_string($confirm_msg)) {
      $confirm_msg = " onclick=\"return confirm('".phpStringToJsString($confirm_msg)."');\"";
    }
    if (is_string($btn_class)) {
      $btn_class = " class=\"$btn_class\"";
    }
    return "<form method=\"post\" action=\"$url\" class=\"buttonTo\">
        <div>
          <input type=\"submit\" value=\"$label\"{$btn_class}{$confirm_msg} />
        </div>
      </form>";
}

function wd_remove_accents($str, $charset='utf-8')
{
    $str = htmlentities($str, ENT_NOQUOTES, $charset);

    $str = preg_replace('#\&([A-za-z])(?:acute|cedil|circ|grave|ring|tilde|uml)\;#', '\1', $str);
    $str = preg_replace('#\&([A-za-z]{2})(?:lig)\;#', '\1', $str); // pour les ligatures e.g. '&oelig;'
    $str = preg_replace('#\&[^;]+\;#', '', $str); // supprime les autres caractères

    return $str;
}

// Case insensitive sort, correct acute letters sort
function wd_unaccent_compare_ci($a, $b)
{
    return strcmp(strtolower(wd_remove_accents($a)), strtolower(wd_remove_accents($b)));
}

function get_localized_countries_array() {
  $countries = include(dirname(__FILE__)."/../lib/Stato/i18n/data/countries/".SI18n::get_locale().".php");
  if (is_array($countries)) usort($countries, 'wd_unaccent_compare_ci');
  return $countries;
}

function get_localized_country($code) {
  $countries = include(dirname(__FILE__)."/../lib/Stato/i18n/data/countries/".SI18n::get_locale().".php");
  return $countries[$code];
}
