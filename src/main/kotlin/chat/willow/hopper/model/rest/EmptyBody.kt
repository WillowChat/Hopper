package chat.willow.hopper.model.rest

import chat.willow.hopper.routes.IStringParser

object EmptyBody : IStringParser<EmptyBody> {

    override fun from(string: String?): EmptyBody? {
        return EmptyBody
    }

}