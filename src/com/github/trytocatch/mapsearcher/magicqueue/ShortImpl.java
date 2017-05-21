package com.github.trytocatch.mapsearcher.magicqueue;

class ShortImpl extends MagicArrayQueue{
	static final int ONE_ELEMENTS = 0xFFFF-1;
	private short[] elements;
	
	ShortImpl(int numElements) {
		super(ONE_ELEMENTS);
		elements = new short[allocateElements(numElements)];
	}
	
	@Override
	int getArraySize() {
		return elements.length;
	}
	
	public boolean offer(int i) {
		if(i < 0)
			throw new IllegalArgumentException("Element to offer must be positive: " + i);
		if(i >= threshold){
			int s = i>>>16;
			if(s == 0){
				offerShort(threshold);
				offerShort(i);
			}else{
				offerShort(threshold+1);
				offerShort(s);
				offerShort(i);
			}
		}else{
			offerShort(i);
		}
		return true;
	}
	
	public int poll() {
		if (head == tail)
			return EMPTY_VALUE;
		int result = pollElement();
		if(result >= threshold){
			result = pollElement();
			if(result == threshold+1)
				result = (result<<16) + pollElement();
		}
		return result;
	}
	
	private void offerShort(int b){
		elements[tail] = (short)b;
		if ((tail = (tail + 1) & (elements.length - 1)) == head)
			elements = doubleCapacity(elements);
	}

	@Override
	int getElement(int index) {
		return elements[index] & 0xFFFF;
	}
}