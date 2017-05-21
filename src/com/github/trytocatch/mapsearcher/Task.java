package com.github.trytocatch.mapsearcher;

import java.util.List;

public abstract class Task<N, R> {
		protected enum ReturnState {
			/**
			 * means continue further search on this path
			 */
			CONTINUE,
			/**
			 * means fork(no guarantee) and continue further search on this path<br>
			 * you should call {@link #setMaxParallelTask} and override {@link #getForkResultHandler}
			 */
			FORK_CONTINUE,
			/**
			 * means stop further search on this path
			 */
			STOP,
			/**
			 * means all search works have done
			 */
			BREAK
		}

		private N start;

		private boolean isDepthFirst;

		private boolean stopFurtherSearchOnRepeat;
		
		private int maxParallelTask = 1000;

		/**
		 * 
		 * @param start
		 *            start node or null
		 */
		public Task(N start) {
			this.start = start;
		}

		public N getStart() {
			return start;
		}

		/**
		 * define that how this task works
		 * 
		 * @param steps
		 *            current steps, DO NOT modify this
		 * @param depth
		 *            start from 0
		 * @param weight
		 *            current weight in total
		 * @param resultHolder
		 *            the result holder, you can use this to maintain
		 *            result/results, add or query or remove
		 * @param isRepeated
		 *            if isStopFurtherSearchOnRepeat() is false, it will always
		 *            be null, otherwise, it won't be null and indicate that
		 *            whether repeat appears
		 * @return CONTINUE means continue further search on this path, STOP
		 *         means stop further search on this path BREAK means all search
		 *         works have done,
		 */
		protected abstract ReturnState check(List<N> steps, int depth, int weight, R resultHolder, Boolean isRepeated);

		public abstract R createResultHolder();

		public boolean isDepthFirst() {
			return isDepthFirst;
		}

		public void setDepthFirst(boolean isDepthFirst) {
			this.isDepthFirst = isDepthFirst;
		}

		public boolean isStopFurtherSearchOnRepeat() {
			return stopFurtherSearchOnRepeat;
		}

		/**
		 * turn on this will make the argument 'Boolean isRepeated' of method
		 * check meaningful, it won't be null and it will stop further search on
		 * current path while repeat appears
		 * 
		 * @param stopFurtherSearchOnRepeat
		 */
		public void setStopFurtherSearchOnRepeat(boolean stopFurtherSearchOnRepeat) {
			this.stopFurtherSearchOnRepeat = stopFurtherSearchOnRepeat;
		}

		public int getMaxParallelTask() {
			return maxParallelTask;
		}

		public void setMaxParallelTask(int maxParallelTask) {
			this.maxParallelTask = maxParallelTask;
		}
		
		public ForkResultHandler<R> getForkResultHandler(){
			throw new UnsupportedOperationException();
		}
	}