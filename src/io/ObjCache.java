package io;

import java.util.HashMap;

import io.FileBlockPointer;

public class ObjCache 
{
	/**
	 * list of zfs objects
	 */
	public static class ObjectList
	{
		// contained objects
		public HashMap<Long,FileBlockPointer> objects = new HashMap<>();
		
		// pointer to the objectlist itself
		public FileBlockPointer listBlock;
		
		// id list (internal for cache, no zfs id!)
		public int listId;
	}
	
	/**
	 * object list cache
	 */
	private static HashMap<Integer,ObjectList> objectcache = new HashMap<>(); 
	
	/**
	 * add zfs object to cached objectlist
	 */
	public static void addObject(int listId, long objectId, FileBlockPointer pointer)
	{
		ObjectList objlist = objectcache.get(Integer.valueOf(listId));
		objlist.objects.put(Long.valueOf(objectId), pointer);
	}
	
	/**
	 * get zfs object from cache
	 */
	public static FileBlockPointer getObject(int listId, long objectId)
	{
		ObjectList objlist = objectcache.get(Integer.valueOf(listId));
		if (objlist == null)
		{
			return null;
		}
		return objlist.objects.get(Long.valueOf(objectId));
	}
	
	/**
	 * get the pointer to objectlist
	 */
	public static FileBlockPointer getListObject(int listId)
	{
		ObjectList objlist = objectcache.get(Integer.valueOf(listId));
		if (objlist == null)
		{
			return null;
		}
		return objlist.listBlock;
	}
	
	/**
	 * sets the the pointer to objectlist
	 */
	public static void setListObject(int listId, FileBlockPointer pointer)
	{
		ObjectList objlist = objectcache.get(Integer.valueOf(listId));
		if (objlist == null)
		{
			objlist = new ObjectList();
			objlist.listId = listId;
			objectcache.put(Integer.valueOf(listId), objlist);
		}
		objlist.listBlock = pointer;
	}

	private static int nextListId = 0;

	/**
	 * get the listid from an objectlist pointer, 
	 * if not found get new listid, and put the pointer in cache
	 */
	public static int getListId(FileBlockPointer pointer)
	{
		for (ObjectList obj : objectcache.values())
		{
			if (obj.listBlock.equals(pointer))
			{
				return obj.listId;
			}
		}
		ObjectList newol = new ObjectList();
		newol.listId = nextListId;
		nextListId++;
		newol.listBlock = pointer;
		objectcache.put(Integer.valueOf(newol.listId), newol);
		return newol.listId;
	}

	/**
	 * get the next listid
	 */
	public static int getNewListId()
	{
		return nextListId++;
	}
	
}
