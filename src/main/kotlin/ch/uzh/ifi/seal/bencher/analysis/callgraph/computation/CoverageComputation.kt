package ch.uzh.ifi.seal.bencher.analysis.callgraph.computation

import ch.uzh.ifi.seal.bencher.Method

interface CoverageComputation {
    fun single(of: Method, unit: Method): CoverageUnitResult
    fun all(removeDuplicates: Boolean = false): Set<CoverageUnitResult>
}
