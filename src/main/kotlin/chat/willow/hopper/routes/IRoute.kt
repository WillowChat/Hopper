package chat.willow.hopper.routes

import org.pac4j.core.profile.CommonProfile

interface IRoute<in RequestType, out ResponseType> {
    fun handle(request: RequestType, user: CommonProfile?): ResponseType
}