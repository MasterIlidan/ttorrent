package ru.students.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConf {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests((authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()

                        .requestMatchers("/").permitAll()
                        .requestMatchers("/getPeers").permitAll()
                        .requestMatchers("/getSizeOfTracker").permitAll()
                        .requestMatchers("/getHash").permitAll()
                        .requestMatchers("/announce").permitAll()
                        .requestMatchers("/deleteTorrent/**").permitAll()
                        .requestMatchers("/download/**").permitAll()
                        .anyRequest().authenticated())).csrf().disable();
        return httpSecurity.build();
    }

}
