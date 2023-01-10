package com.wafflestudio.snu4t.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

@Configuration
@Profile("!test")
class SecretsManagerConfig : EnvironmentAware, BeanFactoryPostProcessor {
    private lateinit var env: Environment

    override fun setEnvironment(environment: Environment) {
        env = environment
    }

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        val secretNames = env.getProperty("secret-names", "").split(",")
        val region = Region.AP_NORTHEAST_2
        val objectMapper = jacksonObjectMapper()

        secretNames.forEach { secretName ->
            val secretString = getSecretString(secretName, region)
            val map = objectMapper.readValue<Map<String, String>>(secretString)
            map.forEach { (key, value) -> System.setProperty(key, value) }
        }
    }

    fun getSecretString(secretName: String, region: Region): String {
        val client = SecretsManagerClient.builder().region(region).build()
        val request = GetSecretValueRequest.builder().secretId(secretName).build()
        return client.getSecretValue(request).secretString()
    }
}
