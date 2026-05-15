package com.image;

public class MapKey {
	private int type;
	private long index;
	
	public MapKey(int type, long index){
		this.type=type;
		this.index=index;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public long getIndex() {
		return index;
	}
	public void setIndex(long index) {
		this.index = index;
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj instanceof MapKey){
			MapKey key=(MapKey) obj;
			if(key.getIndex()==getIndex()&&key.getType()==getType()) return true;
		}
		
		return false;
	}
	
	@Override
	public String toString(){
		System.out.println(1);
		return null;
		
	}
	
	@Override
	public int hashCode(){
//		System.out.println(index*1000+type);
		return new Long(index*1000+type).hashCode();
	}
}
