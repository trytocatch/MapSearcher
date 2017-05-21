package com.github.trytocatch.mapsearcher.magicqueue;

import java.lang.reflect.Array;
import java.util.Deque;

public abstract class MagicArrayQueue {
	
	public static final int EMPTY_VALUE = Integer.MIN_VALUE;

	/**
	 * The index of the element at the head of the deque (which is the element
	 * that would be removed by remove() or pop()); or an arbitrary number equal
	 * to tail if the deque is empty.
	 */
	protected int head;

	/**
	 * The index at which the next element would be added to the tail of the
	 * deque (via addLast(E), add(E), or push(E)).
	 */
	protected int tail;
	
	protected final int threshold;
	
	// ****** Array allocation and resizing utilities ******

	/**
	 * Allocates empty array to hold the given number of elements.
	 *
	 * @param numElements
	 *            the number of elements to hold
	 */
	protected final int allocateElements(int numElements) {
		int initialCapacity = 8;
		// Find the best power of two to hold elements.
		// Tests "<=" because arrays aren't kept full.
		if (numElements >= initialCapacity) {
			initialCapacity = numElements;
			initialCapacity |= (initialCapacity >>> 1);
			initialCapacity |= (initialCapacity >>> 2);
			initialCapacity |= (initialCapacity >>> 4);
			initialCapacity |= (initialCapacity >>> 8);
			initialCapacity |= (initialCapacity >>> 16);
			initialCapacity++;

			if (initialCapacity < 0) // Too many elements, must back off
				initialCapacity >>>= 1;// Good luck allocating 2 ^ 30 elements
		}
		return initialCapacity;
	}
	
	protected MagicArrayQueue(int threshold){
		this.threshold = threshold;
	}

	/**
	 * Doubles the capacity of this deque. Call only when full, i.e., when head
	 * and tail have wrapped around to become equal.
	 */
	protected final <A> A doubleCapacity(A array) {
		assert head == tail;
		int p = head;
		int n = Array.getLength(array);
		int r = n - p; // number of elements to the right of p
		int newCapacity = n << 1;
		if (newCapacity < 0)
			throw new IllegalStateException("Sorry, deque too big");
		@SuppressWarnings("unchecked")
		A newArray = (A)Array.newInstance(array.getClass().getComponentType(), newCapacity);
		System.arraycopy(array, p, newArray, 0, r);
		System.arraycopy(array, 0, newArray, r, p);
		head = 0;
		tail = n;
		return newArray;
	}


	// The main insertion and extraction methods are addFirst,
	// addLast, pollFirst, pollLast. The other methods are defined in
	// terms of these.

	/**
	 * Inserts the specified element at the end of this deque.
	 *
	 * @param e
	 *            the element to add
	 * @return {@code true} (as specified by {@link Deque#offerLast})
	 * @throws NullPointerException
	 *             if the specified element is null
	 */
	public abstract boolean offer(int i);
	
	public abstract int poll();
	
	abstract int getArraySize();
	
	abstract int getElement(int index);
	
	public int pollElement(){
		if (head == tail)
			throw new RuntimeException("queue is empty!");
		int result = getElement(head);
		head = (head + 1) & (getArraySize() - 1);
		return result;
	}
	
	/**
	 * must ensure that no doubleCapacity happened
	 * @param tailStart
	 * @param steps
	 */
	public void moveTail(int tailStart, int steps){
		tail = tailStart;
		for(int n=steps;n>0;n--){
			int b = getElement(tail);
			tail++;
			if(b >= threshold){
				tail+=b - threshold+1;
			}
			tail &= (getArraySize() - 1);
		}
	}
	
	public int getTail(){
		return tail;
	}
	
	public int size() {
		return (tail - head) & (getArraySize() - 1);
	}

	public boolean isEmpty() {
		return head == tail;
	}
	
	/**
	 * 
	 * @param numElements lower bound on initial capacity of the deque
	 * @param maxValue a reference value for choosing the optimum implementation
	 * @return
	 */
	public static MagicArrayQueue create(int numElements, int maxValue){
		if(maxValue < ByteImpl.ONE_ELEMENTS){
			return new ByteImpl(numElements);
		}else if(maxValue < ShortImpl.ONE_ELEMENTS){
			return new ShortImpl(numElements);
		}else{
			return new IntegerImpl(numElements);
		}
	}
}
