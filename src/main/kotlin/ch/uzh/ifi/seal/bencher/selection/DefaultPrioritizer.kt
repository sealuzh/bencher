package ch.uzh.ifi.seal.bencher.selection

import ch.uzh.ifi.seal.bencher.Benchmark
import ch.uzh.ifi.seal.bencher.analysis.finder.JarBenchFinder
import org.funktionale.either.Either
import java.nio.file.Path

class DefaultPrioritizer(private val jar: Path) : Prioritizer {
    override fun prioritize(benchs: Iterable<Benchmark>): Either<String, List<PrioritizedMethod<Benchmark>>> {
        val bf = JarBenchFinder(jar = jar)
        val ebs = bf.all()
        if (ebs.isLeft()) {
            return Either.left(ebs.left().get())
        }
        val bs = ebs.right().get()

        // check if returned from JarBenchFinder WITHOUT function parameters,
        // as JMH enforces that fully-qualified benchmarks are unique,
        // hence no overloading is permitted
        // AND
        // results from JarBenchFinder never contain function parameters
        val filtered = bs.mapNotNull { b ->
            benchs.find { it.clazz == b.clazz && it.name == b.name && it.jmhParams == b.jmhParams }
        }

        return Either.right(
            filtered.mapIndexed { i, b ->
                PrioritizedMethod(
                    method = b,
                    priority = Priority(
                            rank = i+1,
                            total = filtered.size,
                            value = (filtered.size - i).toDouble()
                    )
                )
            }
        )
    }

    private fun hashSetWithoutParams(i: Iterable<Benchmark>): HashSet<Benchmark> =
            i.map { it.copy(params = listOf()) }.toHashSet()
}