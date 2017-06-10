package chat.willow.hopper.routes

import chat.willow.hopper.auth.BasicAuthSparkFilter
import chat.willow.hopper.routes.shared.ErrorResponseBody
import spark.Request
import spark.Response
import spark.Route

object EmptyContext {

    object Builder: IContextBuilder<EmptyContext> {
        override fun build(request: Request): EmptyContext? {
            return EmptyContext
        }
    }

}

interface IContextBuilder<out ContextType> {
    fun build(request: Request): ContextType?
}

data class AuthenticatedContext(val user: String) {

    object Builder: IContextBuilder<AuthenticatedContext> {
        override fun build(request: Request): AuthenticatedContext? {
            val authenticatedUser = BasicAuthSparkFilter.authenticatedUser(request)

            if (authenticatedUser == null || authenticatedUser.username.isEmpty()) {
                return null
            } else {
                return AuthenticatedContext(user = authenticatedUser.username)
            }
        }
    }

}

abstract class JsonRouteHandler<RequestType, SuccessType, ContextType>(val requestAdapter: IStringParser<RequestType>,
                                                                       val successAdapter: IStringSerialiser<SuccessType>,
                                                                       val failureAdapter: IStringSerialiser<ErrorResponseBody>,
                                                                       val contextBuilder: IContextBuilder<ContextType>) : IRoute<RequestType, RouteResult<SuccessType, ErrorResponseBody>, ContextType>, Route {

    override fun handle(request: Request, response: Response): Any? {
        val requestTyped = requestAdapter.parse(request.body())
        if (requestTyped == null) {
            response.status(400)
            return ""
        }

        val context = contextBuilder.build(request)
        if (context == null) {
            // todo: cleanup - context builder should be able to provide some sort of error

            response.status(500)
            return failureAdapter.serialise(unauthenticatedError().failure!!)
        }

        val result = this.handle(requestTyped, context)

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

fun <T>jsonSuccess(success: T): RouteResult<T, ErrorResponseBody> {
    return RouteResult.success(value = success)
}

fun <T>jsonFailure(code: Int, message: String): RouteResult<T, ErrorResponseBody> {
    return RouteResult.failure(code, error = ErrorResponseBody(code, message))
}