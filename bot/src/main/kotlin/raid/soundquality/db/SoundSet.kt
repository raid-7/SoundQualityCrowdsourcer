package raid.soundquality.db

import java.io.File

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

    fun proposeDerivative() = derivatives.random().name

    fun getDerivative(name: String): File {
        return derivatives.filter {
            it.name == name
        }.getOrNull(0) ?: throw IllegalArgumentException("No such derivative")
    }
}

class SoundSet(root: File) {
    private val samples: Map<String, SoundSample>

    init {
        if (!root.isDirectory)
            throw IllegalArgumentException("Root must be a directory")

        samples = root.listFiles()!!
            .map { SoundSample(it) }
            .groupBy { it.name }
            .mapValues { it.value[0] }
    }

    fun proposeSample(): Pair<String, String> {
        val sample = samples.values.random()
        return Pair(sample.name, sample.proposeDerivative())
    }

    fun getFiles(sample: String, derivative: String): Pair<File, File> =
        samples[sample]?.let {
            Pair(it.source, it.getDerivative(derivative))
        } ?: throw IllegalArgumentException("No such sample")
}
