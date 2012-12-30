/*******************************************************************************
* Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*******************************************************************************/
package nl.cwi.sen1.AmbiDexter.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * This map implementation is shareable and can be easily cloned
 * (simple arraycopy of the entries array).
 * 
 * @author Arnold Lankamp
 *
 * @param <K>
 *          The key type
 * @param <V>
 *          The value type
 */
public final class ShareableHashMap<K, V> implements Map<K, V>, Iterable<Map.Entry<K,V>> {
	private final static int INITIAL_LOG_SIZE = 4;

	private int modSize;
	private int hashMask;
	
	private Entry<K, V>[] data;
	
	private int threshold;
	
	private int load;
	
	private int currentHashCode;
	
	/**
	 * Constructor.
	 */
	@SuppressWarnings("unchecked")
	public ShareableHashMap(){
		super();
		
		modSize = INITIAL_LOG_SIZE;
		int tableSize = 1 << modSize;
		hashMask = tableSize - 1;
		data = new Entry[tableSize];
		
		threshold = tableSize;
		
		load = 0;
		
		currentHashCode = 0;
	}
	
	/**
	 * Copy constructor
	 * 
	 * @param sharedHashMap
	 *            The map to copy.
	 */
	public ShareableHashMap(ShareableHashMap<K, V> sharedHashMap){
		super();
		
		throw new UnsupportedOperationException("Not possible anymore due to new rehash().");
		
		/*modSize = sharedHashMap.modSize;
		int tableSize = 1 << modSize;
		hashMask = tableSize - 1;
		data = sharedHashMap.data.clone();
		
		threshold = tableSize;
		
		load = sharedHashMap.load;
		
		currentHashCode = sharedHashMap.currentHashCode;*/
	}
	
	/**
	 * Removes all the entries from this map.
	 */
	@SuppressWarnings("unchecked")
	public void clear(){
		modSize = INITIAL_LOG_SIZE;
		int tableSize = 1 << modSize;
		hashMask = tableSize - 1;
		data = new Entry[tableSize];
		
		threshold = tableSize;
		
		load = 0;
		
		currentHashCode = 0;
	}
	
	/**
	 * Rehashes this map.
	 */
	@SuppressWarnings("unchecked")
	private void rehash(){
		int nrOfEntries = 1 << (++modSize);
		int newHashMask = nrOfEntries - 1;
		
		Entry<K, V>[] oldEntries = data;
		Entry<K, V>[] newEntries = new Entry[nrOfEntries];
		
		// Construct temporary entries that function as roots for the entries that remain in
		// the current bucket and those that are being shifted.
		Entry<K, V> currentEntryRoot = new Entry<K, V>(0, null, null, null);
		Entry<K, V> shiftedEntryRoot = new Entry<K, V>(0, null, null, null);
		
		int oldSize = oldEntries.length;
		for(int i = oldSize - 1; i >= 0; i--){
			Entry<K, V> e = oldEntries[i];
			if(e != null){
				Entry<K, V> lastCurrentEntry = currentEntryRoot;
				Entry<K, V> lastShiftedEntry = shiftedEntryRoot;
				do{
					int position = e.hash & newHashMask;
					
					if(position == i){
						lastCurrentEntry.next = e;
						lastCurrentEntry = e;
					}else{
						lastShiftedEntry.next = e;
						lastShiftedEntry = e;
					}
					e = e.next;
				}while(e != null);
				
				// Set the next pointers of the last entries in the buckets to null.
				lastCurrentEntry.next = null;
				lastShiftedEntry.next = null;
				
				newEntries[i] = currentEntryRoot.next;
				newEntries[i | oldSize] = shiftedEntryRoot.next; // The entries got shifted by the size of the old table.
			}
		}
		
		data = newEntries;
		hashMask = newHashMask;
		threshold <<= 1;

/*		
		modSize++;
		int tableSize = 1 << modSize;
		hashMask = tableSize - 1;
		Entry<K, V>[] newData = (Entry<K, V>[]) new Entry[tableSize];

		threshold = tableSize;
		
		Entry<K, V>[] oldData = data;
		for(int i = oldData.length - 1; i >= 0; i--){
			Entry<K, V> entry = oldData[i];
			
			if(entry != null){
				// Determine the last unchanged entry chain.
				Entry<K, V> lastUnchangedEntryChain = entry;
				int newLastUnchangedEntryChainIndex = entry.hash & hashMask;
				
				Entry<K, V> e = entry.next;
				while(e != null){
					int newIndex = e.hash & hashMask;
					if(newIndex != newLastUnchangedEntryChainIndex){
						lastUnchangedEntryChain = e;
						newLastUnchangedEntryChainIndex = newIndex;
					}
					
					e = e.next;
				}
	
				newData[newLastUnchangedEntryChainIndex] = lastUnchangedEntryChain;
				
				// Reconstruct the other entries (if necessary).
				while(entry != lastUnchangedEntryChain){
					int hash = entry.hash;
					int position = hash & hashMask;
					newData[position] = new Entry<K, V>(hash, entry.key, entry.value, newData[position]);
					
					entry = entry.next;
				}
			}
		}
		
		data = newData; 
		*/
	}
	
