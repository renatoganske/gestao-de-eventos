package com.renatoganske.gestao_de_eventos.exceptions;

/** A request parameter that binds to its type but has a value the endpoint does not accept (mapped to 400). */
public class InvalidRequestParameterException extends RuntimeException {

    public InvalidRequestParameterException(String message) {
        super(message);
    }
}
