package nl.cwi.sen1.AmbiDexter.nu2;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.zip.Deflater;


public class ItemPairArrayHashSet implements IItemPairSet {

	private Bucket[] buckets;
	int totalsize = 0;
	
	public ItemPairArrayHashSet(int items) {
		buckets = new Bucket[items];
		for (int i = buckets.length - 1; i >= 0; --i) {
			buckets[i] = new Bucket();
		}
	}

	public ItemPair getContained(long items, long flags) {
		int pos = (int)((items & ItemPair.ITEM_MASK_2) >>> ItemPair.ITEM_BITS);
		return buckets[pos].getContained(items, flags);
	}

	public ItemPair unsafeAdd(long items, long flags) {
		++totalsize;
		int pos = (int)((items & ItemPair.ITEM_MASK_2) >>> ItemPair.ITEM_BITS);
		return buckets[pos].unsafeAdd(items, flags);
	}
	
	public String usageStatistics() {
		String s = "Size: " + size();
		
		int maxBucketSize = 0;
		int bucketBuckets = 0;
		int totalSpace = 0;
		int maxBucketBucketLength = 0;
		for (int i = buckets.length - 1; i >= 0; --i) {
			int l = buckets[i].size();
			if (l > maxBucketSize) {
				maxBucketSize = l;
			}
			
			for (int j = buckets[i].sizes.length - 1; j >= 0; --j) {
				if (buckets[i].sizes[j] > 0) {
					++bucketBuckets;				
					totalSpace += buckets[i].data[j].length;
					if (buckets[i].data[j].length > maxBucketBucketLength) {
						maxBucketBucketLength = buckets[i].data[j].length;
					}
				}
			}
		}

		s += ", space: " + totalSpace;
		s += ", maxbucketsize: " + maxBucketSize;
		s += ", level2buckets: " + bucketBuckets;
		s += ", maxlevel2bucketlength: " + maxBucketBucketLength;
		return s;
	}

	public int size() {
		/*int size = 0;
		for (int i = buckets.length - 1; i >= 0; --i) {
			size += buckets[i].size();
		}
		return size;*/
		return totalsize;
	}
	
	public long getCompressedSize() {
		long sum = 0;
		for (int i = buckets.length - 1; i >= 0; --i) {
			sum += buckets[i].getCompressedSize();
		}
		return sum;
	}

	public Iterator<ItemPair> iterator() {
		return new ItemPairAHSIterator(this);
	}
	
	public static class Bucket implements IItemPairSet {
		private final static int INITIAL_LOG_SIZE = 5;
		private final static int INITIAL_BUCKET_SIZE = 2;
		private final static int MAX_BUCKET_SIZE = 2048;

		// TODO can we sort upon rehash? for each long[] have one sorted part and an unsorted part that grows with insertions
		
		private long[][] data; // rename keys
		public long[][] properties; // TODO would it be faster to combine properties and data, interleaved?
		private int[] sizes;
		//private long[] bloomfilters; // tried but with no effect, need way more bits than 64, even for bucket length of 256!!
		
		public Bucket() {
			super();
			
			int tableSize = 1 << INITIAL_LOG_SIZE;
			data = new long[tableSize][];
			properties = new long[tableSize][];
			sizes = new int[tableSize];
		}
		
		
		public ItemPair getContained(long items, long flags) {
			final long id = (items & ItemPair.ITEM_MASK_1) | (flags << ItemPair.ITEM_BITS);
			final int pos = (int)(id & (sizes.length - 1));
			final long[] entry = data[pos];
			
			if (entry != null) {
				for (int i = sizes[pos] - 1; i >= 0; --i) {
					// XXX reversing this loop really made a difference: from 1m33 to 1m02 for C :-)
					// find out if it the code is just faster, or that we find a pair earlier...
					if (entry[i] == id) {
						return new ItemPair(items, flags, this, pos, i);
					}
				}
			}
			
			return null;
		}
		
		//static int maxlevel2buckets = 0;

		public ItemPair unsafeAdd(long items, long flags) {
			final long id = (items & ItemPair.ITEM_MASK_1) | (flags << ItemPair.ITEM_BITS);			
			int pos = (int)(id & (sizes.length - 1));
			long[] entry = data[pos];
			
			if (entry == null) {
				entry = data[pos] = new long[INITIAL_BUCKET_SIZE];
				properties[pos] = new long[INITIAL_BUCKET_SIZE];
				entry[0] = id;
				//properties[pos][0] = 0;
				sizes[pos] = 1;
				return new ItemPair(items, flags, this, pos, 0);
			} else {
				final int size = sizes[pos]; 
				if (size == data[pos].length) {
					if (size >= MAX_BUCKET_SIZE) {
						rehash(id);
						pos = (int)(id & (sizes.length - 1));
					} else {
						int growth;
						if (size <= 64) {
							growth = (size + 1) / 2;
						} else {
							growth = size / 10;
						}
						int newSize = size + growth;

						final long[] newData = new long[newSize];
						System.arraycopy(data[pos], 0, newData, 0, size);
						data[pos] = newData;
						
						final long[] newProps = new long[newSize];
						System.arraycopy(properties[pos], 0, newProps, 0, size);
						properties[pos] = newProps;
					}
				}
				
				int i = sizes[pos]++;
				data[pos][i] = id;
				
				// TODO we could save even more memory if we remove the pos bits from id, and tightly pack all ids
				// then we should also take the maximum used bits into account (not all flags are used)
				
				//properties[pos][i] = 0;
				return new ItemPair(items, flags, this, pos, i);
			}
		}

