package ch.uzh.ifi.seal.bencher.prioritization.search

import arrow.core.Either
import arrow.core.getOrElse
import ch.uzh.ifi.seal.bencher.Benchmark
import ch.uzh.ifi.seal.bencher.Version
import ch.uzh.ifi.seal.bencher.analysis.callgraph.CGOverlap
import ch.uzh.ifi.seal.bencher.analysis.callgraph.CGOverlapImpl
import ch.uzh.ifi.seal.bencher.analysis.callgraph.CGResult
import ch.uzh.ifi.seal.bencher.analysis.weight.MethodWeights
import ch.uzh.ifi.seal.bencher.measurement.PerformanceChange
import ch.uzh.ifi.seal.bencher.measurement.PerformanceChangeType
import ch.uzh.ifi.seal.bencher.measurement.PerformanceChanges
import ch.uzh.ifi.seal.bencher.measurement.PerformanceChangesImpl
import ch.uzh.ifi.seal.bencher.prioritization.PrioritizedMethod
import ch.uzh.ifi.seal.bencher.prioritization.PrioritizerMultipleSolutions
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder
import org.uma.jmetal.operator.crossover.impl.PMXCrossover
import org.uma.jmetal.operator.mutation.impl.PermutationSwapMutation
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection
import org.uma.jmetal.solution.permutationsolution.PermutationSolution
import org.uma.jmetal.util.fileoutput.SolutionListOutput
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext
import kotlin.random.Random

class JMetalPrioritizer(
    private val cgResult: CGResult,
    private val methodWeights: MethodWeights,
    performanceChanges: PerformanceChanges?,
    private val v1: Version,
    private val v2: Version,
    override val random: Random = Random(System.nanoTime()),
) : PrioritizerMultipleSolutions {

    private val performanceChanges: PerformanceChanges
    private val overlap: CGOverlap

    // Algorithm parameters
    private val crossoverProbability: Double = 0.9
    private val populationSize: Int = 250
    private val maxEvaluations: Int = 25000

    init {
        val filteredPerformanceChanges = (performanceChanges ?: noPerformanceChanges())
            .changesUntilVersion(v = v1, untilVersion1 = true, including = true)
            .getOrElse {
                throw IllegalArgumentException("could not filter performance changes until version")
            }

        this.performanceChanges = PerformanceChangesImpl(filteredPerformanceChanges)

        this.overlap = CGOverlapImpl(cgResult.calls.map { it.value })
    }

    override fun prioritizeMultipleSolutions(benchs: Iterable<Benchmark>): Either<String, List<List<PrioritizedMethod<Benchmark>>>> {
        val cov = prepareCoverage(benchs)

        val benchmarks = cov.calls.keys.map { m ->
            m as? Benchmark ?: return Either.Left("method not a benchmark: $m")
        }

        val bim = BenchmarkIndexMapImpl(benchmarks)

        val mutationProbability = 1.0 / cov.calls.size

        val problem = PrioritizationProblem(
            cgResult = cov,
            methodWeights = methodWeights,
            cgOverlap = overlap,
            performanceChanges = performanceChanges,
            benchmarkIndexMap = bim
        )

        val algorithm = NSGAIIBuilder<PermutationSolution<Int>>(
            problem,
            PMXCrossover(crossoverProbability),
            PermutationSwapMutation(mutationProbability),
            populationSize,
        )
            .setMaxEvaluations(maxEvaluations)
            .setSelectionOperator(BinaryTournamentSelection())
            .build()

        algorithm.run()

        val solutionList = algorithm.result

        SolutionListOutput(solutionList)
            .setFunFileOutputContext(DefaultFileOutputContext("FUN.csv"))
            .setVarFileOutputContext(DefaultFileOutputContext("VAR.csv"))
            .print()

        return Either.Left("not implemented")
    }

    private fun prepareCoverage(benchs: Iterable<Benchmark>): CGResult =
        CGResult(
            calls = benchs
                .asSequence()
                .filter{ cgResult.calls[it] != null }
                .associateWith { cgResult.calls[it]!! }
        )

    private fun noPerformanceChanges(): PerformanceChanges =
        PerformanceChangesImpl(
            changes = cgResult.calls
                .asSequence()
                // assume that there are only Benchmark objects in there, otherwise a runtime exception is acceptable
                .map { (m, _) -> m as Benchmark }
                .map { noPerformanceChange(it)}
                .toList()
        )

    private fun noPerformanceChange(b: Benchmark): PerformanceChange =
        PerformanceChange(
            benchmark = b,
            v1 = v1,
            v2 = v2,
            type = PerformanceChangeType.NO,
            min = 0,
            max = 0,
        )
}
