package com.github.trytocatch.mapsearcher;

import java.util.concurrent.RecursiveAction;

import com.github.trytocatch.mapsearcher.Task.ReturnState;

class ForkTask<N, R> extends RecursiveAction {
	private static final long serialVersionUID = -4548185314537935131L;
	Searcher<N> searcher;
	SearchInfo<N, R> info;

	ForkTask(Searcher<N> searcher, SearchInfo<N, R> info) {
		this.searcher = searcher;
		this.info = info;
	}

	@Override
	protected void compute() {
		ReturnState rs = searcher.doSearch(info);
		if (!info.phaser.isTerminated()) {
			if (info.task.getForkResultHandler().hasResult(info.result))
				info.mergeResult();
			if (rs == ReturnState.BREAK) {
				info.phaser.forceTermination();
			} else {
				info.phaser.arriveAndDeregister();
			}
		}
	}
}