		public String usageStatistics() {
			return null;
		}

		public int size() {
			int size = 0;
			for (int i = sizes.length - 1; i >= 0; --i) {
				size += sizes[i];
			}
			return size;
		}

		public Iterator<ItemPair> iterator() {
			return null;
		}
		
		private void rehash(long idToBeAdded){
			final int newTableSize = sizes.length << 1;
			final int newHashMask = newTableSize - 1;
			
			final int[] newSizes = new int[newTableSize];
			for (int i = data.length - 1; i >= 0; --i) {
				final long[] entry = data[i];
				for (int j = sizes[i] - 1; j >= 0; --j) {
					++newSizes[(int)(entry[j] & newHashMask)];
				}
			}
			++newSizes[(int)(idToBeAdded & newHashMask)];
			
			final long[][] newData = new long[newTableSize][];
			final long[][] newProperties = new long[newTableSize][];
			
			// create new buckets
			for (int i = newTableSize - 1; i >= 0; --i) {
				if (newSizes[i] > 0) {
					int s = newSizes[i]; //closestTwoPower(newSizes[i]);
					newData[i] = new long[s];
					newProperties[i] = new long[s];
					newSizes[i] = 0;
				}
			}
			
			// reinsert data
			for (int i = data.length - 1; i >= 0; --i) {
				final long[] dEntry = data[i];
				final long[] pEntry = properties[i];
				for (int j = sizes[i] - 1; j >= 0; --j) {
					int pos = (int)(dEntry[j] & newHashMask);
					int p = newSizes[pos]++;
					long id = dEntry[j];
					newData[pos][p] = id;
					newProperties[pos][p] = pEntry[j];					
				}
			}
			
			data = newData;
			properties = newProperties;
			sizes = newSizes;
		}

		public void relookup(ItemPair p) {
			final long id = (p.items & ItemPair.ITEM_MASK_1) | (p.flags << ItemPair.ITEM_BITS);
			final int pos = (int)(id & (sizes.length - 1));
			final long[] entry = data[pos];
			
			for (int i = sizes[pos] - 1; i >= 0; --i) {
				if (entry[i] == id) {
					p.propArray = pos;
					p.propIndex = i;
					return;
				}
			}
		}


		public long getCompressedSize() {
			int sum = 0;
			for (int i = sizes.length - 1; i >= 0; --i) {
				sum += getCompressedSize(i);
			}
			return sum;
		}


		private long getCompressedSize(int j) {
			long[] entry = data[j];
			if (entry == null) {
				return 0;
			}
			
			int longs = sizes[j];
			ByteArrayOutputStream baos = new ByteArrayOutputStream(longs * 8);
			DataOutputStream dos = new DataOutputStream(baos);
			
			try {
				for (int i = 0; i < longs; ++i) {
					dos.writeLong(entry[i]);
				}
				dos.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Deflater def = new Deflater(9);
			def.setInput(baos.toByteArray());
			def.finish();
		
			byte[] b = new byte[longs * 8];
			long compressed = def.deflate(b);
			
			def.end();
			
			try {
				dos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//System.out.println("--- " + (longs * 8) + " --> " + compressed);
			return compressed;
		}		
	}

	public class ItemPairAHSIterator implements Iterator<ItemPair> {

		ItemPairArrayHashSet set;
		Bucket bucket;
		int bucketIndex;
		int arrayIndex;
		int pos;
		
		public ItemPairAHSIterator(ItemPairArrayHashSet set) {
			this.set = set;
			bucketIndex = 0;
			bucket = set.buckets[bucketIndex];
			arrayIndex = 0;
			pos = -1;
		}
		
		public boolean hasNext() {
			if (++pos >= bucket.sizes[arrayIndex]) {
				// end of array, next array
				pos = 0;
				do {
				  ++arrayIndex;
				} while (arrayIndex < bucket.data.length && bucket.sizes[arrayIndex] == 0);
				
				if (arrayIndex == bucket.data.length) {
					// end of bucket, next bucket
					while (true) {
						++bucketIndex;
						if (bucketIndex < set.buckets.length) {
							bucket = set.buckets[bucketIndex];
							arrayIndex = 0;
							while (true) {
								if (arrayIndex < bucket.data.length) {
									if (bucket.sizes[arrayIndex] == 0) {
										++arrayIndex;
									} else {
										return true;
									}
								} else {
									break;
								}
							}							
						} else {
							return false;
						}						
					}	
				}
			}
			return true;
		}

		public ItemPair next() {
			long id = bucket.data[arrayIndex][pos];
			long items = ((long)bucketIndex << ItemPair.ITEM_BITS) | (id & ItemPair.ITEM_MASK_1);
			long flags = id >>> ItemPair.ITEM_BITS;
			return new ItemPair(items, flags, bucket, arrayIndex, pos);
		}

		public void remove() {
			throw new RuntimeException("Remove not supported");
		}
	}
}
