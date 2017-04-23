package chat.willow.hopper.routes

import com.squareup.moshi.Moshi

interface IStringParser<out T> {
    fun from(string: String?): T?
}

inline fun <reified Type> Moshi.stringParser(): IStringParser<Type> {
    val adapter = this.adapter(Type::class.java)

    return object : IStringParser<Type> {

        override fun from(string: String?): Type? {
            if (string == null) {
                return null
            }

            return adapter.fromJson(string)
        }

    }
}