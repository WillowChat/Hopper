package chat.willow.hopper.routes

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

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

inline fun <reified JsonType> Moshi.stringSerialiser(type: Type, vararg parameters: Type): IStringSerialiser<JsonType> {
    val workaroundType = Types.newParameterizedType(type, *parameters)
    val adapter = this.adapter<JsonType>(workaroundType)

    return object : IStringSerialiser<JsonType> {
        override fun serialise(value: JsonType): String? {
            return adapter.toJson(value)
        }
    }
}