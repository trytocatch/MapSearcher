package com.github.trytocatch.mapsearcher.magicqueue;

class ByteImpl extends MagicArrayQueue{
	static final int ONE_ELEMENTS = 0xFF-3;
	private byte[] elements;
	
	ByteImpl(int numElements) {
		super(ONE_ELEMENTS);
		elements = new byte[allocateElements(numElements)];
	}

	@Override
	int getArraySize() {
		return elements.length;
	}
	
	public boolean offer(int i) {
		if(i < 0)
			throw new IllegalArgumentException("Element to offer must be positive: " + i);
		if(i >= threshold){
			boolean flagWriten = false;
			for(int n = 3, b;n>=0;n--){
				b = (i >>>8*n) & 0xFF;
				if(flagWriten){
					offerByte(b);
				}else if(b != 0){
					offerByte(threshold + n);
					offerByte(b);
					flagWriten= true;
				}
			}
		}else{
			offerByte(i);
		}
		return true;
	}
	
	public int poll() {
		if (head == tail)
			return EMPTY_VALUE;
		int result = pollElement();
		if(result >= threshold){
			int byteCount = result - threshold + 1;
			result = 0;
			for(;byteCount>0;byteCount--){
				result = (result<<8) + pollElement();
			}
		}
		return result;
	}
	
	private void offerByte(int b){
		elements[tail] = (byte)b;
		if ((tail = (tail + 1) & (elements.length - 1)) == head)
			elements = doubleCapacity(elements);
	}

	@Override
	int getElement(int index) {
		return elements[index] & 0xFF;
	}

}