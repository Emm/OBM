package org.obm.push.store.ehcache;

import java.util.Set;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.obm.configuration.store.StoreNotFoundException;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.ItemChange;

import bitronix.tm.TransactionManagerServices;

public class UnsynchronizedItemDaoEhcacheImplTest extends StoreManagerConfigurationTest  {

	private ObjectStoreManager objectStoreManager;
	private UnsynchronizedItemDaoEhcacheImpl unSynchronizedItemImpl;
	private Credentials credentials;
	private TransactionManager transactionManager;
	
	public UnsynchronizedItemDaoEhcacheImplTest() {
		super();
	}
	
	@Before
	public void init() throws StoreNotFoundException, NotSupportedException, SystemException {
		transactionManager = TransactionManagerServices.getTransactionManager();
		transactionManager.begin();
		this.objectStoreManager = new ObjectStoreManager( super.initConfigurationServiceMock() );
		this.unSynchronizedItemImpl = new UnsynchronizedItemDaoEhcacheImpl(objectStoreManager);
		this.credentials = new Credentials("login@domain", "password");
	}
	
	@After
	public void cleanup() throws IllegalStateException, SecurityException, SystemException {
		transactionManager.rollback();
	}
	
	@Test
	public void list() {
		Set<ItemChange> itemChanges = unSynchronizedItemImpl.listItemToAdd(credentials, getFakeDeviceId(), 1);
		Assert.assertNotNull(itemChanges);
	}
	
	@Test
	public void add() {
		unSynchronizedItemImpl.storeItemToAdd(credentials, getFakeDeviceId(), 1, buildItemChange("test 1"));
		Set<ItemChange> itemChanges = unSynchronizedItemImpl.listItemToAdd(credentials, getFakeDeviceId(), 1);
		Assert.assertNotNull(itemChanges);
		Assert.assertEquals(1, itemChanges.size());
		Assert.assertEquals("test 1", itemChanges.iterator().next().getDisplayName());
	}
	
	@Test
	public void addTwoItemsOnTheSameCollection() {
		ItemChange itemChange1 = buildItemChange("test 1");
		ItemChange itemChange2 = buildItemChange("test 2");
		ItemChange itemChange3 = buildItemChange("test 3");
		
		unSynchronizedItemImpl.storeItemToAdd(credentials, getFakeDeviceId(), 1, itemChange1);
		unSynchronizedItemImpl.storeItemToAdd(credentials, getFakeDeviceId(), 1, itemChange2);
		
		Set<ItemChange> itemChanges = unSynchronizedItemImpl.listItemToAdd(credentials, getFakeDeviceId(), 1);
		Assert.assertNotNull(itemChanges);
		Assert.assertEquals(2, itemChanges.size());
		
		Assert.assertTrue( contains(itemChanges, itemChange1) );
		Assert.assertTrue( contains(itemChanges, itemChange2) );
		Assert.assertFalse( contains(itemChanges, itemChange3) );
	}
	
	@Test
	public void addItemsOnTwoCollections() {
		ItemChange itemChange1 = buildItemChange("test 1.1");
		ItemChange itemChange2 = buildItemChange("test 1.2");
		ItemChange itemChange21 = buildItemChange("test 2.1");
		
		unSynchronizedItemImpl.storeItemToAdd(credentials, getFakeDeviceId(), 1, itemChange1);
		unSynchronizedItemImpl.storeItemToAdd(credentials, getFakeDeviceId(), 1, itemChange2);
		unSynchronizedItemImpl.storeItemToAdd(credentials, getFakeDeviceId(), 2, itemChange21);
		
		Set<ItemChange> itemChangesOneCollection = unSynchronizedItemImpl.listItemToAdd(credentials, getFakeDeviceId(), 1);
		Set<ItemChange> itemChangesTwoCollection = unSynchronizedItemImpl.listItemToAdd(credentials, getFakeDeviceId(), 2);
		
		Assert.assertNotNull(itemChangesOneCollection);
		Assert.assertEquals(2, itemChangesOneCollection.size());
		Assert.assertTrue( contains(itemChangesOneCollection, itemChange1) );
		Assert.assertTrue( contains(itemChangesOneCollection, itemChange2) );
		
		Assert.assertNotNull(itemChangesTwoCollection);
		Assert.assertEquals(1, itemChangesTwoCollection.size());
		Assert.assertTrue( contains(itemChangesTwoCollection, itemChange21) );	
	}
	
	@Test
	public void addTwoItemsDifferentTypeOnTheSameCollection() {
		ItemChange itemChange1 = buildItemChange("test 1");
		ItemChange itemChange2 = buildItemChange("test 2");
		ItemChange itemChange3 = buildItemChange("test 3");
		
		unSynchronizedItemImpl.storeItemToAdd(credentials, getFakeDeviceId(), 1, itemChange1);
		unSynchronizedItemImpl.storeItemToRemove(credentials, getFakeDeviceId(), 1, itemChange2);
		
		Set<ItemChange> itemChanges = unSynchronizedItemImpl.listItemToAdd(credentials, getFakeDeviceId(), 1);
		Assert.assertNotNull(itemChanges);
		Assert.assertEquals(1, itemChanges.size());
		
		Assert.assertTrue( contains(itemChanges, itemChange1) );
		Assert.assertFalse( contains(itemChanges, itemChange2) );
		Assert.assertFalse( contains(itemChanges, itemChange3) );
	}
	
	@Test
	public void clear() {
		unSynchronizedItemImpl.storeItemToAdd(credentials, getFakeDeviceId(), 1, buildItemChange("test 1"));
		unSynchronizedItemImpl.clearItemToAdd(credentials, getFakeDeviceId(), 1);		
		
		Set<ItemChange> itemChanges = unSynchronizedItemImpl.listItemToAdd(credentials, getFakeDeviceId(), 1);
		Assert.assertNotNull(itemChanges);
		Assert.assertEquals(0, itemChanges.size());
	}
	
	private ItemChange buildItemChange(String displayName) {
		ItemChange itemChange = new ItemChange();
		itemChange.setDisplayName(displayName);
		return itemChange;
	}
	
	private Device getFakeDeviceId(){
		return new Device(1, "DevType", "DevId", null);
	}
	
	private boolean contains(Set<ItemChange> expected, ItemChange actual) {
		for (ItemChange itemChange: expected) {
			if (itemChange.getDisplayName().equals(actual.getDisplayName())) {
				return true;
			}
		}
		return false;
	}
	
}
