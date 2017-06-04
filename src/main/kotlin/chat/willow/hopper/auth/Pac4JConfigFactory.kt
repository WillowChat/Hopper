package chat.willow.hopper.auth

import org.pac4j.core.config.Config
import org.pac4j.core.config.ConfigFactory
import org.pac4j.http.client.direct.DirectBasicAuthClient
import org.pac4j.sparkjava.DefaultHttpActionAdapter

class Pac4JConfigFactory(private val authenticator: IUserTokenAuthenticator) : ConfigFactory {

    override fun build(): Config {
        val httpAuth = DirectBasicAuthClient(Pac4JUserTokenAuthenticator(authenticator))

        val httpActionAdapter = DefaultHttpActionAdapter()

        val config = Config(httpAuth)
        config.httpActionAdapter = httpActionAdapter

        return config
    }

}