package com.realestate.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AgentInUseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleAgentInUse(AgentInUseException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleDuplicateEmail(DuplicateEmailException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidRequest(MethodArgumentNotValidException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request fields are invalid.");
    }

    @ExceptionHandler(RoleNotFoundException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleMissingRole(RoleNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    @ExceptionHandler(PropertyNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handlePropertyNotFound(PropertyNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(PropertyAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handlePropertyAccessDenied(PropertyAccessDeniedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(PropertyInUseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handlePropertyInUse(PropertyInUseException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(PropertyImageNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handlePropertyImageNotFound(PropertyImageNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(PropertyImageStorageException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handlePropertyImageStorage(PropertyImageStorageException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    @ExceptionHandler(InvalidPropertyRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidPropertyRequest(InvalidPropertyRequestException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(FavoriteAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleFavoriteAlreadyExists(FavoriteAlreadyExistsException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(FavoriteNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleFavoriteNotFound(FavoriteNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(FavoriteAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleFavoriteAccessDenied(FavoriteAccessDeniedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(CustomerAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleCustomerAccessDenied(CustomerAccessDeniedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(InquiryAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleInquiryAccessDenied(InquiryAccessDeniedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(InquiryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleInquiryNotFound(InquiryNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleUserNotFound(UserNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(VisitNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleVisitNotFound(VisitNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(VisitAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleVisitAccessDenied(VisitAccessDeniedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(VisitCannotBeCancelledException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleVisitCannotBeCancelled(VisitCannotBeCancelledException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(VisitStatusConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleVisitStatusConflict(VisitStatusConflictException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }
}
