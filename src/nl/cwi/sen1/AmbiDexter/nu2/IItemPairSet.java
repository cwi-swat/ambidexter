package nl.cwi.sen1.AmbiDexter.nu2;


public interface IItemPairSet extends Iterable<ItemPair> {

	ItemPair getContained(long items, long flags);
	ItemPair unsafeAdd(long items, long flags);
	
	int size();
	String usageStatistics();
	long getCompressedSize();
}
