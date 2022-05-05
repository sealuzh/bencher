package ch.uzh.ifi.seal.bencher.analysis.callgraph.sta

import ch.uzh.ifi.seal.bencher.Benchmark
import ch.uzh.ifi.seal.bencher.PlainMethod
import ch.uzh.ifi.seal.bencher.analysis.JarTestHelper
import ch.uzh.ifi.seal.bencher.analysis.SourceCodeConstants
import ch.uzh.ifi.seal.bencher.analysis.callgraph.Coverages
import ch.uzh.ifi.seal.bencher.analysis.finder.asm.AsmBenchFinder
import ch.uzh.ifi.seal.bencher.fileResource
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WalaSCGExtendedTest : WalaSCGTest() {

    override val cgr: Coverages
        get() = WalaSCGExtendedTest.cg

    override val multiCGEntrypoints = false

    @Test
    fun nonLibCallsBench1() {
//        sout(bench1, 2, true, 0.33)
        sout(bench1, 2)
    }

    @Test
    fun nonLibCallsBench2() {
        sout(bench2, 2)
    }

    @Test
    fun nonLibCallsBench3() {
        sout(bench3, 2)
    }

    fun sout(bench: Benchmark, level: Int, possibly: Boolean = false, probability: Double = 1.0) {
        val sbNew = PlainMethod(clazz = "java.lang.StringBuilder", name = "<init>", params = listOf(), returnType = SourceCodeConstants.void)
        h.reachable(cg, bench, sbNew, level, possibly, probability)
        val sbAppend = PlainMethod(clazz = "java.lang.StringBuilder", name = "append", params = listOf("java.lang.String"), returnType = SourceCodeConstants.void)
        h.reachable(cg, bench, sbAppend, level, possibly, probability)
        val sbToString = PlainMethod(clazz = "java.lang.StringBuilder", name = "toString", params = listOf(), returnType = SourceCodeConstants.void)
        h.reachable(cg, bench, sbToString, level, possibly, probability)
        val funPrintln = PlainMethod(clazz = "java.io.PrintStream", name = "println", params = listOf("java.lang.String"), returnType = SourceCodeConstants.void)
        h.reachable(cg, bench, funPrintln, level, possibly, probability)
    }

    companion object {
        lateinit var cg: Coverages

        @JvmStatic
        @BeforeAll
        fun setup() {
            val jar = JarTestHelper.jar4BenchsJmh121.fileResource()

            cg = h.assertCGResult(
                    WalaSCG(
                            entrypoints = CGEntrypoints(
                                    mf = AsmBenchFinder(jar),
                                    me = BenchmarkWithSetupTearDownEntrypoints(),
                                    ea = SingleCGEntrypoints()
                            ),
                            algo = WalaRTA()
                    ),
                    jar = jar
            )
        }
    }
}
