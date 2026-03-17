package com.tgb.cp_dns.entity.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tgb.cp_dns.enums.EmployeeStatus;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Data
@Table(name = "employees")
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long empId;

    @Column(unique = true, nullable = false)
    private String username;

    private String fullName;
    private String phone;
    private String jobTitle;
    private String password;

    @Enumerated(EnumType.STRING)
    private EmployeeStatus status;
    
    @JsonFormat(pattern = "dd-MM-yyyy")
    @Column(columnDefinition = "DATETIME(0)")
    private LocalDateTime createdAt;

    private boolean isDeleted = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "employee_permissions",
        joinColumns = @JoinColumn(name = "emp_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions;
}
