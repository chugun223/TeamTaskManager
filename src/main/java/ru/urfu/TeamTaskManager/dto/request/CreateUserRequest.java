package TeamManagerTest.ru.urfu.dto.request;

import lombok.*;

@Data
@Builder
public class CreateUserRequest {
    private String username;
    private String email;
    private String password;
}
