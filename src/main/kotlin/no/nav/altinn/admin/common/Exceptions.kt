package no.nav.altinn.admin.common

class NotFoundException(message: String) : Exception(message)
class TooManyRequestsException(message: String) : Exception(message)
class ServiceUnavailableException(message: String) : Exception(message)
class InvalidInputException(message: String) : Exception(message)
class TransactionsParsingException(message: String) : Exception(message)
