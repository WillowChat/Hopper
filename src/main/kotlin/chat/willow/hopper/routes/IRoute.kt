package chat.willow.hopper.routes

interface IRoute<in RequestType, out ResponseType, in ContextType> {
    fun handle(request: RequestType, context: ContextType): ResponseType
}