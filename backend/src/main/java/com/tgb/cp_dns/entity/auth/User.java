package com.tgb.cp_dns.entity.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tgb.cp_dns.enums.Gender;
import com.tgb.cp_dns.enums.UserStatus;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String fullName;
    private String email;
    private String phone;
    private String password;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @JsonFormat(pattern = "dd-MM-yyyy")
    @Column(columnDefinition = "DATETIME(0)")
    private LocalDateTime createdAt;

    private boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
