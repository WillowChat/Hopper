package chat.willow.hopper.routes.shared

import chat.willow.hopper.routes.IStringParser
import chat.willow.hopper.routes.IStringSerialiser

object EmptyBody : IStringParser<EmptyBody>, IStringSerialiser<EmptyBody> {

    override fun parse(string: String?): EmptyBody? {
        return EmptyBody
    }

    override fun serialise(value: EmptyBody): String? {
        return ""
    }

    override fun toString(): String {
        return "EmptyBody"
    }

}