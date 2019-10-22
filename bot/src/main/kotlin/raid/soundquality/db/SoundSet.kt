package raid.soundquality.db

import java.io.File
import java.util.*

class SoundSample(private val root: File) {
    private val derivatives: Array<File>

    val source: File
    val name: String
        get() = root.name

    init {
        if (!root.isDirectory)
            throw IllegalArgumentException("Root must be a directory")

        val separated = root.listFiles()!!.groupBy {
            it.name.startsWith("source")
        }
        source = separated[true]
            ?.let { it[0] }
            ?: throw IllegalArgumentException("Cannot find source of the sample")

        derivatives = separated[false]?.toTypedArray() ?: arrayOf()
    }

    fun proposeDerivative(random: Random) =
        if (derivatives.isEmpty()) {
            null
        } else {
            derivatives[random.nextInt(derivatives.size)].name
        }

    fun getDerivative(name: String): File {
        return derivatives.filter {
            it.name == name
        }.getOrNull(0) ?: throw IllegalArgumentException("No such derivative")
    }
}

class SoundSet(root: File) {
    private val samples: Map<String, SoundSample>
    private val samplesList: List<SoundSample>

    private val random: Random = Random()

    init {
        if (!root.isDirectory)
            throw IllegalArgumentException("Root must be a directory")

        samplesList = root.listFiles()!!
            .map { SoundSample(it) }
        samples = samplesList
            .groupBy { it.name }
            .mapValues { it.value[0] }
    }

    fun proposeSample(): Pair<String, String> {
        while (true) {
            val sample = samplesList[random.nextInt(samples.size)]
            val derivative = sample.proposeDerivative(random)
            if (derivative != null)
                return Pair(sample.name, derivative)
        }
    }

    fun getFiles(sample: String, derivative: String): Pair<File, File> =
        samples[sample]?.let {
            Pair(it.source, it.getDerivative(derivative))
        } ?: throw IllegalArgumentException("No such sample")
}
