package com.realestate.exception;

public class AgentInUseException extends RuntimeException {

    public AgentInUseException(Long agentId) {
        super("Agent " + agentId + " has related records and cannot be deleted.");
    }
}
