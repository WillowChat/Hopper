package chat.willow.hopper.routes

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

interface IStringSerialiser<in T> {
    fun serialise(value: T): String?
}

inline fun <reified Type> Moshi.stringSerialiser(): IStringSerialiser<Type> {
    val adapter = this.adapter(Type::class.java)

    return makeSerialiser(adapter)
}

inline fun <reified JsonType> Moshi.stringSerialiser(type: Type, vararg parameters: Type): IStringSerialiser<JsonType> {
    val workaroundType = Types.newParameterizedType(type, *parameters)
    val adapter = this.adapter<JsonType>(workaroundType)

    return makeSerialiser(adapter)
}

fun <T> makeSerialiser(adapter: JsonAdapter<T>): IStringSerialiser<T> {
    return object : IStringSerialiser<T> {
        override fun serialise(value: T): String? {
            return try {
                adapter.toJson(value)
            } catch (exception: Exception) {
                return null
            }
        }
    }
}