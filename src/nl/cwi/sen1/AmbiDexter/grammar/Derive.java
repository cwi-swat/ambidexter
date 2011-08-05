/**
 * 
 */
package nl.cwi.sen1.AmbiDexter.grammar;


public class Derive extends Symbol {
	public Production production;
	
	public Derive(Production p) {
		super("d" + p.nr);
		production = p;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((production == null) ? 0 : production.hashCode());
		return result;
	}

	/*@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Derive other = (Derive) obj;
		if (production == null) {
			if (other.production != null)
				return false;
		} else if (!production.equals(other.production))
			return false;
		return true;
	}*/
}