	/**
	 * Makes sure the size of the entry array and the load of the map stay in proper relation to
	 * eachother.
	 */
	private void ensureCapacity(){
		if(load > threshold){
			rehash();
		}
	}
	
	/**
	 * Replaces the value in the entry by the given value.
	 * 
	 * @param position
	 *            The position in the entry array where the entry is located.
	 * @param entry
	 *            The entry in which the value must be replaced.
	 * @param newValue
	 *            The value.
	 */
	private void replaceValue(int position, Entry<K, V> entry, V newValue){
		Entry<K, V> e = data[position];
		
		// Reconstruct the updated entry.
		data[position] = new Entry<K, V>(entry.hash, entry.key, newValue, entry.next);
		
		// Reconstruct the other entries (if necessary).
		while(e != entry){
			data[position] = new Entry<K, V>(e.hash, e.key, e.value, data[position]);
			
			e = e.next;
		}
	}
	
	/**
	 * Inserts the given key-value pair into this map. In case there already is a value associated
	 * with the given key, the value will be updated and the previous value returned.
	 * 
	 * @param key
	 *            The key
	 * @param value
	 *            The value
	 * @return The previous value that was associated with the key (if any); null otherwise.
	 */
	public V put(K key, V value){
		ensureCapacity();
		
		int hash = key.hashCode();
		int position = hash & hashMask;
		
		Entry<K, V> currentStartEntry = data[position];
		// Check if the key is already in here.
		if(currentStartEntry != null){
			Entry<K, V> entry = currentStartEntry;
			do{
				if(hash == entry.hash && entry.key.equals(key)){ // Replace if present.
					replaceValue(position, entry, value);
					return entry.value; // Return the old value.
				}
				entry = entry.next;
			}while(entry != null);
		}
		
		data[position] = new Entry<K, V>(hash, key, value, currentStartEntry); // Insert the new entry.
		
		load++;
		
		currentHashCode ^= hash; // Update the current hashcode of this map.
		
		return null;
	}
	
	public V unsafePut(K key, V value){
		ensureCapacity();
		
		int hash = key.hashCode();
		int position = hash & hashMask;
		
		Entry<K, V> currentStartEntry = data[position];
		
		data[position] = new Entry<K, V>(hash, key, value, currentStartEntry); // Insert the new entry.
		
		load++;
		
		currentHashCode ^= hash; // Update the current hashcode of this map.
		
		return null;
	}

	/**
	 * Removes the entry from this map that is identified by the given key (if present).
	 * 
	 * @param key
	 *            The key that identifies the entry to remove.
	 * @return The value that was associated with the given key; null if the key was not present in
	 * the map.
	 */
	public V remove(Object key){
		int hash = key.hashCode();
		int position = hash & hashMask;
		
		Entry<K, V> entry = data[position];
		if(entry != null){
			if (hash == entry.hash && entry.key.equals(key)) {
				data[position] = entry.next;
				load--;
				return entry.value;
			} else {
				Entry<K, V> prev = entry;				
				entry = entry.next;
				
				while (entry != null && !(hash == entry.hash && entry.key.equals(key))) {
					prev = entry;
					entry = entry.next;
				}
				
				if (entry != null) {
					prev.next = entry.next;
					load--;
					return entry.value;
				}
				
			}
				
				
			/*Entry<K, V> entry = currentStartEntry;
			do{
				if(hash == entry.hash && entry.key.equals(key)){
					Entry<K, V> e = data[position];
					
					data[position] = entry.next;
					// Reconstruct the other entries (if necessary).
					while(e != entry){
						data[position] = new Entry<K, V>(e.hash, e.key, e.value, data[position]);
						
						e = e.next;
					}
					
					load--;
					
					currentHashCode ^= hash; // Update the current hashcode of this map.
					
					return entry.value; // Return the value.
				}
				
				entry = entry.next;
			}while(entry != null);*/
		}
		
		return null; // Not found.
	}
	
