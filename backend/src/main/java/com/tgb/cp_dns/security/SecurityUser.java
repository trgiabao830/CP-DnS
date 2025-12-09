package com.tgb.cp_dns.security;

import com.tgb.cp_dns.entity.auth.Employee;
import com.tgb.cp_dns.entity.auth.User;
import com.tgb.cp_dns.enums.EmployeeStatus;
import com.tgb.cp_dns.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SecurityUser implements UserDetails {

    @Getter private User user;
    @Getter private Employee employee;

    public SecurityUser(User user) { this.user = user; }
    public SecurityUser(Employee employee) { this.employee = employee; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        if (user != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        } else if (employee != null) { 
            authorities.add(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
            if (employee.getPermissions() != null) {
                authorities.addAll(employee.getPermissions().stream()
                        .map(p -> new SimpleGrantedAuthority(p.getCode()))
                        .collect(Collectors.toList()));
            }
        }
        return authorities;
    }

    @Override
    public String getPassword() { return user != null ? user.getPassword() : employee.getPassword(); }

    @Override
    public String getUsername() { return user != null ? user.getPhone() : employee.getUsername(); }

    @Override
    public boolean isEnabled() {
        if (user != null) return user.getStatus() == UserStatus.ACTIVE;
        if (employee != null) return employee.getStatus() == EmployeeStatus.ACTIVE;
        return false;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
