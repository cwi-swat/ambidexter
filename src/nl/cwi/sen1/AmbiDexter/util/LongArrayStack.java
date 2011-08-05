package nl.cwi.sen1.AmbiDexter.util;


public class LongArrayStack {

	private long[][] data;
	private int size;
	private int fragmentSize;
	
	public LongArrayStack() {
		this(1024);
	}
	
	public LongArrayStack(int size) {
		fragmentSize = size;
		clear();
	}
	
	private void grow(int fragments) {
		long[][] olddata = data;
		data = new long[fragments][];
		for (int i = 0; i < olddata.length; ++i) {
			data[i] = olddata[i];
		}
		for (int i = olddata.length; i < fragments; ++i) {
			data[i] = new long[fragmentSize];
		}
	}
		
	public long get(int i) {
		/*if (i >= size) { // removed for speed
			return -1;
		}*/
		return data[i / fragmentSize][i % fragmentSize];
	}

	public long pop() {
		return get(--size);
	}
	
	public long peek() {
		return get(size - 1);
	}

	public void set(int i, long e) {
		if (i >= data.length * fragmentSize) {
			grow((i / fragmentSize) + 1);
		}
		data[i / fragmentSize][i % fragmentSize] = e;
		if (i >= size) {
			size = i + 1;
		}
	}

	public void add(long o) {
		if (size == data.length * fragmentSize) {
			grow(data.length * 2);
		}
		data[size / fragmentSize][size % fragmentSize] = o;
		++size;
	}

	public void clear() {
		// TODO retain first fragment?
		data = new long[1][fragmentSize];
		size = 0;
	}

	public int size() {
		return size;
	}
	
	public int allocatedSize() {
		return data.length * fragmentSize;
	}

	public boolean isEmpty() {
		return size == 0;
	}
	
	// returns next empty offset
	public int copyIntoArray(long[] array, int offset) {
		int s = size;
		int b = 0;
		long[] f = data[0];
		while (s > fragmentSize) {
			System.arraycopy(f, 0, array, offset, fragmentSize);
			s -= fragmentSize;
			f = data[++b];
			offset += fragmentSize;
		}
		System.arraycopy(f, 0, array, offset, s);
		return offset + s;
	}

	public void quickClear() {
		size = 0;
	}
}
