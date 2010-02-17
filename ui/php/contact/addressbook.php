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
<?php
require_once 'obminclude/of/of_search.php';
require_once 'obminclude/of/of_contact.php';

class OBM_AddressBook implements OBM_ISearchable {
  private $id;
  private $name;
  private $displayname;
  private $owner;
  private $isDefault;
  private $access;
  private $read;
  private $write;
  private $admin;
  private $syncable;
  private $synced;

  public function __construct($id, $name, $is_default, $owner, $syncable, $synced, $access, $read, $write, $admin) {
    $this->id = $id;
    $this->name = $name;
    $this->displayname = $name;
    $this->access = $access;
    $this->read = $read;
    $this->write = $write;
    if($GLOBALS['obm'])
    if((Perm::get_module_rights($entityType) & $GLOBALS['cright_write_admin']) == $GLOBALS['cright_write_admin']) $this->admin = 1;
    else $this->admin = $admin;
    $this->isDefault = $is_default;
    $this->owner = $owner;
    $this->syncable = $syncable;
    $this->synced = $synced;
    $this->db = new DB_OBM;
  }

  public function __set($property, $value) {
    if ($property != 'id') {
      $this->$property = $value;
    }
  }

  public function __get($property) {
    if (property_exists($this, $property)) {
      if ($property == "displayname") {
        if ($this->isDefault) {
          if ($this->owner == $GLOBALS['obm']['uid'] || $this->name == 'public_contacts') {
            return $GLOBALS["l_{$this->name}"];
          } else {
            $owner = get_entity_info($this->owner, 'user');
            return $GLOBALS["l_{$this->name}"]." ($owner[label])";
          }
        } else {
          return $this->name;
        }
      } else {
        return $this->$property;
      }
    }
    return;
  }

  public static function fieldsMap() {
    $fields['*'] = array('AddressBook.name' => 'text');
    $fields['name'] = array('AddressBook.name' => 'text');
    $fields['id'] = array('AddressBook.id' => 'integer');
    $fields['owner'] = array('AddressBook.owner' => 'integer');
    $fields['default'] = array('AddressBook.is_default' => 'boolean');

    return $fields;
  }

  public function getContacts($pattern='', $offset=0, $limit=100) {
    $pattern .= ' addressbookId:'.$this->id;
    return OBM_Contact::search($pattern, $offset, $limit);
  }

  public function addContact($fields) {
    return OBM_Contact::create($fields, $this);
  }

  public static function get($pattern) {
    return self::search($pattern)->current();
  }

