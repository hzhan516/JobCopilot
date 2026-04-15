package edu.asu.ser594.resumeassistant.application.user.command;

import lombok.Builder;

/**
 * 邮箱注册命令
 * Register by email command
 *
 * 命令对象是不可变的，所有字段使用 final 修饰
 */
@Builder
public record RegisterByEmailCommand(
    String email,
    String password
) {}
