package com.codeWithJeff.SampleSpringBootApplication.Security;


import com.codeWithJeff.SampleSpringBootApplication.Exceptions.UsernameNotFoundException;
import com.codeWithJeff.SampleSpringBootApplication.Entity.User;
import com.codeWithJeff.SampleSpringBootApplication.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security needs a way to load user data from YOUR database.
 * This class bridges YOUR User entity ↔ Spring Security's UserDetails.
 *
 * FLOW:
 *   JwtAuthenticationFilter extracts email from token
 *   → calls this service to load the user from DB
 *   → Spring Security uses the returned UserDetails to set the security context
 */

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException{
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found" + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }



}
