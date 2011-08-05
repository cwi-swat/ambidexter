package nl.cwi.sen1.AmbiDexter.util;


public class LongQueue {

	private LongQueue next = null;
	private long[] data;
	private int size;
	private int maxSize;
	
	public LongQueue() {
		this(2048);
	}
		
	public LongQueue(int max) {
		maxSize = max;
		size = 0;
		data = new long[maxSize];
	}

	// push
	public boolean add(long o) {
		if (size == data.length) {
			if (next == null) {
				next = new LongQueue(maxSize);
			}
			return next.add(o);
		}
		data[size] = o;
		size++;
		return true;
	}
	
	public void clear() {
		size = 0;
		data = new long[maxSize];
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
	
	public long get(int i) {
		if (i >= data.length) {
			if (next != null) {
				return next.get(i - data.length);
			} else {
				return -1;
			}
		}
		return data[i];
	}
	
	/*public void set(int i, long e) {
		if (i >= data.length) {
			if (next == null) {
				next = new LongQueue(maxSize);
			}
			next.set(i - data.length, e);
		} else {
			data[i] = e;
		}
	}*/
	
	public long pop() {
		if (next != null) {
			long e = next.pop();
			if (e == -1) {
				next = null;
			} else {
				return e;
			}
		}
		if (size == 0) {
			return -1;
		}
		return data[--size];
	}
	
	public long peek() {
		if (next != null) {
			return next.peek();
		}
		if (size == 0) {
			return -1;
		}
		return data[size - 1];
	}
	
	// returns next empty offset
	public int copyIntoArray(long[] array, int offset) {
		System.arraycopy(data, 0, array, offset, size);
		if (size == data.length && next != null) {
			return next.copyIntoArray(array, offset + size);
		} else {
			return offset + size;
		}
	}
}
