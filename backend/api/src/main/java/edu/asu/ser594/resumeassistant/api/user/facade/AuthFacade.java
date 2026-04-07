package edu.asu.ser594.resumeassistant.api.user.facade;

import edu.asu.ser594.resumeassistant.api.user.dto.request.LoginByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.RegisterByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.response.AuthResponse;

// Authentication facade interface
public interface AuthFacade {
    // Register by email
    AuthResponse registerByEmail(RegisterByEmailRequest request);

    // Login by email
    AuthResponse loginByEmail(LoginByEmailRequest request);
}
