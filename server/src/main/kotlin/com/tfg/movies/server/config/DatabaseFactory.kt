package com.tfg.movies.server.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import org.slf4j.LoggerFactory
import javax.sql.DataSource

/**
 * Manages the HikariCP connection pool to the PostgreSQL database.
 *
 * The pool is created once at application startup, reused for every
 * request, and closed when the application stops.
 *
 * Connection details (URL, pool size) are read from application.yaml,
 * where DATABASE_URL is in turn injected from the runtime environment
 * (.env file in local development, environment variables in production).
 */
object DatabaseFactory {

    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    /**
     * The DataSource exposed to the rest of the application.
     * Backed by HikariCP. Initialized in [init], closed in [close].
     */
    lateinit var dataSource: DataSource
        private set

    /**
     * Initializes the connection pool reading from the provided
     * Ktor configuration. Called once during application startup.
     */
    fun init(config: ApplicationConfig) {
        val jdbcUrl = config.property("database.jdbcUrl").getString()
        val user = config.property("database.user").getString()
        val password = config.property("database.password").getString()
        val poolSize = config.property("database.poolSize").getString().toInt()

        logger.info("Initializing HikariCP pool (size=$poolSize)")

        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = user
            this.password = password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = poolSize
            minimumIdle = 2
            connectionTestQuery = "SELECT 1"
            connectionTimeout = 30_000
            idleTimeout = 600_000
            maxLifetime = 1_800_000
            poolName = "tfg-movies-pool"
        }

        dataSource = HikariDataSource(hikariConfig)
        logger.info("HikariCP pool initialized")
    }

    /**
     * Closes the connection pool. Called when the application stops.
     */
    fun close() {
        if (::dataSource.isInitialized && dataSource is HikariDataSource) {
            logger.info("Closing HikariCP pool")
            (dataSource as HikariDataSource).close()
        }
    }

    /**
     * Performs a lightweight health check by acquiring a connection
     * from the pool and executing a trivial query. Returns true if
     * the database is reachable, false otherwise.
     *
     * Errors are logged but not thrown — this is intentional: a
     * health check should never crash the caller.
     */
    fun isHealthy(): Boolean {
        return try {
            dataSource.connection.use { conn ->
                conn.prepareStatement("SELECT 1").use { stmt ->
                    stmt.executeQuery().use { rs ->
                        rs.next() && rs.getInt(1) == 1
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Database health check failed: ${e.message}")
            false
        }
    }
}