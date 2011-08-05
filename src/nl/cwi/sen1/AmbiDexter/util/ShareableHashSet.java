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
import java.util.NoSuchElementException;

/**
 * This set implementation is shareable and can be easily cloned
 * (simple arraycopy of the entries array).
 * 
 * @author Arnold Lankamp
 * @author Bas Basten
 *
 * @param <V>
 *            The value type.
 */
public class ShareableHashSet<V> implements ESet<V>, Iterable<V>{
	private final static int INITIAL_LOG_SIZE = 4;

	private int modSize;
	private int hashMask;
	
	private Entry<V>[] data;
	
	private int threshold;
	
	private int load;
	
	private int currentHashCode;
	
	/**
	 * Default constructor.
	 */
	@SuppressWarnings({ "unchecked" })
	public ShareableHashSet(){
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
	 * Copy constructor.
	 * 
	 * @param sharedHashSet
	 *            The set to copy.
	 */
	@SuppressWarnings("unchecked")
	public ShareableHashSet(ShareableHashSet<V> s){
		super();

		modSize = s.modSize;
		int tableSize = 1 << modSize;
		hashMask = tableSize - 1;
		data = new Entry[tableSize];		
		threshold = tableSize;		
		load = 0;		
		currentHashCode = 0;
		
		addAll(s);
	}
	
	@SuppressWarnings("unchecked")
	public ShareableHashSet(int initialSize){
		super();
		
		modSize = closestPowerOfTwo(initialSize);
		int tableSize = 1 << modSize;
		hashMask = tableSize - 1;
		data = new Entry[tableSize];
		
		threshold = tableSize;
		
		load = 0;
		
		currentHashCode = 0;
	}

	private static int closestPowerOfTwo(int number){
		int power = 0;
		do{/* Nothing. */}while((1 << (++power)) < number);
		
		return power;
	}
	
	/**
	 * Removes all the entries from this set.
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
	 * Rehashes this set.
	 */
	@SuppressWarnings("unchecked")
	private void rehash(){
		
		int oldsize = size();
		
		int nrOfEntries = 1 << (++modSize);
		int newHashMask = nrOfEntries - 1;
		
		Entry<V>[] oldEntries = data;
		Entry<V>[] newEntries = new Entry[nrOfEntries];
		
		// Construct temporary entries that function as roots for the entries that remain in
		// the current bucket and those that are being shifted.
		Entry<V> currentEntryRoot = new Entry<V>(0, null, null);
		Entry<V> shiftedEntryRoot = new Entry<V>(0, null, null);
		
		int oldSize = oldEntries.length;
		for(int i = oldSize - 1; i >= 0; i--){
			Entry<V> e = oldEntries[i];
			if(e != null){
				Entry<V> lastCurrentEntry = currentEntryRoot;
				Entry<V> lastShiftedEntry = shiftedEntryRoot;
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

		if (oldsize != size()) throw new RuntimeException();
		
		/*modSize++;
		int tableSize = 1 << modSize;
		hashMask = tableSize - 1;
		Entry<V>[] newData = (Entry<V>[]) new Entry[tableSize];

		threshold = tableSize;
		
		Entry<V>[] oldData = data;
		for(int i = oldData.length - 1; i >= 0; i--){
			Entry<V> entry = oldData[i];
			
			if(entry != null){
				// Determine the last unchanged entry chain.
				Entry<V> lastUnchangedEntryChain = entry;
				int newLastUnchangedEntryChainIndex = entry.hash & hashMask;
				
				Entry<V> e = entry.next;
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
					newData[position] = new Entry<V>(hash, entry.value, newData[position]);
					
					entry = entry.next;
				}
			}
		}
		
		data = newData; */
	}
	
	/**
	 * Makes sure the size of the entry array and the load of the set stay in proper relation to
	 * eachother.
	 */
	private void ensureCapacity(){
		if(load > threshold){
			rehash();
		}
	}
	
	/**
	 * Inserts the given value into this set.
	 * 
	 * @param value
	 *            The value to insert.
	 * @return Returns true if this set didn't contain the given value yet; false if it did.
	 */
	public boolean add(V value){
		if (value == null)
			return false;
		
		ensureCapacity();
		
		int hash = value.hashCode();
		int position = hash & hashMask;
		
		Entry<V> currentStartEntry = data[position];
		// Check if the value is already in here.
		if(currentStartEntry != null){
			Entry<V> entry = currentStartEntry;
			do{
				if(hash == entry.hash && entry.value.equals(value)){
					return false; // Return false if it's already present.
				}
				
				entry = entry.next;
			}while(entry != null);
		}
		
		data[position] = new Entry<V>(hash, value, currentStartEntry); // Insert the new entry.
		
		load++;
		
		currentHashCode ^= hash; // Update the current hashcode of this map.
		
		return true;
	}

	public void unsafeAdd(V value) {
		ensureCapacity();
		
		int hash = value.hashCode();
		int position = hash & hashMask;
		
		Entry<V> currentStartEntry = data[position];
		
		data[position] = new Entry<V>(hash, value, currentStartEntry); // Insert the new entry.
		
		load++;
		
		currentHashCode ^= hash; // Update the current hashcode of this map.
	}

	public V removeOne() {
		for (int i = 0; i < data.length; i++) {
			Entry<V> e = data[i];
			if (e != null) {
				data[i] = e.next;
				load--;
				currentHashCode ^= e.hash;
				return e.value;
			}
		}
		return null;
	}
	
	public V getOne() {
		for (int i = 0; i < data.length; i++) {
			Entry<V> e = data[i];
			if (e != null) {
				return e.value;
			}
		}
		return null;
	}

	/**
	 * Checks if this set contains the given value.
	 * 
	 * @param value
	 *            The value to check for.
	 * @return True if this set contains the given value; false otherwise.
	 */
	public boolean contains(Object value){
		int hash = value.hashCode();
		int position = hash & hashMask;
		
		Entry<V> entry = data[position];
		while(entry != null){
			if (hash == entry.hash && value.equals(entry.value)) {
				return true;
			}
			entry = entry.next;
		}
		
		return false;
	}
	
	public boolean containsRef(Object value){
		int hash = value.hashCode();
		int position = hash & hashMask;
		
		Entry<V> entry = data[position];
		while(entry != null){
			if (hash == entry.hash && value == entry.value) {
				return true;
			}			
			entry = entry.next;
		}		
		return false;
	}

	/**
	 * if the given object or an equal one already exists, return this old object
	 * 
	 * 
	 * @author Bas Basten
	 */
	public V getContained(Object value){
		int hash = value.hashCode();
		int position = hash & hashMask;
		
		Entry<V> entry = data[position];
		while(entry != null){
			if(hash == entry.hash && value.equals(entry.value)) {
				return entry.value;
			}
			
			entry = entry.next;
		}
		
		return null;
	}
	/**
	 * Removes the given object from this set (if present.)
	 * 
	 * @param value
	 *            The value to remove.
	 * @return True if this set contained the given object; false otherwise.
	 */
	public boolean remove(Object value){
		int hash = value.hashCode();
		int position = hash & hashMask;
		
		Entry<V> entry = data[position];
		if(entry != null){
			if (hash == entry.hash && entry.value.equals(value)) {
				data[position] = entry.next;
				load--;
				currentHashCode ^= hash;
				return true;
			} else {
				Entry<V> prev = entry;				
				entry = entry.next;
				
				while (entry != null && !(hash == entry.hash && entry.value.equals(value))) {
					prev = entry;
					entry = entry.next;
				}
				
				if (entry != null) {
					prev.next = entry.next;
					load--;
					currentHashCode ^= hash;
					return true;
				}
				
			}

			/*Entry<V> entry = currentStartEntry;
			do{
				if(hash == entry.hash && entry.value.equals(value)){
					Entry<V> e = data[position];
					
					data[position] = entry.next;
					// Reconstruct the other entries (if necessary).
					while(e != entry){
						data[position] = new Entry<V>(e.hash, e.value, data[position]);
						
						e = e.next;
					}
					
					load--;
					
					currentHashCode ^= hash; // Update the current hashcode of this set.
					
					return true;
				}
				
				entry = entry.next;
			}while(entry != null);*/
		}
		
		return false;
	}
	
	/**
	 * Returns the number of values this set currently contains.
	 * 
	 * @return The number of values this set currently contains.
	 */
	public int size(){
		return load;
	}
	
	/**
	 * Checks whether or not this set is empty.
	 * 
	 * @return True is this set is empty; false otherwise.
	 */
	public boolean isEmpty(){
		return (load == 0);
	}
	
	/**
	 * Constructs an iterator for this set.
	 * 
	 * @return An iterator for this set.
	 * 
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<V> iterator(){
		return new SetIterator<V>(this);
	}
	
	/**
	 * Adds all the elements from the given collection to this set.
	 * 
	 * @param collection
	 *            The collection that contains the elements to add.
	 * @return True if this set changed; false if it didn't.
	 */
	public boolean addAll(Collection<? extends V> collection){
		boolean changed = false;
		
		Iterator<? extends V> collectionIterator = collection.iterator();
		while(collectionIterator.hasNext()){
			changed |= add(collectionIterator.next());
		}
		
		return changed;
	}
	
	/**
	 * Checks if the collection contains all the elements in the given collection.
	 * 
	 * @param collection
	 *            The collection that contains the elements to check for.
	 * @return True if this set contains all the elements in the given collections; false if it
	 * didn't.
	 */
	public boolean containsAll(Collection<?> collection){
		Iterator<?> collectionIterator = collection.iterator();
		while(collectionIterator.hasNext()){
			if(!contains(collectionIterator.next())) return false;
		}
		
		return true;
	}
	
	/**
	 * Removes all the elements from this set which are not present in the given collection.
	 * 
	 * @param collection
	 *            The collection that contains the elements which need to be retained.
	 * @return True if this set changed; false if it didn't.
	 */
	public boolean retainAll(Collection<?> collection){
		boolean changed = false;
		
		Iterator<V> valuesIterator = iterator();
		while(valuesIterator.hasNext()){
			V value = valuesIterator.next();
			if(!collection.contains(value)){
				remove(value);
				
				changed = true;
			}
		}
		
		return changed;
	}
	
	/**
	 * Removes all the elements in the given collection from this set.
	 * 
	 * @param collection
	 *            The collection that contains the elements to remove from this set.
	 * @return True if this set change; false if it didn't.
	 */
	public boolean removeAll(Collection<?> collection){
		boolean changed = false;
		
		Iterator<?> collectionIterator = collection.iterator();
		while(collectionIterator.hasNext()){
			Object value = collectionIterator.next();
			changed |= remove(value);
		}
		
		return changed;
	}
	
	/**
	 * Returns all the elements from this set in an array.
	 * 
	 * @return All the elements from this set in an array.
	 */
	public Object[] toArray(){
		Object[] values = new Object[load];
		
		Iterator<V> valuesIterator = iterator();
		int i = 0;
		while(valuesIterator.hasNext()){
			values[i++] = valuesIterator.next();
		}
		
		return values;
	}
	

	/**
	 * Returns all the elements from this set in an array.
	 * 
	 * @param array
	 *            The array to use; in case it isn't large enough a new one will be allocated.
	 * @return All the elements from this set in an array.
	 */
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] array){
		if(array.length < load) return (T[]) toArray();
		
		Iterator<V> valuesIterator = iterator();
		int i = 0;
		while(valuesIterator.hasNext()){
			array[i++] = (T) valuesIterator.next();
		}
		
		for(; i < load; i++){
			array[i] = null;
		}
		
		return array;
	}
	
	/**
	 * Prints the internal representation of this set to a string.
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		boolean comma = false;
		StringBuilder buffer = new StringBuilder();
		
		buffer.append('{');
		for(int i = 0; i < data.length; i++){
			//buffer.append('[');
			Entry<V> e = data[i];
			if(e != null){
				
				if (comma) {
					buffer.append(", ");
				} else {
					comma = true;
				}
				
				buffer.append(e);
				
				e = e.next;
				
				while(e != null){
					buffer.append(", ");
					buffer.append(e);
					
					e = e.next;
				}
			}
			//buffer.append(']');
		}
		buffer.append('}');
		
		return buffer.toString();
	}
	
	/**
	 * Returns the current hash code of this set.
	 * 
	 * @return The current hash code of this set.
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode(){
		return currentHashCode;
	}

	/**
	 * Check whether or not the current content of this set is equal to that of the given object / set. 
	 * 
	 * @return True if the content of this set is equal to the given object / set.
	 * 
	 * @see java.lang.Object#equals(Object)
	 */
	@SuppressWarnings("unchecked")
	public boolean equals(Object o){
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			ShareableHashSet<V> other = (ShareableHashSet<V>) o;
			
			if(other.currentHashCode != currentHashCode) return false;
			if(other.size() != size()) return false;
			
			if(isEmpty()) return true; // No need to check if the sets are empty.
			
			Iterator<V> otherIterator = other.iterator();
			while(otherIterator.hasNext()){
				if(!contains(otherIterator.next())) return false;
			}
			return true;
		}
		
		return false;
	}

