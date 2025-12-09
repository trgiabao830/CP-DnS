package com.tgb.cp_dns.security;

import com.tgb.cp_dns.entity.auth.Employee;
import com.tgb.cp_dns.entity.auth.User;
import com.tgb.cp_dns.repository.auth.EmployeeRepository;
import com.tgb.cp_dns.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        Optional<Employee> empOpt = employeeRepository.findByUsername(identifier);
        if (empOpt.isPresent()) {
            return new SecurityUser(empOpt.get());
        }

        Optional<User> userOpt = userRepository.findByPhone(identifier);
        if (userOpt.isPresent()) {
            return new SecurityUser(userOpt.get());
        }

        throw new UsernameNotFoundException("Tài khoản không tồn tại: " + identifier);
    }
}
