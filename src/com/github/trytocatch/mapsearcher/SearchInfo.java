package com.github.trytocatch.mapsearcher;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Phaser;

class SearchInfo<N, R> implements Cloneable {
		ArrayList<N> steps;
		R result;
		int depth;
		int weight;
		Task<N, R> task;
		int startIndex;
		BitSet nodeBitSet;
		List<N> unmodifiableSteps;
		ArrayList<Integer> stepIndexes;
		ConcurrentLinkedQueue<R> resultQueue;
		Phaser phaser;

		SearchInfo(ArrayList<N> steps, R result, int depth, int weight, Task<N, R> task, int startIndex,
				BitSet nodeBitSet, ArrayList<Integer> stepIndexes, ConcurrentLinkedQueue<R> resultQueue, Phaser phaser) {
			this.steps = steps;
			this.result = result;
			this.depth = depth;
			this.weight = weight;
			this.startIndex = startIndex;
			this.nodeBitSet = nodeBitSet;
			this.stepIndexes = stepIndexes;
			this.task = task;
			this.resultQueue = resultQueue;
			this.phaser = phaser;
			unmodifiableSteps = Collections.unmodifiableList(steps);
		}

		@SuppressWarnings("unchecked")
		SearchInfo<N, R> fork(int startIndex, int curWeight) {
			SearchInfo<N, R> newObj;
			try {
				newObj = (SearchInfo<N, R>) super.clone();
				newObj.startIndex = startIndex;
				newObj.steps = (ArrayList<N>) steps.clone();
				newObj.unmodifiableSteps = Collections.unmodifiableList(newObj.steps);
				if (nodeBitSet != null)
					newObj.nodeBitSet = (BitSet) nodeBitSet.clone();
				if (stepIndexes != null)
					newObj.stepIndexes = (ArrayList<Integer>) stepIndexes.clone();
				newObj.depth++;
				newObj.weight += curWeight;
				newObj.result = task.getForkResultHandler().fork(result);
				return newObj;
			} catch (CloneNotSupportedException e) {
				return null;// won't happen
			}
		}

		void mergeResult() {
//			for (R r, newResult = result;;) {
//				r = joinResult.get();
//				if (r == null) {
//					if (joinResult.compareAndSet(null, newResult))
//						break;
//				} else if (joinResult.compareAndSet(r, null)) {
//					newResult = task.getForkResultHandler().merge(r, newResult);
//				}
//			}
			resultQueue.add(result);
		}
	}