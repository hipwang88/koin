package org.koin.standalone

import org.koin.core.Koin
import org.koin.core.KoinContext
import org.koin.core.ModuleCallback
import org.koin.core.bean.BeanRegistry
import org.koin.core.instance.InstanceFactory
import org.koin.core.path.PathRegistry
import org.koin.core.property.PropertyRegistry
import org.koin.dsl.module.Module
import org.koin.error.AlreadyStartedException
import org.koin.log.Logger
import org.koin.log.PrintLogger

/**
 * Koin agnostic context support
 * @author - Arnaud GIULIANI
 * @author - Laurent BARESSE
 */
object StandAloneContext {

    private var isStarted = false

    /**
     * Koin ModuleDefinition
     */
    lateinit var koinContext: StandAloneKoinContext

    /**
     * Load Koin modules - whether Koin is already started or not
     * allow late module definition load (e.g: libraries ...)
     *
     * @param modules : List of Module
     */
    fun loadKoinModules(vararg modules: Module): Koin = synchronized(this) {
        createContextIfNeeded()
        return getKoin().build(modules.toList())
    }

    /**
     * Load Koin modules - whether Koin is already started or not
     * allow late module definition load (e.g: libraries ...)
     *
     * @param modules : List of Module
     */
    fun loadKoinModules(modules: List<Module>): Koin = synchronized(this) {
        createContextIfNeeded()
        return getKoin().build(modules)
    }

    /**
     * Create Koin context if needed :)
     */
    private fun createContextIfNeeded() = synchronized(this) {
        if (!isStarted) {
            Koin.logger.log("[context] create")
            val beanRegistry = BeanRegistry()
            val propertyResolver = PropertyRegistry()
            val pathRegistry = PathRegistry()
            val instanceFactory = InstanceFactory()
            koinContext = KoinContext(beanRegistry, pathRegistry, propertyResolver, instanceFactory)
            isStarted = true
        }
    }

    /**
     * Register ModuleDefinition callbacks
     * @see ModuleCallback - ModuleDefinition CallBack
     */
    fun registerCallBack(moduleCallback: ModuleCallback) {
        Koin.logger.log("[context] callback registering with $moduleCallback")
        getKoinContext().contextCallback.add(moduleCallback)
    }

    /**
     * Load Koin properties - whether Koin is already started or not
     * Will look at koin.properties file
     *
     * @param useEnvironmentProperties - environment properties
     * @param useKoinPropertiesFile - koin.properties file
     * @param extraProperties - additional properties
     */
    fun loadProperties(
        useEnvironmentProperties: Boolean = false,
        useKoinPropertiesFile: Boolean = true,
        extraProperties: Map<String, Any> = HashMap()
    ): Koin = synchronized(this) {
        createContextIfNeeded()

        val koin = getKoin()

        if (useKoinPropertiesFile) {
            Koin.logger.log("[properties] load koin.properties")
            koin.bindKoinProperties()
        }

        if (extraProperties.isNotEmpty()) {
            Koin.logger.log("[properties] load extras properties : ${extraProperties.size}")
            koin.bindAdditionalProperties(extraProperties)
        }

        if (useEnvironmentProperties) {
            Koin.logger.log("[properties] load environment properties")
            koin.bindEnvironmentProperties()
        }
        return koin
    }

    /**
     * Koin starter function to load modules and extraProperties
     * Throw AlreadyStartedException if already started
     * @param list : Modules
     * @param useEnvironmentProperties - use environment extraProperties
     * @param useKoinPropertiesFile - use /koin.extraProperties file
     * @param extraProperties - extra extraProperties
     * @param logger - Koin logger
     */
    fun startKoin(
        list: List<Module>,
        useEnvironmentProperties: Boolean = false,
        useKoinPropertiesFile: Boolean = true,
        extraProperties: Map<String, Any> = HashMap(),
        logger: Logger = PrintLogger()
    ): Koin {
        if (isStarted) {
            throw AlreadyStartedException("Koin is already started. Run startKoin only once or use loadKoinModules")
        }
        Koin.logger = logger
        createContextIfNeeded()
        loadKoinModules(list)
        loadProperties(useEnvironmentProperties, useKoinPropertiesFile, extraProperties)
        return getKoin()
    }

    /**
     * Close actual Koin context
     */
    fun closeKoin() = synchronized(this) {
        if (isStarted) {
            // Close all
            getKoinContext().close()
            isStarted = false
        }
    }

    /**
     * Displays Module paths
     */
    fun dumpModulePaths() {
        Koin.logger.log("Module paths:")
        getKoinContext().pathRegistry.paths.forEach { Koin.logger.log("[$it]") }
    }

    /**
     * Get Koin
     */
    private fun getKoin(): Koin = Koin(koinContext as KoinContext)

    /**
     * Get KoinContext
     */
    private fun getKoinContext(): KoinContext = (koinContext as KoinContext)
}

/**
 * Stand alone Koin context
 */
interface StandAloneKoinContext