	/**
	 * Retrieves the value from the entry in this map which is identified by the given key
	 * (if present).
	 * 
	 * @param key
	 *            The key that identifies the entry that contains the value.
	 * @return The retrieved value; null if not present.
	 */
	public V get(Object key){
		int hash = key.hashCode();
		int position = hash & hashMask;
		
		Entry<K, V> entry = data[position];
		while(entry != null){
			if(hash == entry.hash && key.equals(entry.key)) return entry.value;
			
			entry = entry.next;
		}
		
		return null;
	}
	
	/**
	 * Checks if there is an entry present in this map, which is identified by the given key.
	 * 
	 * @param key
	 *            The key that identifies the entry.
	 * @return True if this map contains an entry which is identified by the given key;
	 * false otherwise.
	 */
	public boolean contains(K key){
		return (get(key) != null);
	}
	
	/**
	 * Returns the number of entries this map contains.
	 * 
	 * @return The number of entries this map contains.
	 */
	public int size(){
		return load;
	}
	
	/**
	 * Checks whether or not this map is empty.
	 * 
	 * @return True if this map was empty; false otherwise.
	 */
	public boolean isEmpty(){
		return (load == 0);
	}
	
	/**
	 * Constructs an iterator for the entries in this map.
	 * 
	 * @return An iterator for the entries in this map.
	 */
	public Iterator<Map.Entry<K, V>> entryIterator(){
		return new EntryIterator<K, V>(this);
	}
	
	/**
	 * Constructs an iterator for the keys in this map.
	 * 
	 * @return An iterator for the keys in this map.
	 */
	public Iterator<K> keysIterator(){
		return new KeysIterator<K, V>(this);
	}
	
	/**
	 * Constructs an iterator for the values in this map.
	 * 
	 * @return An iterator for the values in this map.
	 */
	public Iterator<V> valuesIterator(){
		return new ValuesIterator<K, V>(this);
	}
	
	/**
	 * Copies over all entries from the given map, to this map.
	 */
	@SuppressWarnings("unchecked")
	public void putAll(Map<? extends K, ? extends V> otherMap){
		Set<Map.Entry<K, V>> entrySet = (Set<Map.Entry<K, V>>) (Set<?>) otherMap.entrySet(); // Generics stink.
		Iterator<Map.Entry<K, V>> entrySetIterator = entrySet.iterator();
		while(entrySetIterator.hasNext()){
			Map.Entry<K, V> next = entrySetIterator.next();
			put(next.getKey(), next.getValue());
		}
	}
	
	/**
	 * Checks if this map contains an entry with the given key.
	 */
	public boolean containsKey(Object key){
		int hash = key.hashCode();
		int position = hash & hashMask;
		
		Entry<K, V> entry = data[position];
		while(entry != null){
			if(hash == entry.hash && key.equals(entry.key)) return true;
			
			entry = entry.next;
		}
		
		return false;
	}
	
	/**
	 * Checks if this map contains an entry with the given value.
	 */
	public boolean containsValue(Object value){
		Iterator<V> valuesIterator = valuesIterator();
		while(valuesIterator.hasNext()){
			V nextValue = valuesIterator.next();
			if(nextValue == value || (nextValue != null && nextValue.equals(value))){
				return true;
			}
		}

		return false;
	}
	
	/**
	 * Constructs a set containing all entries from this map.
	 */
	public Set<Map.Entry<K, V>> entrySet(){
		ShareableHashSet<Map.Entry<K, V>> entrySet = new ShareableHashSet<Map.Entry<K, V>>();
		
		Iterator<Map.Entry<K, V>> entriesIterator = entryIterator();
		while(entriesIterator.hasNext()){
			entrySet.add(entriesIterator.next());
		}
		
		return entrySet;
	}
	
	/**
	 * Constructs a set containing all keys from this map.
	 */
	public Set<K> keySet(){
		ShareableHashSet<K> keysSet = new ShareableHashSet<K>();
		
		Iterator<K> keysIterator = keysIterator();
		while(keysIterator.hasNext()){
			keysSet.add(keysIterator.next());
		}
		
		return keysSet;
	}
	
