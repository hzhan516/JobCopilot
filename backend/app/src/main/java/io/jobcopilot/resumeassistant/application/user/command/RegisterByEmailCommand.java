package io.jobcopilot.resumeassistant.application.user.command;

import lombok.Builder;

/**
 * 邮箱注册命令
 * Register by email command
 * <p>
 * 命令对象是不可变的，所有字段使用 final 修饰
 * The command object is immutable and all fields are decorated with final
 */
@Builder
public record RegisterByEmailCommand(
        String email,
        String password,
        String verificationCode
) {
}
