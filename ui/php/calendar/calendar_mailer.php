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

require("$obminclude/of/of_mailer.php");

class CalendarMailer extends OBM_Mailer {
  protected $module = 'calendar';
  
  protected $attachIcs = true;

  protected $return_path;
  
  public function __construct() {
    parent::__construct();
    $this->attachIcs = $GLOBALS['ccalendar_send_ics'];
  }
  
  protected function eventInvitation($event, $attendees) {
    $this->from = $this->getSender();
    $this->recipients = $this->getRecipients($attendees);
    $this->return_path = $this->getOwner($event);
    $this->subject = __('New event created by %sender% on OBM: %title%', array('%sender%'=>$this->from[1], '%title%' => $event->title));
    $this->body = $this->extractEventDetails($event, $this->from);
    if ($this->attachIcs) {
      $this->parts[] = array(
        'content' => fopen($this->generateIcs($event, $attendees, "request"), 'r'), 
        'content_type' => 'text/calendar; charset=UTF-8; method=REQUEST'
      );
      $this->attachments[] = array(
        'content' => fopen($this->generateIcs($event, $attendees, "request"), 'r'), 
        'filename' => 'meeting.ics', 'content_type' => 'application/ics'
      );
    }
  }

  protected function eventCancel($event, $attendees) {
    $this->from = $this->getSender();
    $this->recipients = $this->getRecipients($attendees);
    $this->return_path = $this->getOwner($event);
    $this->subject = __('Event cancelled by %sender% on OBM: %title%', array('%sender%'=>$this->from[1], '%title%' => $event->title));
    $this->body = $this->extractEventDetails($event, $this->from);
    if ($this->attachIcs) {
      $this->parts[] = array(
        'content' => fopen($this->generateIcs($event, $attendees, "cancel"), 'r'), 
        'content_type' => 'text/calendar; charset=UTF-8; method=CANCEL'
      );
      $this->attachments[] = array(
        'content' => fopen($this->generateIcs($event, $attendees, "cancel"), 'r'), 
        'filename' => 'meeting.ics', 'content_type' => 'application/ics'
      );
    }
  }
  
  protected function eventUpdate($event, $oldEvent, $attendees) {
    $this->from = $this->getSender();
    $this->recipients = $this->getRecipients($attendees);
    $this->return_path = $this->getOwner($event);
    $this->subject = __('Event updated by %sender% on OBM: %title%', array('%sender%'=>$this->from[1], '%title%' => $event->title));
    $this->body = array_merge($this->extractEventDetails($event, $this->from),
                              $this->extractEventDetails($oldEvent, $this->from, 'old_'));
    if ($this->attachIcs) {
      $this->parts[] = array(
        'content' => fopen($this->generateIcs($event, $attendees, "request"), 'r'), 
        'content_type' => 'text/calendar; charset=UTF-8; method=REQUEST'
      );
      $this->attachments[] = array(
        'content' => fopen($this->generateIcs($event, $attendees, "request"), 'r'), 
        'filename' => 'meeting.ics', 'content_type' => 'application/ics'
      );      
    }
  }

  protected function eventStateUpdate($event, $user) {
    $this->from = $this->getSender();
    $this->recipients = $this->getRecipients(array($event->owner), 'set_mail_participation');
    $this->subject = __('Participation updated on OBM: %title%', array('%title%' => $event->title));
    $this->body = $this->extractEventDetails($event, $this->from, '', $user);
  }


  /////////////////////////////////////////////////////////////////////////////
  // RESOURCE
  /////////////////////////////////////////////////////////////////////////////
  protected function resourceReservation($event, $resourceOwners, $resource) {
    $this->from = $this->getSender();
    $this->recipients = $this->getRecipients($resourceOwners);
    $this->subject = __('Resource %resource% reservation on OBM: %title%', array('%resource%' => $resource->label, '%title%' => $event->title));
    $this->body = $this->extractEventDetails($event, $this->from);
  }

