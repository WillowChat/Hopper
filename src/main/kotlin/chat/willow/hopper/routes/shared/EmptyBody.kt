package chat.willow.hopper.routes.shared

import chat.willow.hopper.routes.IStringParser

object EmptyBody : IStringParser<EmptyBody> {

    override fun parse(string: String?): EmptyBody? {
        return EmptyBody
    }

    override fun toString(): String {
        return "EmptyBody"
    }

}