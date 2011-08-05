package nl.cwi.sen1.AmbiDexter.nu2;

import nl.cwi.sen1.AmbiDexter.automata.NFA;
import nl.cwi.sen1.AmbiDexter.automata.LALR1NFA.LALR1Item;
import nl.cwi.sen1.AmbiDexter.automata.LR1NFA.LR1Item;
import nl.cwi.sen1.AmbiDexter.automata.NFA.EndItem;
import nl.cwi.sen1.AmbiDexter.automata.NFA.Item;
import nl.cwi.sen1.AmbiDexter.automata.NFA.StartItem;
import nl.cwi.sen1.AmbiDexter.nu2.ItemPairArrayHashSet.Bucket;

public class ItemPair {
	
	// maximum nr of itempairs: 2^31 (b/c of encoding of properties)
	
	long items;
	protected long flags = 0;
	//private long properties; // layout: 1 bit onstack, 1 bit alive, 31 bit lowlink, 31 bit number;

	public Bucket bucket;
	public int propArray;
	public int propIndex;
	
	public boolean symmetrical = false;
	public boolean swapped = false;
	
	protected static final long ALLOW_PAIRWISE_REDUCE_1 = 0x01;
	protected static final long ALLOW_PAIRWISE_REDUCE_2 = 0x02;
	
	protected static int ITEM_BITS;// = 16;
	protected static long ITEM_MASK_1;// = 0x0000FFFF;
	protected static long ITEM_MASK_2;// = 0xFFFF0000;
	
	protected static int flagBits = 2;
	protected static long flagMask = 0x03;
		
	protected ItemPair() {
	}
	
	public static int newFlags(int nr) {
		if (nr + flagBits + 2 * ITEM_BITS > 64) {
			throw new RuntimeException("More than 64 bits required for encoding of item pairs");
		}		
		int pos = flagBits;
		flagBits += nr;
		flagMask = makeMask(flagBits, 0);
		return pos;
	}
	
	public static long makeMask(int nr, int offset) {
		long mask = 0;
		for (int i = nr - 1; i >= 0; --i) {
			mask |= 0x01L << i;
		}
		return mask << offset;
	}
	
	public static void initItemMasks(int nrItems) {
		ITEM_BITS = 1;
		ITEM_MASK_1 = 1;
		while ((1 << ITEM_BITS) < nrItems) {
			ITEM_MASK_1 |= (1 << ITEM_BITS);
			++ITEM_BITS;
		}
		ITEM_MASK_2 = ITEM_MASK_1 << ITEM_BITS;
		System.out.println("Item bits: " + ITEM_BITS);
	}

	public ItemPair(long items, long flags, Bucket bucket, int propArray, int propIndex) {
		this.items = items;
		this.flags = flags;
		this.bucket = bucket;
		this.propArray = propArray;
		this.propIndex = propIndex;
	}
	
	public ItemPair(Item i1, Item i2, boolean allowPWR1, boolean allowPWR2) {
		if (i1 == i2) {
			if (allowPWR1 == allowPWR2) {
				symmetrical = true;
			} else {
				swapped = !allowPWR1 && allowPWR2;
			}
		} else {
			swapped = i1.ID > i2.ID;
		}
		
		if (swapped) {
			Item i3 = i1;
			i1 = i2;
			i2 = i3;
			boolean b = allowPWR1;
			allowPWR1 = allowPWR2;
			allowPWR2 = b;
		}

		items = i1.ID | ((long)i2.ID << ITEM_BITS);
		if (allowPWR1) flags |= ALLOW_PAIRWISE_REDUCE_1;
		if (allowPWR2) flags |= ALLOW_PAIRWISE_REDUCE_2;
	}

	public Item getA() {
		return NFA.itemQueue.get((int) (items & ITEM_MASK_1));
	}

	public Item getB() {
		return NFA.itemQueue.get((int) (items >>> ITEM_BITS));
	}
	
	public boolean equalItems() {
		return (items & ITEM_MASK_1) == (items >>> ITEM_BITS);
	}

	public boolean getAllowPairwiseReduce1() {
		return (flags & ALLOW_PAIRWISE_REDUCE_1) > 0;
	}

