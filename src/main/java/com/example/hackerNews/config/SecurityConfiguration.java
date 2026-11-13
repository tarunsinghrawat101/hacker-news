package com.example.hackerNews.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Override
    protected void configure(
            AuthenticationManagerBuilder authenticationManagerBuilder)
            throws Exception {

        authenticationManagerBuilder.authenticationProvider(
                authenticationProvider()
        );
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
                .authorizeRequests()
                .antMatchers(
                        "/", "/page/**", "/newNews/**", "/pastNews/**",
                        "/registration", "/forgotPassword", "/resetPassword",
                        "/login", "/css/**", "/js/**", "/images/**",
                        "/webjars/**", "/favicon.ico"
                ).permitAll()
                .antMatchers("/admin", "/admin/**")
                .hasAuthority("ROLE_ADMIN")
                .anyRequest()
                .hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                .and()
                .formLogin()
                .loginPage("/login")
                .failureUrl("/login?error=true")
                .permitAll()
                .and()
                .logout()
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .permitAll()
                .and()
                .csrf().disable();
    }
}