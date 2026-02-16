package com.ab.orderservice.auth.userdetails;


import com.ab.orderservice.auth.model.User;
import com.ab.orderservice.auth.repository.UserRepository;
import com.ab.orderservice.common.exception.enums.ErrorCode;
import com.ab.orderservice.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new NotFoundException(ErrorCode.USER_NOT_FOUND));

        return new CustomUserDetails(user);
    }
}