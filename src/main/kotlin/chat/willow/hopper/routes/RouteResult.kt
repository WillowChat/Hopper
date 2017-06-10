package chat.willow.hopper.routes

data class RouteResult<out SuccessType, out FailureType> private constructor (val code: Int, val success: SuccessType? = null, val failure: FailureType? = null) {
    companion object {
        fun <SuccessType, FailureType>success(code: Int = 200, value: SuccessType) = RouteResult<SuccessType, FailureType>(code = code, success = value)
        fun <SuccessType, FailureType>failure(code: Int = 500, error: FailureType) = RouteResult<SuccessType, FailureType>(code = code, failure = error)
    }

    val isSuccess = success != null
    val isFailure = failure != null
}