package com.supportassistant.rag;

/** Thrown when the Python RAG service cannot be reached or returns an error. */
public class RagUnavailableException extends RuntimeException {

    public RagUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
