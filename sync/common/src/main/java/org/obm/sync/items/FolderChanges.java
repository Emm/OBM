package org.obm.sync.items;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.obm.sync.book.Folder;

public class FolderChanges {

	private List<Folder> updated;
	private Set<Integer> removed;
	private Date lastSync;

	public Date getLastSync() {
		return lastSync;
	}

	public void setLastSync(Date lastSync) {
		this.lastSync = lastSync;
	}

	public List<Folder> getUpdated() {
		return updated;
	}

	public void setUpdated(List<Folder> updated) {
		this.updated = updated;
	}

	public Set<Integer> getRemoved() {
		return removed;
	}

	public void setRemoved(Set<Integer> removed) {
		this.removed = removed;
	}

}