  protected function resourceCancel($event, $resourceOwners) {
    $this->from = $this->getSender();
    $this->recipients = $this->getRecipients($resourceOwners);
    $this->subject = __('Resource %resource% reservation cancelled on OBM: %title%', array('%title%' => $event->title));
    $this->body = $this->extractEventDetails($event, $this->from);
  }
  
  protected function resourceUpdate($event, $oldEvent, $resourceOwners, $resource) {
    $this->from = $this->getSender();
    $this->recipients = $this->getRecipients($resourceOwners);
    $this->subject = __('Resource %resource% reservation updated on OBM: %title%', array('%resource%' => $resource->label, '%title%' => $event->title));
    $this->body = array_merge($this->extractEventDetails($event, $this->from),
                              $this->extractEventDetails($oldEvent, $this->from, 'old_'));
  }

  protected function resourceStateUpdate($event, $res) {
    $this->from = $this->getSender();
    $this->recipients = $this->getRecipients(array($event->owner), 'set_mail_participation');
    $this->subject = __('Resource participation updated on OBM: %title%', array('%title%' => $event->title));
    $this->body = $this->extractEventDetails($event, $this->from, '', $res);
  }


  /////////////////////////////////////////////////////////////////////////////
  // CONTACT
  /////////////////////////////////////////////////////////////////////////////
  protected function contactInvitation($event, $contacts) {
    $this->from = $this->getSender();
    $recips = array();
    foreach($contacts as $contact) {
      $contact_info = get_entity_info($contact->id, 'contact');
      $label = $contact_info['label']; 
      $email = $contact_info['email'];
      if (trim($email) != "") {
        array_push($recips, array($email, $label));
      }
    }
    $this->recipients = $recips;
    $this->subject = __('New event created by %sender% on OBM: %title%', array('%sender%'=>$this->from[1], '%title%' => $event->title));
    $this->body = $this->extractEventDetails($event, $this->from);
    if ($this->attachIcs) {
      $this->parts[] = array(
        'content' => fopen($this->generateIcs($event, $contacts, "request"), 'r'), 
        'content_type' => 'text/calendar; charset=UTF-8; method=REQUEST'
      );
      $this->attachments[] = array(
        'content' => fopen($this->generateIcs($event, $contacts, "request"), 'r'), 
        'filename' => 'meeting.ics', 'content_type' => 'application/ics'
      );
    }
  }


  protected function contactCancel($event, $contacts) {
    $this->from = $this->getSender();
    $recips = array();
    foreach($contacts as $contact) {
      $contact_info = get_entity_info($contact->id, 'contact');
      $label = $contact_info['label']; 
      $email = $contact_info['email'];
      if (trim($email) != "") {
        array_push($recips, array($email, $label));
      }
    }
    $this->recipients = $recips;
    $this->subject = __('Event cancelled by %sender% on OBM: %title%', array('%sender%'=>$this->from[1], '%title%' => $event->title));
    $this->body = $this->extractEventDetails($event, $this->from);
    if ($this->attachIcs) {
      $this->parts[] = array(
        'content' => fopen($this->generateIcs($event, $contacts, "cancel"), 'r'), 
        'content_type' => 'text/calendar; charset=UTF-8; method=CANCEL'
      );
      $this->attachments[] = array(
        'content' => fopen($this->generateIcs($event, $contacts, "cancel"), 'r'), 
        'filename' => 'meeting.ics', 'content_type' => 'application/ics'
      );
    }
  }


