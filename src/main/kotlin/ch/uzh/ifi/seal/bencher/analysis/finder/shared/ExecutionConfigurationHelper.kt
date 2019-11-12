package ch.uzh.ifi.seal.bencher.analysis.finder.shared

import ch.uzh.ifi.seal.bencher.execution.ExecutionConfiguration
import org.funktionale.option.Option

object ExecutionConfigurationHelper {
    fun toExecutionConfiguration(forkVisitor: BenchForkAnnotation?,
                                 measurementVisitor: BenchIterationAnnotation?,
                                 warmupVisitor: BenchIterationAnnotation?,
                                 benchModeVisitor: BenchModeAnnotation?,
                                 outputTimeUnitAnnotationVisitor: BenchOutputTimeUnitAnnotation?): Option<ExecutionConfiguration> {

        val (f, wf) = Pair(forkVisitor?.forks() ?: -1, forkVisitor?.warmups() ?: -1)

        val (wi, wt, wtu) = Triple(warmupVisitor?.iterations() ?: -1, warmupVisitor?.time()
                ?: -1, warmupVisitor?.timeUnit() ?: Option.empty())

        val (mi, mt, mtu) = Triple(measurementVisitor?.iterations() ?: -1, measurementVisitor?.time()
                ?: -1, measurementVisitor?.timeUnit() ?: Option.empty())

        val bm = benchModeVisitor?.mode() ?: listOf()

        val otu = outputTimeUnitAnnotationVisitor?.timeUnit() ?: Option.empty()

        return Option.Some(ExecutionConfiguration(
                forks = f,
                warmupForks = wf,
                measurementIterations = mi,
                measurementTime = mt,
                measurementTimeUnit = mtu,
                warmupIterations = wi,
                warmupTime = wt,
                warmupTimeUnit = wtu,
                mode = bm,
                outputTimeUnit = otu
        ))
    }
}