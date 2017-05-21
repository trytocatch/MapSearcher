package com.github.trytocatch.mapsearcher;

public interface ForkResultHandler<R> {
	
	/**
	 * define how to create a new result holder from current result holder
	 * @param old
	 * @return
	 */
	public R fork(R old);
	
	/**
	 * define how merge two result holders to one holder
	 * @param result1
	 * @param result2
	 * @return
	 */
	public R merge(R result1, R result2);
	
	/**
	 * is this result holder has real result and need to merge? otherwise just drop it
	 * @param result
	 * @return
	 */
	public boolean hasResult(R result);
}

