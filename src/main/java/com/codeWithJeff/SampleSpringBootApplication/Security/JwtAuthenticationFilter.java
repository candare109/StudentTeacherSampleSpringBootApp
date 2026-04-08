package com.codeWithJeff.SampleSpringBootApplication.Security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * This filter runs ONCE for EVERY HTTP request (before your controllers).
 *
 * FLOW for each request:
 *   1. Check if "Authorization: Bearer <token>" header exists
 *   2. If no header → skip (let SecurityConfig decide if endpoint is public)
 *   3. If header exists → extract token → validate it
 *   4. If valid → load user from DB → tell Spring Security "this user is authenticated"
 *   5. If invalid → skip (Spring Security will return 401)
 *
 * ANALOGY:
 *   Think of this as a security guard at the door.
 *   Every person (request) walks past the guard.
 *   If they show a valid badge (JWT token), the guard lets them through.
 *   If no badge or fake badge, the guard blocks them.
 */

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
            ) throws ServletException, IOException{
        //1. Get the Authorization header
        final String authHeader = request.getHeader("Authorization");
        //2.If no header or doesn't start with "Bearer ", skip this filter
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }
        //3. Extract the token (remove "Bearer " prefix)
        final String token = authHeader.substring(7);

        //4. Extract the email from the token
        final String email = jwtService.extractEmail(token);
        //5. If we got an email AND user is not already authenticated in this request
        if(email !=null && SecurityContextHolder.getContext().getAuthentication() == null){
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // 7. Validate the token against the loaded user
            if(jwtService.isTokenValid(token, userDetails.getUsername())){
                //8. Create authentication token and set it in SecurityContext
                //   This tells spring: "This request is from an authenticated user"
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null, // no credentials needed(already verified)
                                userDetails.getAuthorities() //roles: ROLE_USER , ROLE_ADMIN
                        );
                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request,response);
    }

}