	public boolean getAllowPairwiseReduce2() {
		return (flags & ALLOW_PAIRWISE_REDUCE_2) > 0;
	}
	
	public boolean inConflict() {
		return (flags & (ALLOW_PAIRWISE_REDUCE_1 | ALLOW_PAIRWISE_REDUCE_2)) > 0;
	}
	
	public void unsetAllowPairwiseReduce() {
		flags &= 0xFFFFFFFFFFFFFFFCL;
	}
	
	public int getDistanceToEnd() {
		return getA().distanceToEnd + getB().distanceToEnd;
	}
	
	public boolean isStartOrEndPair() {
		Item a = getA();
		Item b = getB();
		return (a instanceof StartItem || a instanceof EndItem) && (b instanceof StartItem || b instanceof EndItem) ;
	}
	
	public String toString() {
		String s = "(" + getA().toString() + ", " + getB().toString() + ")" + " " +
		(getAllowPairwiseReduce1()?"1":"0") + (getAllowPairwiseReduce2()?"1":"0");
		return s;
	}

	protected int getId() {
		// bitmask: (30 bit, to keep them positive, which is better for Dot)
		// 14 bit item1
		// 14 bit item2
		// 1 bit allowPairwiseReduce1
		// 1 bit allowPairwiseReduce2
		
		// 16,18: these weird shift offsets give the fastest result for grammar 130 with lr0 and slr1
		// 14,28: are better for lalr1 and lr1
		// TODO what if we put the flags as the first bits?
		if (getA() instanceof LR1Item || getA() instanceof LALR1Item) {
			return getA().hashCode() + (getB().hashCode() << 14) + ((int)(flags & flagMask) << 28);
		} else {
			return getA().hashCode() + (getB().hashCode() << 16) + ((int)(flags & flagMask) << 18);
		}
	}
	
	@Override
	public int hashCode() {
		//return getId();
		return (int) (items + ((flags & flagMask) << 12));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ItemPair other = (ItemPair) obj;
		
		return this.items == other.items &&
		       (this.flags) == (other.flags);
	}

	public void setNumber(int number) {
		long properties = bucket.properties[propArray][propIndex];
		properties = (properties & 0xFFFFFFFF80000000L) | number;
		bucket.properties[propArray][propIndex] = properties;
	}

	public int getNumber() {
		long properties = bucket.properties[propArray][propIndex];
		return (int)properties & 0x7FFFFFFF;
	}

	public void setLowlink(int lowlink) {
		long properties = bucket.properties[propArray][propIndex];
		properties = (properties & 0xC00000007FFFFFFFL) | ((long)lowlink << 31);
		bucket.properties[propArray][propIndex] = properties;
	}

	public int getLowlink() {
		long properties = bucket.properties[propArray][propIndex];
		return (int) ((properties & 0x3FFFFFFF80000000L) >>> 31);
	}
	
	public void setAlive() {
		long properties = bucket.properties[propArray][propIndex];
		properties |=  0x4000000000000000L;
		bucket.properties[propArray][propIndex] = properties;
	}

	public boolean isAlive() {
		long properties = bucket.properties[propArray][propIndex];
		return (properties & 0x4000000000000000L) == 0x4000000000000000L;
	}

	public void setOnStack() {
		long properties = bucket.properties[propArray][propIndex];
		properties |=  0x8000000000000000L;
		bucket.properties[propArray][propIndex] = properties;
	}

	public void unsetOnStack() {
		long properties = bucket.properties[propArray][propIndex];
		properties &= 0x7FFFFFFFFFFFFFFFL;
		bucket.properties[propArray][propIndex] = properties;
	}

	public boolean isOnStack() {
		long properties = bucket.properties[propArray][propIndex];
		return (properties & 0x8000000000000000L) == 0x8000000000000000L;
	}

	public void init(int number) {
		long properties = bucket.properties[propArray][propIndex];
		// set nr
		// set lowlink = nr
		// set on stack
		properties = (properties & 0x4000000000000000L) | number | ((long)number << 31) | 0x8000000000000000L;
		bucket.properties[propArray][propIndex] = properties;
	}
}