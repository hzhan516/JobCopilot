package edu.asu.ser594.resumeassistant.application.user.command;

import lombok.Builder;
import lombok.Getter;

// Login by email command
@Getter
@Builder
public class LoginByEmailCommand {
    private String email;
    private String password;
}