	/**
	 * Constructs a collection containing all values from this map.
	 */
	public Collection<V> values(){
		ShareableHashSet<V> valuesSet = new ShareableHashSet<V>();
		
		Iterator<V> valuesIterator = valuesIterator();
		while(valuesIterator.hasNext()){
			valuesSet.add(valuesIterator.next());
		}
		
		return valuesSet;
	}
	
	/**
	 * Prints the internal representation of this map to a string.
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		StringBuilder buffer = new StringBuilder();
		
		buffer.append('{');
		for(int i = 0; i < data.length; i++){
			buffer.append('[');
			Entry<K, V> e = data[i];
			if(e != null){
				buffer.append(e);
				
				e = e.next;
				
				while(e != null){
					buffer.append(',');
					buffer.append(e);
					
					e = e.next;
				}
			}
			buffer.append(']');
		}
		buffer.append('}');
		
		return buffer.toString();
	}
	
	/**
	 * Returns the current hash code of this map.
	 * 
	 * @return The current hash code of this map.
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode(){
		return currentHashCode;
	}
	
	/**
	 * Check whether or not the current content of this set is equal to that of the given object / map. 
	 * 
	 * @return True if the content of this set is equal to the given object / map.
	 * 
	 * @see java.lang.Object#equals(Object)
	 */
	@SuppressWarnings("unchecked")
	public boolean equals(Object o){
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			ShareableHashMap<K, V> other = (ShareableHashMap<K, V>) o;
			
			if(other.currentHashCode != currentHashCode) return false;
			if(other.size() != size()) return false;
		
			if(isEmpty()) return true; // No need to check if the maps are empty.
			
			Iterator<Map.Entry<K, V>> otherIterator = other.entryIterator();
			while(otherIterator.hasNext()){
				Map.Entry<K, V> entry = otherIterator.next();
				V otherValue = entry.getValue();
				V thisValue = get(entry.getKey());
				if(otherValue != thisValue && thisValue != null && !thisValue.equals(entry.getValue())) return false;
			}
			return true;
		}
		
