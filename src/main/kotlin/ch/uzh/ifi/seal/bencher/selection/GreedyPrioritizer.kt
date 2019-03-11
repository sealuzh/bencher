package ch.uzh.ifi.seal.bencher.selection

import ch.uzh.ifi.seal.bencher.Benchmark
import ch.uzh.ifi.seal.bencher.Method
import ch.uzh.ifi.seal.bencher.PlainMethod
import ch.uzh.ifi.seal.bencher.PossibleMethod
import ch.uzh.ifi.seal.bencher.analysis.callgraph.CGResult
import ch.uzh.ifi.seal.bencher.analysis.callgraph.MethodCall
import ch.uzh.ifi.seal.bencher.analysis.weight.MethodWeights

abstract class GreedyPrioritizer(
        private val cgResult: CGResult,
        private val methodWeights: MethodWeights
): Prioritizer {

    protected fun benchValue(b: Benchmark, alreadySelected: Set<Method>): Pair<PrioritizedMethod<Benchmark>, Set<Method>> {
        val cs = cgResult.calls[b]
        val p = if (cs == null) {
            Pair(
                    PrioritizedMethod(
                            method = b,
                            priority = Priority(
                                    rank = -1,
                                    total = -1,
                                    value = 0.0
                            )
                    ),
                    alreadySelected
            )
        } else {
            val (v, s) = benchCallSum(cs, alreadySelected)
            Pair(
                    PrioritizedMethod(
                            method = b,
                            priority = Priority(
                                    rank = 0,
                                    total = 0,
                                    value = v
                            )
                    ),
                    alreadySelected + s
            )
        }

        return p
    }

    private fun benchCallSum(bcs: Iterable<MethodCall>, alreadySelected: Set<Method>): Pair<Double, Set<Method>> =
            benchCallSum(bcs.toList(), alreadySelected, 0.0)

    private tailrec fun benchCallSum(bcs: List<MethodCall>, alreadySelected: Set<Method>, currentSum: Double): Pair<Double, Set<Method>> =
            if (bcs.isEmpty()) {
                Pair(currentSum, alreadySelected)
            } else {
                val mc = bcs[0]
                val m = mc.method
                if (!alreadySelected.contains(m)) {
                    val pm = PlainMethod(
                            clazz = m.clazz,
                            name = m.name,
                            params = m.params
                    )
                    val weight: Double = methodWeights[pm] ?: 0.0
                    val adaptedWeight: Double = if (m is PossibleMethod) {
                        // if m is PossibleMethod divide weight by the number of possible targets
                        // reason: the call is not certain because of multiple targets due to static points-to analysis
                        weight / m.nrPossibleTargets
                    } else {
                        weight
                    }

                    benchCallSum(bcs.drop(1), alreadySelected + m, currentSum + adaptedWeight)
                } else {
                    // already added prio of this method (method can be contained multiple times because of multiple levels in CG)
                    benchCallSum(bcs.drop(1), alreadySelected, currentSum)
                }
            }
}
