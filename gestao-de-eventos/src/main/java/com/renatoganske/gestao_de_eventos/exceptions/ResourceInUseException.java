package com.renatoganske.gestao_de_eventos.exceptions;

public class ResourceInUseException extends RuntimeException {

    public ResourceInUseException(String message) {
        super(message);
    }
}
