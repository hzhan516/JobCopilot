package io.jobcopilot.resumeassistant.application.user.command;

import lombok.Builder;

/**
 * Google 登录命令
 * Google login command
 */
@Builder
public record LoginByGoogleCommand(
        String idToken
) {
}
