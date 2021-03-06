/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (c) 1997-2008 Aliasource - Groupe LINAGORA
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 2 of the
 *  License, (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 * 
 *  http://www.obm.org/                                              
 * 
 * ***** END LICENSE BLOCK ***** */
package fr.aliacom.obm.common.contact;

import java.util.List;

import org.obm.sync.book.Folder;

import com.google.common.collect.ImmutableList;

public class FolderUpdates {

	private List<Folder> folders;
	
	public FolderUpdates() {
		folders = ImmutableList.of();
	}
	
	public List<Folder> getFolders() {
		return folders;
	}
	
	public void setFolders(List<Folder> folders) {
		this.folders = folders;
	}
	
}
