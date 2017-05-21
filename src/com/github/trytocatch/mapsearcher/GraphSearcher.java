package com.github.trytocatch.mapsearcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Phaser;

import com.github.trytocatch.mapsearcher.Task.ReturnState;
import com.github.trytocatch.mapsearcher.magicqueue.MagicArrayQueue;

/**
 * @author trytocatch@163.com
 */
public class GraphSearcher<N> {
	private final int MAX_NODE_COUNT = 0x40000 + 1;

	/**
	 * if (unused << TRIM_THRESHOLD)>=array size, then do trim
	 */
	private final int TRIM_THRESHOLD = 2;
	/**
	 * ensure that: weight>WEIGHT_THRESHOLD
	 */
	private final int WEIGHT_THRESHOLD = Integer.MIN_VALUE + (MAX_NODE_COUNT - 1);

	// do some mapping work to improve performance
	private HashMap<N, Integer> mapper;// node to index
	private N[] unmapper;// index to node
	private int nodeCount;
	/**
	 * weight data, it is a (nodeCount+1) * (nodeCount+1) double dimensional array<br>
	 * data[nodeCount] is dummy info for all start nodes<br>
	 * data[n][nodeCount] is trim info for data[n](0~nodeCount-1)<br>
	 */
	private int[][] data;
	
	private int rootMark;

	private final Searcher<N> breadthFirstSearcher;

	private final Searcher<N> depthFirstSearcher;

	public GraphSearcher(N[] from, N[] to, int[] weight) {
		initData(from, to, weight);
		breadthFirstSearcher = new BreadthFirstSearcher();
		depthFirstSearcher = new DepthFirstSearcher();
	}

	protected void initData(N[] from, N[] to, int[] weight) {
		if (from == null || to == null || weight == null)
			throw new IllegalArgumentException("None of the arguments 'from' 'to' 'wight' can be null");
		if (from.length != to.length || to.length != weight.length)
			throw new IllegalArgumentException("The arguments 'from' 'to' 'wight' have different length!");

		mapNode2Index(from, to);
		
		rebuildMapData(from, to, weight);
		
		markForFastIterationAndDoArrayTrim();
	}
	
	@SuppressWarnings("unchecked")
	private void mapNode2Index(N[] from, N[] to) {
		mapper = new HashMap<N, Integer>();
		List<N> list = new LinkedList<N>();
		for (Object nodes : new Object[] { from, to }) {
			for (N node : (N[]) nodes) {
				if (mapper.putIfAbsent(node, list.size()) == null){
					if (list.size() == MAX_NODE_COUNT)
						throw new IllegalArgumentException(
								"There are too many nodes, more than " + MAX_NODE_COUNT);
					list.add(node);
				}
			}
		}
		unmapper = (N[]) list.toArray();
		nodeCount = unmapper.length;
	}

	private void rebuildMapData(N[] from, N[] to, int[] weight) {
		data = new int[nodeCount + 1][];
		rootMark = data.length+1;
		// for all start nodes, dummy[nodeCount]: trim info
		int[] dummy = new int[nodeCount + 1];
		Arrays.fill(dummy, WEIGHT_THRESHOLD);
		for (int n = 0; n < from.length; n++) {
			if (weight[n] <= WEIGHT_THRESHOLD)
				throw new IllegalArgumentException("Weight can't be less than or equal " + WEIGHT_THRESHOLD);
			int indexFrom = mapper.get(from[n]);
			int indexTo = mapper.get(to[n]);
			int[] distanceInfo = data[indexFrom];
			if (distanceInfo == null) {
				// distanceInfo[nodeCount]: trim info
				distanceInfo = new int[nodeCount + 1];
				Arrays.fill(distanceInfo, WEIGHT_THRESHOLD);
				data[indexFrom] = distanceInfo;
			}
			dummy[indexFrom] = 0;
			distanceInfo[indexTo] = weight[n];
		}
		data[data.length - 1] = dummy;
	}
	
	private void markForFastIterationAndDoArrayTrim() {
		int[] info;
		for (int m = 0; m < data.length; m++) {
			info = data[m];
			if (info != null) {
				int firstIndex = 0;
				int pos = 0;
				for (int n = 0; n < info.length - 1; n++) {
					if (info[n] != WEIGHT_THRESHOLD) {
						// store the next index for faster iteration
						if (pos < n) {
							info[pos] = WEIGHT_THRESHOLD - n;
							if (pos == 0)
								firstIndex = n;
						}
						pos = n + 1;
					}
				}
				// try to trim the array
				int unused = firstIndex + (nodeCount - pos);
				if ((unused << TRIM_THRESHOLD) >= nodeCount) {
					int[] trimmed = new int[nodeCount - unused + 1];
					System.arraycopy(info, firstIndex, trimmed, 0, nodeCount - unused);
					trimmed[trimmed.length - 1] = firstIndex;
					data[m] = trimmed;
				} else {
					info[info.length - 1] = 0;
				}
			}
		}
	}

	private <T> void addOrSet(List<T> list, int index, T item) {
		if (list.size() <= index)// list.size() < index won't happen
			list.add(item);
		else {
			list.set(index, item);
		}
	}
	
