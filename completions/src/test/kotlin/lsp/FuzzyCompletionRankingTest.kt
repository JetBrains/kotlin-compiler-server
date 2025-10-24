package lsp

import completions.lsp.util.completions.FuzzyCompletionRanker
import org.eclipse.lsp4j.CompletionItem
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [completions.CompletionsApplication::class]
)
class FuzzyCompletionRankingTest {

    @Autowired
    private lateinit var completionRanker: FuzzyCompletionRanker

    @Test
    fun `empty query ranks only by sortText`() = rankingTest("") {
        val a = item("zeta", sortText = "3")
        val b = item("alpha", sortText = "1")
        val c = item("gamma", sortText = "2")
        expectOrder(b, c, a)
    }

    @Test
    fun `Kotlin common API names should be properly ranked`() = rankingTest("pr") {
        val println = item("println", sortText = "2")
        val print = item("print", sortText = "1")
        val property = item("property", sortText = "4")
        val map = item("map", sortText = "3")
        expectOrder(print, println, property, map)
    }

    @Test
    fun `completions are ranked by relevance and length (shortest first)`() = rankingTest("toIn") {
        val c1 = item("toInt", sortText = "3")
        val c2 = item("toUInt", sortText = "1")
        val c3 = item("toInterval", sortText = "2")
        expectOrder(c1, c3, c2)
    }

    @Test
    fun `tie break by sortText when scores equal`() = rankingTest("pr") {
        val a = item("prX", sortText = "1")
        val b = item("prY", sortText = "2")
        expectOrder(a, b)
    }

    @Test
    fun `exact and consecutive matches outrank sparse matches`() = rankingTest("pr") {
        val a = item("print", sortText = "2")     // exact
        val b = item("p...r..", sortText = "1")   // non-consecutive
        val c = item("pxxx", sortText = "3")      // sparse/non-full
        expectOrder(a, b, c)
    }

    @Test
    fun `case insensitive matching, tie-break with sortText`() = rankingTest("PrI") {
        val a = item("pRiNtLn", sortText = "2")
        val b = item("println", sortText = "1")
        expectOrder(b, a)
    }

    @Test
    fun `consecutive matches beat non-consecutive`() = rankingTest("io") {
        val consecutive = item("ioScope", sortText = "2")
        val sparse = item("iXoScope", sortText = "1")
        expectOrder(consecutive, sparse)
    }

    private fun rankingTest(query: String, build: RankingTestDSL.RankingCase.() -> Unit) {
        val case = RankingTestDSL.RankingCase(query).apply(build)
        val ranked = with(completionRanker) { case.items.rankCompletions(query) }
        assertIterableEquals(case.expected, ranked, "Expected(query=$query): ${case.expected} but got: $ranked")
    }
}

private object RankingTestDSL {
    data class RankingCase(private val query: String) {
        val items = mutableListOf<CompletionItem>()
        val expected = mutableListOf<CompletionItem>()

        fun item(
            label: String,
            sortText: String,
            dataJson: String? = null,
        ): CompletionItem = CompletionItem(label).apply {
            this.sortText = sortText
            if (dataJson != null) this.data = dataJson
            items += this
        }

        fun expectOrder(vararg items: CompletionItem) {
            expected.clear()
            expected += items
        }
    }
}