package nl.cwi.sen1.AmbiDexter.grammar;

public class Character extends Symbol {
	
	public static int maxCharacterUsed = 0;
	public static int maxCharacterInOriginalGrammar = 0;

	public Character(int c) {
		super("" + c, -c);
	}
	
	@Override
	public boolean canShiftWith(Symbol s) {
		return s.canShiftWith(this);
	}
	
	@Override
	public String prettyPrint() {
		Character c = this;
		String s = null;
		switch(c.id) {
		case -0:
			s = "\\0";
		break;
		case -9:
			s = "\\t";
		break;
		case -10:
			s = "\\n";
		case -13:
			s = "\\r";
		break;
		case -256:
			s = "eof";
		break;
		default:
			s = "" + (char)-c.id;
		}
		return "'" + s + "'";
	}
	
	public String toAscii() {
		return java.lang.Character.toString((char) -id);
	}
	
	public boolean reconstructed() {
		return -id > maxCharacterInOriginalGrammar;
	}
}
