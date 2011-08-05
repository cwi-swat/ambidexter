package nl.cwi.sen1.AmbiDexter.util;

import java.util.Collection;
import java.util.Iterator;

public class Queue<E> implements Stack<E> {

	private Queue<E> next = null;
	private Object[] data;
	private int size;
	private int maxSize;
	
	public Queue() {
		this(2048);
	}
		
	public Queue(int max) {
		maxSize = max;
		size = 0;
		data = new Object[maxSize];
	}

	// clone
	public Queue(Queue<E> q) {
		maxSize = q.maxSize;
		size = q.size;
		data = q.data.clone();
		if (q.next != null) {
			next = new Queue<E>(q.next);
		}
	}
	
	// push
	public boolean add(E o) {
		if (size == data.length) {
			if (next == null) {
				next = new Queue<E>(maxSize);
			}
			return next.add(o);
		}
		data[size] = o;
		size++;
		return true;
	}
	
	public boolean addAll(Collection<? extends E> c) {
		// TODO do arraycopy if c instanceof queue
		for (E e : c) {
			add(e);
		}
		return true;
	}

	public void clear() {
		size = 0;
		data = new Object[maxSize];
		next = null;
	}

	public void quickClear() {
		size = 0;
		next = null;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public int size() {
		return size + (next == null ? 0 : next.size());
	}
	
	public int allocatedSize() {
		return data.length + (next == null ? 0 : next.allocatedSize());
	}
	
	@SuppressWarnings("unchecked")
	public E get(int i) { // XXX does not update size!!!
		if (i >= data.length) {
			if (next != null) {
				return next.get(i - data.length);
			} else {
				return null;
			}
		}
		return (E) data[i];
	}
	
	public void set(int i, E e) { // XXX does not update size!!!
		if (i >= data.length) {
			if (next == null) {
				next = new Queue<E>(maxSize);
			}
			next.set(i - data.length, e);
		} else {
			data[i] = e;
		}
	}
	
	@SuppressWarnings("unchecked")
	public E pop() {
		if (next != null) {
			E e = next.pop();
			if (e == null) {
				next = null;
			} else {
				return e;
			}
		}
		if (size == 0) {
			return null;
		}
		return (E) data[--size];
	}
	
	@SuppressWarnings("unchecked")
	public E peek() {
		if (next != null) {
			return next.peek();
		}
		if (size == 0) {
			return null;
		}
		return (E) data[size - 1];
	}
	
	public void searchIdentical() {
		for (int i = 0; i < size; i++) {
			Object o = data[i];
			Queue<E> q = this;
			int j = i + 1;
			while(q != null) {
				for (; j < q.size; j++) {
					if (q.data[j].equals(o)) {
						System.out.println("Identical: " + o);
						if (!o.equals(q.data[j])) {
							System.out.println("But different...");
						}
					}
				}
				q = q.next;
				j = 0;
			}
		}
		if (next != null) {
			next.searchIdentical();
		}
	}
	
	public boolean contains(Object o) {
		for (int i = 0; i < size; i++) {
			if (data[i].equals(o)) {
				return true;
			}			
		}
		if (next != null) {
			return next.contains(o);
		}
		return false;
	}
	
	public boolean containsRef(Object o) {
		for (int i = 0; i < size; i++) {
			if (data[i] == o) {
				return true;
			}			
		}
		if (next != null) {
			return next.containsRef(o);
		}
		return false;
	}

	@Override
	public String toString() {
		String s = "[";
		for (int i = 0; i < size; i++) {
			if (i == 0) {
				s = s + get(i);
			} else {
				s = s + "," + get(i);
			}
		}
		return s + "]";
	}
	
	public boolean containsAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	public Iterator<E> iterator() {
		return new QueueIterator<E>(this);
	}
	
	public boolean remove(Object o) {
		for (int i = 0; i < size; i++) {
			if (data[i].equals(o)) {
				remove(i);
				return true;
			}			
		}
		if (next != null) {
			return next.remove(o);
		}
		return false;
		//throw new RuntimeException("Queue.remove not implemented");
	}
	
	public void remove(int i) {
		if (i >= size) {
			next.remove(i - size);
		} else {
			data[i] = pop();
		}
	}
	
	public boolean removeAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	public Object[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	public <T> T[] toArray(T[] a) {
		// TODO Auto-generated method stub
		return null;
	}

	public static class QueueIterator<E> implements Iterator<E> {
		Queue<E> queue;
		int pos = 0;
		
		public QueueIterator(Queue<E> q) {
			queue = q;
		}
		
		public boolean hasNext() {
			return pos < queue.size;
		}
		
		@SuppressWarnings("unchecked")
		public E next() {
			E e = (E) queue.data[pos];
			pos++;
			if (pos == queue.maxSize && queue.next != null) {
				queue = queue.next;
				pos = 0;
			}
			return e;
		}
		
		public void remove() {
			throw new RuntimeException("QueueIterator.remove not possible");			
		}
	}
}
