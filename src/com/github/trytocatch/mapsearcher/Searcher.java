package com.github.trytocatch.mapsearcher;

import com.github.trytocatch.mapsearcher.Task.ReturnState;

abstract class Searcher<N> {
	abstract <R> ReturnState doSearch(SearchInfo<N, R> info);
}