  public static function search($searchPattern=null) {
    if($searchPattern !== null) {
      $query = " AND AddressBook.id=$searchPattern";
    }
    $db = new DB_OBM;
    $addressBooks = array();
    $db->xquery('
      SELECT 
        AddressBook.id,
        AddressBook.owner,
        AddressBook.name,
        AddressBook.is_default,
        AddressBook.syncable,
        1 as entityright_access,
        1 as entityright_read,
        1 as entityright_write,
        1 as entityright_admin,
        SyncedAddressbook.user_id as synced
      FROM AddressBook 
      LEFT JOIN SyncedAddressbook ON SyncedAddressbook.addressbook_id = AddressBook.id AND SyncedAddressbook.user_id = '.$GLOBALS['obm']['uid'].'
      WHERE AddressBook.owner = '.$GLOBALS['obm']['uid'].' '.$query.' ORDER BY AddressBook.is_default DESC, AddressBook.name'); 
    while($db->next_record()) {
      $addressBooks[$db->f('id')] = new OBM_AddressBook($db->f('id'), $db->f('name'), $db->f('is_default'), $db->f('owner'), $db->f('syncable'), $db->f('synced'), $db->f('entityright_access'),
                                                        $db->f('entityright_read'), $db->f('entityright_write'),$db->f('entityright_admin'));
    }    
    $columns = array('addressbookentity_addressbook_id', 'entityright_access', 'entityright_read', 'entityright_write', 'entityright_admin');
    $db->xquery('
      SELECT 
        AddressBook.id,
        AddressBook.owner,
        AddressBook.name,
        AddressBook.is_default,
        AddressBook.syncable,
        Rights.entityright_access,
        Rights.entityright_read,
        Rights.entityright_write,
        Rights.entityright_admin,
        SyncedAddressbook.user_id as synced
      FROM AddressBook 
      INNER JOIN ('.OBM_Acl::getAclSubselect($columns, 'addressbook', null, $GLOBALS['obm']['uid']).') AS Rights ON AddressBook.id = Rights.addressbookentity_addressbook_id
      LEFT JOIN SyncedAddressbook ON SyncedAddressbook.addressbook_id = AddressBook.id AND SyncedAddressbook.user_id = '.$GLOBALS['obm']['uid'].'
      WHERE 1=1 '.$query.' AND AddressBook.domain_id = '.$GLOBALS['obm']['domain_id'].' ORDER BY AddressBook.name');
    while($db->next_record()) {
      if($addressBooks[$db->f('id')]) {
        if($db->f('entityright_access') == 1) $addressBooks[$db->f('id')]->access = 1;
        if($db->f('entityright_read') == 1) $addressBooks[$db->f('id')]->read = 1;
        if($db->f('entityright_write') == 1) $addressBooks[$db->f('id')]->write = 1;
        if($db->f('entityright_admin') == 1) $addressBooks[$db->f('id')]->admin = 1;
      } else {
        $addressBooks[$db->f('id')] = new OBM_AddressBook($db->f('id'), $db->f('name'), $db->f('is_default'), $db->f('owner'), $db->f('syncable'), $db->f('synced'), $db->f('entityright_access'),
                                                          $db->f('entityright_read'), $db->f('entityright_write'),$db->f('entityright_admin'));
      }
    }  

    return new OBM_AddressBookArray($addressBooks);
  }

  public static function create($addressbook) {

    $domain_id = $GLOBALS['obm']['domain_id'];
    $uid = $GLOBALS['obm']['uid'];
    $ad_name = $addressbook['name'];

    $query = "INSERT INTO AddressBook (
      domain_id,
      timeupdate,
      timecreate,
      userupdate,
      usercreate,
      origin,
      owner,
      name,
      is_default,
      syncable) VALUES (
      $domain_id,
      NOW(),
      NOW(),
      $uid,
      $uid,
      '$GLOBALS[c_origin_web]',
      $uid,
      '$ad_name',
      false,
      true)";
    $db = new DB_OBM;
    $db->query($query);
    $id = $db->lastid();
    $entity_id = of_entity_insert('addressbook', $id);    
    return self::get($id);
  }

  public static function delete($addressbook) {
    $id = $addressbook['addressbook_id'];
    $uid = $GLOBALS['obm']['uid'];
    $ad = self::get($id);
    if (!$ad->isDefault && $ad->admin) {
      $db = new DB_OBM;
      // Delete contacts
      $query = "DELETE FROM Contact WHERE contact_addressbook_id='$id'";
      $db->query($query);
      // Delete addressbook
      $query = "DELETE FROM AddressBook WHERE id='$id'";
      $db->query($query);
      // Delete solr
      OBM_IndexingService::deleteByQuery('contact', "addressbookId:$id");
    }
  }

  public static function timestamp($id) {
    $query = "UPDATE AddressBook SET timeupdate=NOW() WHERE id='$id'";
    $db = new DB_OBM;
    $db->query($query);
  }


  public static function store($addressbook) {
    $id = $addressbook['addressbook_id'];
    $syncable = $addressbook['sync'];
    $name = $addressbook['name'];
    $ad = self::get($id);
    $syncable = $ad->syncable;
    $name = $ad->name;
    if (isset($addressbook['name'])) $name = $addressbook['name'];
    if ($addressbook['action'] == 'setSyncable') {
      $syncable = !$syncable;
    }
    $syncable = $syncable ? 'true':'false';

    if ($ad->write) {
      if (!$ad->isDefault) $name_q =  "name='$name',";
      $query = "UPDATE AddressBook SET $name_q syncable=$syncable WHERE id='$id'";
      $db = new DB_OBM;
      $db->query($query);
    }
  }

  public static function setSynced($addressbook) {
    $id = $addressbook['addressbook_id'];
    $ad = self::get($id);
    $uid = $GLOBALS['obm']['uid'];
    $db = new DB_OBM;
    if(!$ad->syncable) return true;
    if ($ad->synced) {
      // Remove synchronized addressbook
      $query = "DELETE FROM SyncedAddressbook WHERE user_id='$uid' AND addressbook_id='$id'";
    } else {
      // Add synchronized addressbook
      $query = "INSERT INTO SyncedAddressbook VALUES ($uid, $id, NOW())"; 
    }
    $db->query($query);
  }

  public function __toString() {
    return $this->__get('displayname');
  }
}


class OBM_AddressBookArray implements ArrayAccess, Iterator {

  private $addressbooks = array();

  private $offset = 0;

  public function __construct($addressbooks) {
    $this->addressbooks = $addressbooks;
  }

  public function searchContacts($pattern, $offset=0) {
    if(!empty($this->addressbooks)) {
      $pattern .= ' addressbookId:('.implode(' OR ',array_keys($this->addressbooks)).')';
      return OBM_Contact::search($pattern, $offset);
    }
  }

  public function getMyContacts() {
    foreach($this->addressbooks as $addressbook) {
      if($addressbook->isDefault && $addressbook->name == 'contacts' && $addressbook->owner == $GLOBALS['obm']['uid'])
        return $addressbook;
    }
  }

  public function getCollectedAddressbook() {
    foreach($this->addressbooks as $addressbook) {
      if($addressbook->isDefault && $addressbook->name == 'collected_contacts' && $addressbook->owner == $GLOBALS['obm']['uid'])
        return $addressbook;
    }
  }

  public function getPublicAddressbook() {
    foreach($this->addressbooks as $addressbook) {
      if($addressbook->isDefault && $addressbook->name == 'public_contacts')
        return $addressbook;
    }
  }

  public function offsetSet($offset, $value) {
    $this->$addressbooks[$offset] = $value;
  }

  public function offsetExists($offset) {
    return isset($this->addressbooks[$offset]);
  }

  public function offsetUnset($offset) {
    unset($this->addressbooks[$offset]);
  }

  public function offsetGet($offset) {
    return isset($this->addressbooks[$offset]) ? $this->addressbooks[$offset] : null;
  }

  public function rewind() {
    reset($this->addressbooks);
  }

  public function current() {
    return current($this->addressbooks);
  }

  public function key() {
    return key($this->addressbooks);
  }

  public function next() {
    next($this->addressbooks);
  }

  public function valid() {
   return current($this->addressbooks); 
  }
}
