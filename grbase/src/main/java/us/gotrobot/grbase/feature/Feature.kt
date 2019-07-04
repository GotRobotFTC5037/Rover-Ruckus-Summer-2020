package us.gotrobot.grbase.feature

import org.firstinspires.ftc.robotcore.external.Telemetry
import us.gotrobot.grbase.robot.RobotContext
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

abstract class Feature {

    lateinit var telemetry: Telemetry
        internal set

    private val log by lazy { telemetry.log() }

    fun log(data: String) {
        log.add(data)
    }

}


interface FeatureConfiguration

abstract class FeatureInstaller<TFeature : Feature, TConfiguration : FeatureConfiguration> {

    abstract val name: String

    abstract suspend fun install(
        context: RobotContext,
        featureSet: FeatureSet,
        configure: TConfiguration.() -> Unit
    ): TFeature

}

interface FeatureKey<T : Any>

fun <F : Any> featureKey() = object : FeatureKey<F> {}

abstract class KeyedFeatureInstaller<F : Feature, C : FeatureConfiguration> :
    FeatureInstaller<F, C>(), FeatureKey<F>

interface FeatureSet : Iterable<Feature> {

    operator fun <F : Feature> contains(key: FeatureKey<F>): Boolean

    operator fun <F : Any> contains(featureClass: KClass<F>): Boolean

    operator fun <F : Feature> get(key: FeatureKey<F>): F

    fun <F : Any> getAll(featureClass: KClass<F>): List<F>

}

fun <F : Any> FeatureSet.getSingle(featureClass: KClass<F>): F = getAll(featureClass).single()

class MutableFeatureSet : FeatureSet {

    private val features: MutableMap<FeatureKey<*>, Feature> = mutableMapOf()

    operator fun <F : Feature> set(key: FeatureKey<F>, feature: F): Boolean =
        if (key !in this) {
            features[key] = feature; true
        } else false

    override fun <F : Feature> contains(key: FeatureKey<F>): Boolean = key in features

    override fun <F : Any> contains(featureClass: KClass<F>): Boolean =
        features.toList().any { it.second::class.isSubclassOf(featureClass) }

    @Suppress("UNCHECKED_CAST")
    override fun <F : Feature> get(key: FeatureKey<F>): F =
        try {
            features[key] as F
        } catch (e: ClassCastException) {
            throw MissingFeatureException(key)
        }


    @Suppress("UNCHECKED_CAST")
    override fun <F : Any> getAll(featureClass: KClass<F>): List<F> =
        features.toList()
            .filter { it.second::class.isSubclassOf(featureClass) }
            .map { it.second as F }


    override fun iterator(): Iterator<Feature> = object : Iterator<Feature> {
        override fun hasNext(): Boolean = features.iterator().hasNext()
        override fun next(): Feature = features.iterator().next().component2()
    }

}

class MissingFeatureException(key: FeatureKey<*>) :
    RuntimeException("${key::class.qualifiedName} isn't installed.")