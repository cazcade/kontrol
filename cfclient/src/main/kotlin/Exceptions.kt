package kontrol.cfclient



public open class ResourceNotFoundException(message:String?=null) : Exception(message) {
}

public open class RequestUnsuccessfulException(message:String?=null,cause:Throwable?=null) : Exception(message,cause) {
}

public open class AccessDeniedException(message:String?=null) : Exception(message) {

}


