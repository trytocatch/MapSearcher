package com.github.trytocatch.mapsearcher.magicqueue;

class IntegerImpl extends MagicArrayQueue{
	private int[] elements;
	
	IntegerImpl(int numElements) {
		super(Integer.MAX_VALUE);
		elements = new int[allocateElements(numElements)];
	}
	
	@Override
	int getArraySize() {
		return elements.length;
	}
	
	public boolean offer(int i) {
		if(i < 0)
			throw new IllegalArgumentException("Element to offer must be positive: " + i);
		elements[tail] = i;
		if ((tail = (tail + 1) & (elements.length - 1)) == head)
			elements = doubleCapacity(elements);
		return true;
	}
	
	public int poll() {
		if (head == tail)
			return EMPTY_VALUE;
		return pollElement();
	}
	
	@Override
	int getElement(int index) {
		return elements[index];
	}
	
	@Override
	public void moveTail(int tailStart, int steps) {
		tail = tailStart + (steps>0?steps:0);
		tail &= elements.length - 1;
	}
}