  protected function contactUpdate($event, $oldEvent, $contacts) {
    $this->from = $this->getSender();
    $recips = array();
    foreach($contacts as $contact) {
      $contact_info = get_entity_info($contact->id, 'contact');
      $label = $contact_info['label']; 
      $email = $contact_info['email'];
      if (trim($email) != "") {
        array_push($recips, array($email, $label));
      }
    }
    $this->recipients = $recips;
    $this->subject = __('Event updated by %sender% on OBM: %title%', array('%sender%'=>$this->from[1], '%title%' => $event->title));
    $this->body = array_merge($this->extractEventDetails($event, $this->from),
                              $this->extractEventDetails($oldEvent, $this->from, 'old_'));
    if ($this->attachIcs) {
      $this->parts[] = array(
        'content' => fopen($this->generateIcs($event, $contacts, "request"), 'r'), 
        'content_type' => 'text/calendar; charset=UTF-8; method=REQUEST'
      );
      $this->attachments[] = array(
        'content' => fopen($this->generateIcs($event, $contacts, "request"), 'r'), 
        'filename' => 'meeting.ics', 'content_type' => 'application/ics'
      );      
    }
  }


  /**
   * Perform the export meeting to the vCalendar format
   */
  private function generateIcs($event, $attendees, $method) {
    include_once('obminclude/of/vcalendar/writer/ICS.php');
    include_once('obminclude/of/vcalendar/reader/OBM.php');
    
    $reader = new Vcalendar_Reader_OBM(array('user' => array($this->userId => 'dummy')), array($event->id));
    $document = $reader->getDocument($method);
    $writer = new Vcalendar_Writer_ICS();  
    $writer->writeDocument($document);

    $tmpFilename = secure_tmpname('.ics','ics_');
    $res = fopen($tmpFilename, 'w');

    if (!$res) {
      throw new Exception('Unable to open file');
    }
    fputs($res, $writer->buffer);
    fclose($res);
    return $tmpFilename;
  }
  
  private function extractEventDetails($event, $sender, $prefix = '', $target = null) {
	$contacts = $event->contact;
	foreach ($contacts as $contact) {
		$attendees[] = $contact->label;
	}
	$users = $event->user;
	foreach ($users as $user) {
		$attendees[] = $user->label;
	}
	$i = 0;
	while ($i<10 && $attendees[$i]) {
		$list_attendees .= $attendees[$i].', ';
		$i++;
	} 
	$list_attendees[strlen($list_attendees)-2] = '';
	if ($i == 10) $list_attendees .= '...'; 
    return array(
      'host'             => $GLOBALS['cgp_host'],
      $prefix.'id'       => $event->id,
      $prefix.'start'    => $event->date_begin->getOutputDateTime(),
      $prefix.'end'      => $event->date_end->getOutputDateTime(),
      $prefix.'title'    => $event->title,
      $prefix.'location' => $event->location,
      $prefix.'auteur'   => $event->owner->label,
      $prefix.'target'   => $target->label,
      $prefix.'targetState' => __($target->state),
      $prefix.'attendees' => $list_attendees
    );
  }

}

class shareCalendarMailer extends OBM_Mailer {
  protected $module = 'calendar';

  public function addRecipient($mail) {
    if($mail != 'all')
      $this->recipients[] = $mail;
  }

  private function extractEntityDetails($entity, $sender, $prefix = '', $target = null) {
    return array(
      'host'             => $GLOBALS['cgp_host'],
      $prefix.'name'     => $entity['lastname'],
      $prefix.'firstname'=> $entity['firstname'],
      $prefix.'token'    => $entity['token']
    );
  }

  public function userShareHtml($entity) {
    $this->from = $this->getSender();
    $this->subject = __('Partage d\'agenda : %firstname% %name%', array( '%name%' => $entity['lastname'],'%firstname%' => $entity['firstname']));
    $this->body = $this->extractEntityDetails($entity, $this->from); 
  }

  public function userShareIcs($entity) {
    $this->from = $this->getSender();
    $this->subject = __('Partage d\'agenda : %firstname% %name%', array( '%name%' => $entity['lastname'],'%firstname%' => $entity['firstname']));
    $this->body = $this->extractEntityDetails($entity, $this->from); 
  }
}
