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

include_once('CronJob.class.php');

global $obminclude; 

class DelegationGroups extends CronJob {
  /**
   * @var Logger
   */
  var $logger;

  function mustExecute($date) {
    $min = date('i',$date);
    return ($min%10 === 0);
  }

  function execute($date) {
    $this->logger->debug('Updating groups built from delegations');
    $domains = $this->getDomains();
    foreach ($domains as $domain_id) {
      $this->processDomain($domain_id);
    }
  }

  protected function processDomain($domain_id) {
    $delegations = $this->buildDelegationsList($domain_id);

    foreach ($delegations as $delegation => $parent) {
      $group_id = $this->getDelegationGroup($domain_id, $delegation);
      $this->purgeGroupMembers($group_id);
      $this->populateGroup($domain_id, $group_id, $delegation);
      if ($parent) {
        $parent_id = $this->getDelegationGroup($domain_id, $parent);
        $this->associatesDelegationGroups($parent_id, $group_id);
      }
      of_usergroup_update_group_node($group_id);
      //FIXME: appeller l'automate sur ce groupe ??
    }
  }

  protected function delegationGroupName($delegation) {
    include("obminclude/lang/".$GLOBALS[ini_array][lang]."/global.inc");
    if ($delegation == "/") {
      $name = $l_all;
    } else {
      $name = "$l_all.$delegation";
    }
    return $name;
  }

  protected function associatesDelegationGroups($parent_id, $child_id) {
    $obm_q = new DB_OBM;
    $query = "INSERT INTO GroupGroup (groupgroup_parent_id, groupgroup_child_id)
      VALUES ($parent_id, $child_id)";
    $this->logger->core($query);
    $obm_q->query($query);
  }

  protected function populateGroup($domain_id, $group_id, $delegation) {
    $obm_q = new DB_OBM;
    $query = "INSERT INTO UserObmGroup (userobmgroup_group_id, userobmgroup_userobm_id)
      SELECT $group_id, userobm_id
      FROM UserObm
      WHERE userobm_domain_id=$domain_id AND (userobm_delegation='$delegation' OR userobm_delegation='$delegation/')";
    $this->logger->core($query);
    $obm_q->query($query);
  }

  protected function purgeGroupMembers($group_id) {
    $obm_q = new DB_OBM;
    $query = "DELETE FROM UserObmGroup WHERE userobmgroup_group_id=$group_id";
    $this->logger->core($query);
    $obm_q->query($query);
    $query = "DELETE FROM GroupGroup WHERE groupgroup_parent_id=$group_id OR groupgroup_child_id=$group_id";
    $this->logger->core($query);
    $obm_q->query($query);
  }

  protected function getDelegationGroup($domain_id, $delegation) {
    $groupName = $this->delegationGroupName($delegation);
    $obm_q = new DB_OBM;
    $query = "SELECT group_id FROM UGroup WHERE group_domain_id=$domain_id AND group_name='$groupName'";
    $this->logger->core($query);
    $obm_q->query($query);
    if ($obm_q->next_record()) {
      return $obm_q->f('group_id');
    }

    // The group does not exists, we create !
    return $this->createGroup($domain_id, $delegation);
  }

  protected function createGroup($domain_id, $delegation) {
    $groupName = $this->delegationGroupName($delegation);
    $gid = sql_parse_int(get_first_group_free_gid());
    $q_delegation = of_delegation_query_insert_clauses('group_delegation', $delegation);

    $query = "INSERT INTO UGroup (
    group_timeupdate,
    group_timecreate,
    group_userupdate,
    group_usercreate,
    group_domain_id,
    group_privacy,
    group_gid,
    group_name
    $q_delegation[field],
    group_desc
    ) VALUES (
    null,
    NOW(),
    null,
    1,
    $domain_id,
    0,
    $gid,
    '$groupName'
    $q_delegation[value],
    ''
    )";
    $this->logger->core($query);
    $obm_q = new DB_OBM;
    $retour = $obm_q->query($query);
    $id = $obm_q->lastid();
    if ($id > 0) {
      $entity_id = of_entity_insert('group', $id);  
    }
    return $id;    
  }

  protected function buildDelegationsList($domain_id) {
    $separator = '/';
    $delegations = array();
    $obm_q = new DB_OBM;
    $query = "SELECT DISTINCT userobm_delegation FROM UserObm WHERE userobm_domain_id=$domain_id";
    $this->logger->core($query);
    $obm_q->query($query);

    while ($obm_q->next_record()) {
      $data = explode($separator,$obm_q->f('userobm_delegation'));
      $delegation = "$separator";
      $parent = false;
      foreach ($data as $current) {
        $delegation.= $separator.$current;
        if ($delegation[strlen($delegation)-1]==$separator) {
          $delegation = substr($delegation, 0, -1);
        }
        $delegation = str_replace($separator.$separator,$separator,$delegation);
        if (!isset($delegations[$delegation]))
          $delegations[$delegation] = $parent;
        $parent = $delegation;
      }
    }
    return $delegations;
  }

  protected function getDomains() {
    $domains = array();
    $obm_q = new DB_OBM;
    $query = "SELECT domain_id FROM Domain WHERE domain_global=0";
    $this->logger->core($query);
    $obm_q->query($query);

    while ($obm_q->next_record()) {
      $domains[] = $obm_q->f('domain_id');
    }
    return $domains;
  }
}
?>
