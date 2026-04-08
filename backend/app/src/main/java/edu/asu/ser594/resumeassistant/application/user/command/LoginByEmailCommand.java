package edu.asu.ser594.resumeassistant.application.user.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 邮箱登录命令
 * Login by email command
 *
 * 命令对象是不可变的，所有字段使用 final 修饰
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginByEmailCommand {
    private String email;
    private String password;
}