	public int getWeight(List<N> steps){
		int weight = 0;
		if(!steps.isEmpty()){
			int index = mapper.get(steps.get(0));
			int d[];
			for(int n = 1;n<steps.size();n++){
				d = data[index];
				index = mapper.get(steps.get(n));
				weight+=d[index - d[d.length-1]];
			}
		}
		return weight;
	}

	public <R> R search(Task<N, R> task) {
		BitSet nodeBitSet = task.isStopFurtherSearchOnRepeat() ? new BitSet(nodeCount) : null;
		N start = task.getStart();
		SearchInfo<N, R> info = new SearchInfo<N, R>(new ArrayList<N>(0x2000), task.createResultHolder(), -1, 0, task,
				nodeCount, nodeBitSet, null, new ConcurrentLinkedQueue<R>(), new Phaser(1));
		Searcher<N> searcher;
		if (task.isDepthFirst()) {
			searcher = depthFirstSearcher;
		} else {
			info.stepIndexes = new ArrayList<Integer>(0x2000);
			searcher = breadthFirstSearcher;
		}
		if (start == null) {
			searcher.doSearch(info);
		} else {
			Integer index = mapper.get(start);
			if (index == null)
				throw new IllegalArgumentException("Illegal node name: " + start);
			info.steps.add(start);
			ReturnState rs = task.check(info.unmodifiableSteps, 0, 0, info.result, false);
			if (rs == ReturnState.CONTINUE || rs == ReturnState.FORK_CONTINUE) {
				info.depth = 0;
				info.startIndex = index;
				if (!task.isDepthFirst()) {
					info.stepIndexes.add(index);
				}
				searcher.doSearch(info);
			}
		}
		info.phaser.arriveAndAwaitAdvance();
		if (!info.resultQueue.isEmpty()) {
			ForkResultHandler<R> handler = task.getForkResultHandler();
			for (R result : info.resultQueue) {
				info.result = handler.merge(result, info.result);
			}
		}
		return info.result;
	}

	class DepthFirstSearcher extends Searcher<N> {
		@Override
		<R> ReturnState doSearch(SearchInfo<N, R> info) {
			boolean toClearBitSet = false;
			if (info.phaser.isTerminated())
				return ReturnState.BREAK;
			try {
				int[] lData;
				if (info.startIndex == nodeCount) {
					lData = data[data.length - 1];
				} else {
					if (info.startIndex < 0 || info.startIndex >= data.length - 1)
						return ReturnState.STOP;
					lData = data[info.startIndex];
					if (info.nodeBitSet != null) {
						info.nodeBitSet.set(info.startIndex);
						toClearBitSet = true;
					}
				}
				Boolean isRepeated = null;
				int trimmedHead = lData[lData.length - 1];
				for (int n = 0; n < lData.length - 1;) {
					if (lData[n] == WEIGHT_THRESHOLD)
						break;
					if (lData[n] > WEIGHT_THRESHOLD) {
						addOrSet(info.steps, info.depth + 1, unmapper[n + trimmedHead]);
						if (info.nodeBitSet != null)
							isRepeated = info.nodeBitSet.get(n + trimmedHead);
						ReturnState code = info.task.check(info.unmodifiableSteps, info.depth + 1,
								info.weight + lData[n], info.result, isRepeated);
						if (code == ReturnState.BREAK)
							return ReturnState.BREAK;
						// stop on repeat
						if ((code == ReturnState.FORK_CONTINUE || code == ReturnState.CONTINUE)
								&& (isRepeated == null || !isRepeated)) {
							if (code == ReturnState.FORK_CONTINUE && info.task.getMaxParallelTask() > 1
									&& info.phaser.getRegisteredParties() < info.task.getMaxParallelTask()) {
								info.phaser.register();
								new ForkTask<N, R>(this, info.fork(n + trimmedHead, lData[n])).fork();
							} else {
								int orgStartIndex = info.startIndex;
								info.startIndex = n + trimmedHead;
								info.depth++;
								info.weight += lData[n];
								ReturnState rs = doSearch(info);
								info.startIndex = orgStartIndex;
								info.depth--;
								info.weight -= lData[n];
								if (rs == ReturnState.BREAK) {
									return ReturnState.BREAK;
								}
							}
						}
						n++;
					} else {
						n = WEIGHT_THRESHOLD - lData[n] - trimmedHead;
					}
				}
				return ReturnState.STOP;
			} finally {
				while (info.steps.size() > info.depth && !info.steps.isEmpty())
					info.steps.remove(info.steps.size() - 1);
				if (toClearBitSet) {
					info.nodeBitSet.clear(info.startIndex);
				}
			}
		}
	}

