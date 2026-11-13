package com.scasp.mobile.data.remote

import java.io.IOException

/** The server answered with an error status (401 session expired, 403, 404, 409 conflict, 400 validation...). */
class ApiException(val status: Int, message: String) : Exception(message)

/** The server could not be reached (no network, wrong address, timeout). */
class OfflineException(message: String, cause: Throwable? = null) : IOException(message, cause)
