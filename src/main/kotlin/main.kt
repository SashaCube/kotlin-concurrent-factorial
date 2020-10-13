import kotlinx.coroutines.*
import java.math.BigInteger
import kotlin.system.measureTimeMillis

const val MAX_VALUE_TO_COMPUTE_ON_SINGLE_CORE = 15_000

fun main() {
    runBlocking {

        // initial values
        val targetFactorialNumber = 200_000L
        val cores = Runtime.getRuntime().availableProcessors()

        // launch different computations of the same factorial
        println("Compute factorial of $targetFactorialNumber")
        println()
        println("Use all available cores($cores), to compute concurrently")
        computeOnAllAvailableCores(targetFactorialNumber, cores)
        println()
        println("Use the simplest method")
        computeOnSingleCoroutine(targetFactorialNumber)
    }
}

private suspend fun computeOnAllAvailableCores(
    targetFactorialNumber: Long,
    cores: Int
) = withContext(Dispatchers.Default) {
    var factorial = BigInteger.valueOf(1)

    val time = measureTimeMillis {
        launch {
            factorial = getComputationRangeList(targetFactorialNumber, cores)
                .map { computeRangeAsync(it) }
                .awaitAll()
                .multiplyAll()
        }.join()
    }

    printResults(time, factorial)
}

suspend fun computeOnSingleCoroutine(targetFactorialNumber: Long) {
    withContext(Dispatchers.Default) {
        var factorial = BigInteger.valueOf(1)

        val time = measureTimeMillis {
            launch {
                for (i in 1..targetFactorialNumber) {
                    factorial = factorial.multiply(
                        BigInteger.valueOf(i)
                    )
                }
            }.join()
        }

        printResults(time, factorial)
    }
}

fun getComputationRangeList(targetFactorialNumber: Long, numOfCores: Int): List<Pair<Long, Long>> {
    val numOfNeededRanges = if (targetFactorialNumber < MAX_VALUE_TO_COMPUTE_ON_SINGLE_CORE) 1 else numOfCores
    val computationRangeList = mutableListOf<Pair<Long, Long>>()
    val computationRangeSizeForOneCore = targetFactorialNumber / numOfNeededRanges

    var first: Long
    var second = 1L

    for (i in numOfNeededRanges downTo 1) {
        first = second
        second = targetFactorialNumber - computationRangeSizeForOneCore * (i - 1)

        // last ranges end value should be full
        if (i == 1) second = targetFactorialNumber

        computationRangeList.add(first + 1 to second)
    }

    return computationRangeList
}

private fun CoroutineScope.computeRangeAsync(range: Pair<Long, Long>): Deferred<BigInteger> {
    return async {
        var partResult = BigInteger.valueOf(1)
        for (i in range.first..range.second) {
            partResult = partResult.multiply(BigInteger.valueOf(i))
        }
        partResult
    }
}

suspend fun List<BigInteger>.multiplyAll(): BigInteger = withContext(Dispatchers.Default) {
    var result = BigInteger.valueOf(1)
    forEach {
        result = result.multiply(it)
    }
    result
}

fun printResults(time: Long, factorial: BigInteger) {
    println("time: $time")
}