	class BreadthFirstSearcher extends Searcher<N> {
		@Override
		<R> ReturnState doSearch(SearchInfo<N, R> info) {
			MagicArrayQueue deque = MagicArrayQueue.create(64, nodeCount+info.depth+64);
//			deque.offer((info.depth>0?info.depth:0)+rootMark+1);
			deque.offer(info.startIndex);
			deque.offer(rootMark);
			int firstBranch = 0;
			int firstBranchTemp = info.depth;
			int i;
			int tailMark = -1;
			int markDepth = 0;
			while ((i = deque.poll()) != MagicArrayQueue.EMPTY_VALUE) {
				if(i == rootMark){
					if(tailMark != -1){
						deque.moveTail(tailMark, 0);
						tailMark = -1;
					}
					if(deque.isEmpty())
						return ReturnState.STOP;
					info.depth++;
					deque.offer(rootMark);
					firstBranch = firstBranchTemp;
					firstBranchTemp = info.depth;
					if (info.depth >= 2) {
						int ppIndex = info.stepIndexes.get(info.depth - 2);
						info.weight += data[ppIndex][info.stepIndexes.get(info.depth - 1)
								- data[ppIndex][data[ppIndex].length - 1]];
					}
					continue;
				}else if(i>rootMark){
					int startDepth = i-(rootMark+1);
					for(;startDepth < firstBranch;startDepth++){
						deque.poll();
					}
					if(tailMark != -1){
						deque.moveTail(tailMark, startDepth - markDepth + 1);
						if(startDepth<markDepth){
							deque.offer(startDepth + rootMark+1);
							markDepth = startDepth;
						}
					}else{
						tailMark = deque.getTail();
						markDepth = startDepth;
						deque.offer(startDepth + rootMark+1);
					}
					int pIndex = startDepth>=1?info.stepIndexes.get(startDepth - 1):-1;
					for(int n=startDepth, index;n<info.depth;n++){
						index = deque.poll();
						if (pIndex >= 0) {
							info.weight = info.weight
									- data[pIndex][info.stepIndexes.get(n) - data[pIndex][data[pIndex].length - 1]]
									+ data[info.stepIndexes.get(n - 1)][index
											- data[info.stepIndexes.get(n - 1)][data[info.stepIndexes.get(n - 1)].length
													- 1]];
						}
						pIndex = info.stepIndexes.get(n);
						if (pIndex != index) {
							info.stepIndexes.set(n, index);
							if (n < firstBranchTemp)
								firstBranchTemp = n;
						}

						if (info.nodeBitSet != null)
							info.nodeBitSet.clear(pIndex);
						info.steps.set(n, unmapper[index]);
						deque.offer(index);
					}
					
					if (info.nodeBitSet != null){
						for (int l = startDepth; l < info.depth; l++) {
							info.nodeBitSet.set(info.stepIndexes.get(l));
						}
					}
					i = deque.poll();
				}
				if (info.phaser.isTerminated())
					return ReturnState.BREAK;
				boolean toClearBitSet = false;
				int[] lData;
				int curWeight = 0;
				boolean addParent;
				if (i == nodeCount) {
					addParent = false;
					lData = data[i];
				} else {
					addParent = true;
					lData = data[i];
					info.steps.set(info.depth, unmapper[i]);
					info.stepIndexes.set(info.depth, i);
					if (info.depth > 0) {
						int preIndex = info.stepIndexes.get(info.depth - 1);
						curWeight = data[preIndex][i - data[preIndex][data[preIndex].length - 1]];
					}
					if (info.nodeBitSet != null) {
						info.nodeBitSet.set(i);
						toClearBitSet = true;
					}
				}
				Boolean isRepeated = null;
				int trimmedHead = lData[lData.length - 1];
				for (int n = 0; n < lData.length - 1;) {
					if (lData[n] == WEIGHT_THRESHOLD)
						break;
					if (lData[n] > WEIGHT_THRESHOLD) {
						addOrSet(info.steps, info.depth + 1, unmapper[n + trimmedHead]);
						if (info.stepIndexes.size() <= info.depth + 1)
							info.stepIndexes.add(n + trimmedHead);
						else
							info.stepIndexes.set(info.depth + 1, n + trimmedHead);
						if (info.nodeBitSet != null)
							isRepeated = info.nodeBitSet.get(n + trimmedHead);
						ReturnState code = info.task.check(info.unmodifiableSteps, info.depth + 1,
								info.weight + curWeight + lData[n], info.result, isRepeated);
						if (code == ReturnState.BREAK)
							return ReturnState.BREAK;
						// stop on repeat
						if ((code == ReturnState.FORK_CONTINUE || code == ReturnState.CONTINUE)
								&& (isRepeated == null || !isRepeated)) {
							if (code == ReturnState.FORK_CONTINUE && info.task.getMaxParallelTask() > 1
									&& info.phaser.getRegisteredParties() < info.task.getMaxParallelTask()) {
								info.phaser.register();
								new ForkTask<N, R>(this, info.fork(n + trimmedHead, curWeight)).fork();
							} else {
								if(addParent){
									if(tailMark == -1)
										deque.offer(info.depth + rootMark + 1);
									else
										tailMark = -1;
									deque.offer(i);
									addParent = false;
								}
								deque.offer(n + trimmedHead);
							}
						}
						n++;
					} else {
						n = WEIGHT_THRESHOLD - lData[n] - trimmedHead;
					}
				}
				if (toClearBitSet)
					info.nodeBitSet.clear(i);
			}
			return ReturnState.STOP;
		}
	}
}
