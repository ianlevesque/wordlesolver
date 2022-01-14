import com.microsoft.z3.*

const val alphabet = "abcdefghijklmnopqrstuvwxyz"
val reverseAlphabet = alphabet.mapIndexed { index, c -> c to index }.toMap()

class WordleSolver {
    private val z3 = Context()
    private val solver = z3.mkSolver()
    private val letterVariables = (1..5).asSequence().map { z3.mkIntConst("letter_$it") }.toList()

    fun solve(
        doesntContain: String = "",
        contains: String = "",
        containsOnlyOnce: String = "",
        invalidPositions: String = "",
        correctPositions: String = ""
    ) {
        constrainToValidLetterIndexes()
        constrainToValidWords(WORDS)

        doesntContain.toCharArray().forEach { doesntContainLetter(it) }
        contains.toCharArray().forEach { containsLetter(it) }
        containsOnlyOnce.toCharArray().forEach { containsLetterOnlyOnce(it) }

        invalidPositions.toCharArray().mapIndexed { index, c -> index to c }
            .groupBy { it.first / 2 }
            .mapValues { (_, it) -> it[0].second to it[1].second.toString().toInt() }.values
            .forEach { (c, index) ->
                invalidPosition(c, index)
            }

        correctPositions.toCharArray().mapIndexed { index, c -> index to c }
            .groupBy { it.first / 2 }
            .mapValues { (_, it) -> it[0].second to it[1].second.toString().toInt() }.values
            .forEach { (c, index) ->
                correctPosition(c, index)
            }

        val status = solver.check()
        if (status == Status.SATISFIABLE) {
            println(lettersToStr(solver.model, letterVariables))
        } else {
            println("No valid solution")
        }
    }

    private fun doesntContainLetter(c: Char) {
        for (letterVar in letterVariables) {
            solver.add(z3.mkNot(z3.mkEq(letterVar, z3.mkInt(reverseAlphabet[c]!!))))
        }
    }

    private fun constrainToValidWords(
        words: List<String>
    ) {
        solver.add(
            z3.mkOr(*words.map { word ->
                z3.mkAnd(
                    *word.toCharArray()
                        .mapIndexed { index, c ->
                            z3.mkEq(
                                letterVariables[index],
                                z3.mkInt(reverseAlphabet[c]!!)
                            )
                        }.toTypedArray()
                )
            }.toTypedArray())
        )
    }

    private fun containsLetter(c: Char) {
        solver.add(z3.mkOr(*letterVariables.map { letterVar ->
            z3.mkEq(letterVar, z3.mkInt(reverseAlphabet[c]!!))
        }.toTypedArray()))
    }

    private fun containsLetterOnlyOnce(c: Char) {
        solver.add(z3.mkOr(*letterVariables.map { letterVar1 ->
            z3.mkAnd(z3.mkEq(letterVar1, z3.mkInt(reverseAlphabet[c]!!)),
                *letterVariables.filter { it != letterVar1 }.map { letterVar2 ->
                    z3.mkNot(z3.mkEq(letterVar2, z3.mkInt(reverseAlphabet[c]!!)))
                }.toTypedArray()
            )
        }.toTypedArray()))
    }

    private fun invalidPosition(
        c: Char,
        index: Int
    ) {
        solver.add(z3.mkNot(z3.mkEq(letterVariables[index - 1], z3.mkInt(reverseAlphabet[c]!!))))
    }

    private fun correctPosition(
        c: Char,
        index: Int
    ) {
        solver.add(z3.mkEq(letterVariables[index - 1], z3.mkInt(reverseAlphabet[c]!!)))
    }

    private fun constrainToValidLetterIndexes() {
        letterVariables.forEach {
            solver.add(z3.mkGt(it, z3.mkInt(-1)))
            solver.add(z3.mkLt(it, z3.mkInt(26)))
        }
    }

    private fun lettersToStr(model: Model, letterVariables: Iterable<IntExpr>): String {
        return letterVariables
            .map { (model.evaluate(it, false) as IntNum) }
            .map { alphabet[it.int] }
            .joinToString(separator = "")
    }

}

fun main() {
    WordleSolver().solve(
        "shvecirodm",
        "agn",
        "",
        "a3a5a4g1",
        "n3a2g4y5"
    )
}