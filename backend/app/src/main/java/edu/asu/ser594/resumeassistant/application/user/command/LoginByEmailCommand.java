package edu.asu.ser594.resumeassistant.application.user.command;

import lombok.Builder;

/**
 * 邮箱登录命令
 * Login by email command
 *
 * 命令对象是不可变的，所有字段使用 final 修饰
 * The command object is immutable and all fields are decorated with final
 */
@Builder
public record LoginByEmailCommand(
    String email,
    String password
) {}
