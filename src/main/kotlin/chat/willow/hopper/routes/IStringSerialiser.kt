package chat.willow.hopper.routes

import com.squareup.moshi.Moshi

interface IStringSerialiser<in T> {
    fun serialise(value: T): String?
}

inline fun <reified Type> Moshi.stringSerialiser(): IStringSerialiser<Type> {
    val adapter = this.adapter(Type::class.java)

    return object : IStringSerialiser<Type> {
        override fun serialise(value: Type): String? {
            return adapter.toJson(value)
        }
    }
}