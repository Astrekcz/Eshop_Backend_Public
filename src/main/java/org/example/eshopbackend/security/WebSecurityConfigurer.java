package org.example.eshopbackend.security;


import lombok.RequiredArgsConstructor;
import org.example.eshopbackend.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity    // povolí @PreAuthorize, @Secured apod.
@RequiredArgsConstructor
public class WebSecurityConfigurer {
    private final CustomUserDetailsService userDetailsService;
    private final JwtRequestFilter jwtRequestFilter;
    private final AuthenticationEntryPoint jwtAuthEntryPoint;

    @Value("${public.endpoints:/api/auth/**}")
    private String[] publicEndpoints;

    @Bean
    public SecurityFilterChain securityFilterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        http
                // 1) CORS a CSRF
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // explicitně
                .csrf(AbstractHttpConfigurer::disable)

                // 2) Chování při neautorizovaném přístupu
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthEntryPoint)
                )

                // 3) Statelss session – REST + JWT
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 4) Registrace vlastního AuthenticationProvider
                .authenticationProvider(daoAuthenticationProvider())

                // 5) Pravidla pro URL + role
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicEndpoints).permitAll()      // veřejné endpointy
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()   // povolit pre-flight
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/orders/**").hasAnyRole("USER","ADMIN") //TODO zmenit
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/products/**").permitAll()
                        .requestMatchers("/api/catalog/**").permitAll()
                        .requestMatchers("/api/order/**").permitAll()
                        .requestMatchers("/api/payments/**").permitAll()
                        .requestMatchers("/api/verification/send").permitAll()
                        .requestMatchers("/api/verification/verify").permitAll()
                        .requestMatchers("/api/shipping/ppl/_diag/**").hasRole("ADMIN")
                        .requestMatchers("/api/shipping/**").hasRole("ADMIN")

                        .requestMatchers("/images/**","/css/**","/js/**","/static/**","/favicon.ico","/","/error").permitAll()


                        .anyRequest().authenticated()
                )

                // 6) Náš JWT-filter před standardním filtrem
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    /** Explicitní DAO-provider, který použije CustomUserDetailsService + BCrypt */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /** Silné BCrypt heslo */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Jednoduchá CORS konfigurace – upravte origins podle produkce */
// WebSecurityConfigurer.corsConfigurationSource()

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        cfg.setAllowedOriginPatterns(Arrays.asList(

                "https://https://eshop-frontend-kappa.vercel.app/",
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));

        cfg.setAllowCredentials(true);
        cfg.setAllowedMethods(Arrays.asList("GET","POST","PUT","PATCH","DELETE","OPTIONS"));


        cfg.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "X-Adulto-Dev"     // <—
        ));

        cfg.setExposedHeaders(Arrays.asList("Authorization","Location","Content-Disposition"));
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }




}