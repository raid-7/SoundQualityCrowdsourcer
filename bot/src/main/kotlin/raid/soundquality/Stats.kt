package raid.soundquality

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import kotlin.math.sqrt

fun exportStats(file: File, data: Map<Pair<String, String>, List<Int>>) {
    file.delete()
    PrintWriter(BufferedWriter(FileWriter(file))).use { w ->
        w.println("sample,subsample,num_rates,mean,std")

        data.forEach { key, value ->
            val (mean, std) = getStats(value)
            w.println("${key.first},${key.second},${value.size},${mean.format(3)},${std.format(3)}")
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)

private fun getStats(rates: List<Int>): Pair<Double, Double> {
    val data = rates.map(Int::toDouble)
    val sum = data.reduce { acc, d -> acc + d }
    val mean = sum / data.size
    val stdSq = data.fold(0.0) { acc, d -> acc + (d - mean) * (d - mean) }
    return Pair(mean, sqrt(stdSq))
}
