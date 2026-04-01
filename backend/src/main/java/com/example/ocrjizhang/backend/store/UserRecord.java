package com.example.ocrjizhang.backend.store;

public record UserRecord(
    long id,
    String username,
    String passwordHash,
    String nickname,
    String email,
    String phone,
    long updatedAt
) {

    public UserRecord withProfile(String nickname, String email, String phone, String passwordHash, long updatedAt) {
        return new UserRecord(
            id,
            username,
            passwordHash != null ? passwordHash : this.passwordHash,
            nickname != null ? nickname : this.nickname,
            email != null ? email : this.email,
            phone != null ? phone : this.phone,
            updatedAt
        );
    }
}
