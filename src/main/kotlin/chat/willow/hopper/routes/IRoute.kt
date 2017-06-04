package chat.willow.hopper.routes

import chat.willow.hopper.auth.BasicSparkAuthFilter

interface IRoute<in RequestType, out ResponseType> {
    fun handle(request: RequestType, user: BasicSparkAuthFilter.AuthenticatedUser?): ResponseType
}