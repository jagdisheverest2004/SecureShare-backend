package org.example.secureshare.security.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoResponse {
    private Long userId;
    private String username;
    private List<String> roles;

    private String jwtToken;

    public UserInfoResponse(Long id, String username, List<String> roles) {
        this.userId = id;
        this.username = username;
        this.roles = roles;
    }
}
