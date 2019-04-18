package org.springframework.content.cmis.support;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

@Configuration
@EnableWebSecurity
@EnableJpaAuditing
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth)
			throws Exception {
		auth.
				inMemoryAuthentication()
					.withUser("test")
					.password("{noop}test")
						.roles("USER");
	}

	protected void configure(HttpSecurity http) throws Exception {
		http
				.csrf()
					.disable()
				.authorizeRequests()
					.anyRequest()
						.authenticated()
					.and()
						.httpBasic();
	}

	@Bean
	public AuditorAware<String> folderAuditor() {
		return new AuditorAware<String>() {
			@Override
			public Optional<String> getCurrentAuditor() {
				return Optional.ofNullable(SecurityContextHolder.getContext())
						.map(SecurityContext::getAuthentication)
						.filter(Authentication::isAuthenticated)
						.map(Authentication::getPrincipal)
						.map((u) -> ((User)u).getUsername());
			}
		};
	}
}