		return false;
	}
	
	/**
	 * Entry, used for containing key-value pairs and constructing buckets.
	 *  
	 * @author Arnold Lankamp
	 *
	 * @param <K>
	 *            The key type
	 * @param <V>
	 *            The value type
	 */
	private static class Entry<K, V> implements Map.Entry<K, V>{
		public final int hash;
		public final K key;
		public final V value;
		
		public Entry<K, V> next;
		
		/**
		 * Constructor
		 * 
		 * @param hash
		 *            The hash code of the key
		 * @param key
		 *            The key
		 * @param value
		 *            The value
		 * @param next
		 *            A reference to the next entry in the bucket (if any).
		 */
		public Entry(int hash, K key, V value, Entry<K, V> next){
			super();
			
			this.hash = hash;
			this.key = key;
			this.value = value;
			
			this.next = next;
		}
		
		/**
		 * Returns a reference to the key.
		 * 
		 * @return A reference to the key.
		 */
		public K getKey(){
			return key;
		}
		
		/**
		 * Returns a reference to the value.
		 * 
		 * @return A reference to the value.
		 */
		public V getValue(){
			return value;
		}
		
		/**
		 * Unsupported operation.
		 * 
		 * @param value
		 *        The value which we will not set.
		 * @return Null.
		 * @throws java.lang.UnsupportedOperationException
		 * 
		 * @see java.util.Map.Entry#setValue(Object)
		 */
		public V setValue(V value){
			throw new UnsupportedOperationException("The setting of values is not supported by this map implementation.");
		}

		/**
		 * Prints the internal representation of this entry to a string.
		 * 
		 * @see java.lang.Object#toString()
		 */
		public String toString(){
			StringBuilder buffer = new StringBuilder();
			
			buffer.append('<');
			buffer.append(key);
			buffer.append(':');
			buffer.append(value);
			buffer.append('>');
			
			return buffer.toString();
		}
	}
	
	/**
	 * Iterator for entries.
	 * 
	 * @author Arnold Lankamp
	 *
	 * @param <K>
	 *            The key type.
	 * @param <V>
	 *            The value type.
	 */
	private static class EntryIterator<K, V> implements Iterator<Map.Entry<K, V>>{
		private final Entry<K, V>[] data;
		
		private Entry<K, V> current;
		private int index;
		
		/**
		 * Constructor.
		 * 
		 * @param sharedHashMap
		 *            The map to iterator over.
		 */
		public EntryIterator(ShareableHashMap<K, V> sharedHashMap){
			super();
			
			data = sharedHashMap.data;

			current = null;			
			for(int i = data.length - 1; i >= 0 ; i--){
				Entry<K, V> entry = data[i];
				if(entry != null){
					current = entry;
					index = i;
					return;
				}
			}
		}
		
		/**
		 * Locates the next entry in the map.
		 */
		private void locateNext(){
			Entry<K, V> next = current.next;
			if(next != null){
				current = next;
				return;
			}
			
			for(int i = index - 1; i >= 0 ; i--){
				Entry<K, V> entry = data[i];
				if(entry != null){
					current = entry;
					index = i;
					return;
				}
			}
			
			current = null;
			index = 0;
		}
		
		/**
		 * Check if there are more elements in this iteration.
		 * 
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext(){
			return (current != null);
		}
		
		/**
		 * Returns the next element in this iteration.
		 * 
		 * @return The next element in this iteration.
		 * @throws NoSuchElementException
		 *            Thrown if there are no more elements in this iteration when calling this
		 *            method.
		 * 
		 * @see java.util.Iterator#next()
		 */
		public Entry<K, V> next(){
			if(!hasNext()) throw new UnsupportedOperationException("There are no more elements in this iterator.");
			
			Entry<K, V> entry = current;
			locateNext();
			
			return entry;
		}
		
		/**
		 * Removal is not supported by this iterator.
		 * 
		 * @throws java.lang.UnsupportedOperationException
		 * 
		 * @see java.util.Iterator#remove()
		 */
		public void remove(){
			throw new UnsupportedOperationException("This iterator doesn't support removal.");
		}
	}
	
	/**
	 * Iterator for keys.
	 * 
	 * @author Arnold Lankamp
	 *
	 * @param <K>
	 *            The key type.
	 * @param <V>
	 *            The value type.
	 */
	private static class KeysIterator<K, V> implements Iterator<K>{
		private final EntryIterator<K, V> entryIterator;
		
		/**
		 * Constructor.
		 * 
		 * @param sharedHashMap
		 *            The map to iterate over.
		 */
		public KeysIterator(ShareableHashMap<K, V> sharedHashMap){
			super();
			
			entryIterator = new EntryIterator<K, V>(sharedHashMap);
		}
		
		/**
		 * Check if there are more elements in this iteration.
		 * 
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext(){
			return entryIterator.hasNext();
		}
		
		/**
		 * Returns the next element in this iteration.
		 * 
		 * @return The next element in this iteration.
		 * @throws NoSuchElementException
		 *            Thrown if there are no more elements in this iteration when calling this
		 *            method.
		 * 
		 * @see java.util.Iterator#next()
		 */
		public K next(){
			return entryIterator.next().key;
		}
		
		/**
		 * Removal is not supported by this iterator.
		 * 
		 * @throws java.lang.UnsupportedOperationException
		 * 
		 * @see java.util.Iterator#remove()
		 */
		public void remove(){
			throw new UnsupportedOperationException("This iterator doesn't support removal.");
		}
	}

	/**
	 * Iterator for values.
	 * 
	 * @author Arnold Lankamp
	 *
	 * @param <K>
	 *            The key type.
	 * @param <V>
	 *            The value type.
	 */
	private static class ValuesIterator<K, V> implements Iterator<V>{
		private final EntryIterator<K, V> entryIterator;
		
		/**
		 * Constructor.
		 * 
		 * @param sharedHashMap
		 *            The map to iterate over.
		 */
		public ValuesIterator(ShareableHashMap<K, V> sharedHashMap){
			super();
			
			entryIterator = new EntryIterator<K, V>(sharedHashMap);
		}
		
		/**
		 * Check if there are more elements in this iteration.
		 * 
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext(){
			return entryIterator.hasNext();
		}
		
		/**
		 * Returns the next element in this iteration.
		 * 
		 * @return The next element in this iteration.
		 * @throws NoSuchElementException
		 *            Thrown if there are no more elements in this iteration when calling this
		 *            method.
		 * 
		 * @see java.util.Iterator#next()
		 */
		public V next(){
			return entryIterator.next().value;
		}
		
		/**
		 * Removal is not supported by this iterator.
		 * 
		 * @throws java.lang.UnsupportedOperationException
		 * 
		 * @see java.util.Iterator#remove()
		 */
		public void remove(){
			throw new UnsupportedOperationException("This iterator doesn't support removal.");
		}
	}

	public Iterator<java.util.Map.Entry<K, V>> iterator() {
		return entryIterator();
	}
}
