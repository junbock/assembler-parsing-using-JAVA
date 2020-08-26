import java.util.ArrayList;

public class Extref {
	String ref; //extref ¹®ÀÚ
	ArrayList<Integer> locationList; //addr
	ArrayList<Boolean> isSubList; //+ or -
	
	public Extref(String ref) {
		this.ref = ref;
		locationList = new ArrayList<Integer>();
		isSubList = new ArrayList<Boolean>();
	}
	
	public void add(int location, Boolean isSub) {
		locationList.add(location);
		isSubList.add(isSub);
	}
	
	public int length() {
		return locationList.size();
	}
	
	public String getRef( ) {
		return ref;
	}
	
	public int getLocation(int index) {
		return locationList.get(index);
	}
	
	public boolean isSub(int index) {
		return isSubList.get(index);
	}
}