	public boolean refEquals(ShareableHashSet<V> other){
		if (other == null) return false;
		if(other.currentHashCode != currentHashCode) return false;
		if(other.size() != size()) return false;
		
		if(isEmpty()) return true; // No need to check if the sets are empty.
		
		Iterator<V> otherIterator = other.iterator();
		while(otherIterator.hasNext()){
			if(!containsRef(otherIterator.next())) return false;
		}
		return true;
	}

	/**
	 * Entry, used for containing values and constructing buckets.
	 * 
	 * @author Arnold Lankamp
	 *
	 * @param <V>
	 *            The value type.
	 */
	private static class Entry<V>{
		public final int hash; // TODO do we need this???
		public final V value;
		
		public Entry<V> next;
		
		/**
		 * Constructor.
		 * 
		 * @param hash
		 *            The hash code of the value
		 * @param value
		 *            The value
		 * @param next
		 *            A reference to the next entry in the bucket (if any).
		 */
		public Entry(int hash, V value, Entry<V> next){
			super();
			
			this.hash = hash;
			this.value = value;
			
			this.next = next;
		}
		
		/**
		 * Prints the internal representation of this entry to a string.
		 * 
		 * @see java.lang.Object#toString()
		 */
		public String toString(){
			StringBuilder buffer = new StringBuilder();
			
			//buffer.append('<');
			buffer.append(value);
			//buffer.append('>');
			
			return buffer.toString();
		}
	}
	
	/**
	 * Iterator for this set.
	 * 
	 * @author Arnold Lankamp
	 *
	 * @param <V>
	 *            The value type.
	 */
	private static class SetIterator<V> implements Iterator<V>{
		private final Entry<V>[] data;
		
		private Entry<V> current;
		private int index;
		
		/**
		 * Constructor.
		 * 
		 * @param sharedHashSet
		 *            The set to iterator over.
		 */
		public SetIterator(ShareableHashSet<V> sharedHashSet){
			super();
			
			data = sharedHashSet.data;

			current = null;			
			for(int i = data.length - 1; i >= 0 ; i--){
				Entry<V> entry = data[i];
				if(entry != null){
					current = entry;
					index = i;
					return;
				}
			}
		}
		
		/**
		 * Locates the next value in the set.
		 */
		private void locateNext(){
			Entry<V> next = current.next;
			if(next != null){
				current = next;
				return;
			}
			
			for(int i = index - 1; i >= 0 ; i--){
				Entry<V> entry = data[i];
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
		 * Checks if there are more elements in this iteration.
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
		public V next(){
			if(!hasNext()) throw new NoSuchElementException("There are no more elements in this iteration");
			
			V value = current.value;
			locateNext();
			
			return value;
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
}
