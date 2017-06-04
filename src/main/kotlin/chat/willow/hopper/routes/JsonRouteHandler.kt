package chat.willow.hopper.routes

import chat.willow.hopper.auth.BasicSparkAuthFilter
import chat.willow.hopper.routes.shared.ErrorResponseBody
import spark.Request
import spark.Response
import spark.Route

abstract class JsonRouteHandler<RequestType, SuccessType>(val requestAdapter: IStringParser<RequestType>, val successAdapter: IStringSerialiser<SuccessType>, val failureAdapter: IStringSerialiser<ErrorResponseBody>) : IRoute<RequestType, RouteResult<SuccessType, ErrorResponseBody>>, Route {

    override fun handle(request: Request, response: Response): Any? {
        val requestTyped = requestAdapter.from(request.body())
        if (requestTyped == null) {
            response.status(400)
            return ""
        }

        val authenticatedUser = BasicSparkAuthFilter.authenticatedUser(request)

        val result = this.handle(requestTyped, authenticatedUser)

        response.status(result.code)

        if (result.success != null) {
            return successAdapter.serialise(result.success)
        } else if (result.failure != null) {
            return failureAdapter.serialise(result.failure)
        }

        throw RuntimeException("not success or failure")
    }

    fun unauthenticatedError(): RouteResult<SuccessType, ErrorResponseBody> {
        return RouteResult.failure(code = 401, error = ErrorResponseBody(code = 123, message = "not authenticated"))
    }

}