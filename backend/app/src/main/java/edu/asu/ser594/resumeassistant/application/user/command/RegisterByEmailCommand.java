package edu.asu.ser594.resumeassistant.application.user.command;

import lombok.Builder;
import lombok.Getter;

// Register by email command
@Getter
@Builder
public class RegisterByEmailCommand {
    private final String email;
    private final String password;
}
