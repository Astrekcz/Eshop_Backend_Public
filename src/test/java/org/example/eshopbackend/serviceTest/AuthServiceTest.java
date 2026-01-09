package org.example.eshopbackend.serviceTest;

import org.example.eshopbackend.dto.AuthenticationRequestDTO;
import org.example.eshopbackend.dto.AuthenticationResponseDTO;
import org.example.eshopbackend.dto.CreateUserRequestDTO;
import org.example.eshopbackend.dto.UpdateUserRequestDTO;
import org.example.eshopbackend.entity.Role;
import org.example.eshopbackend.entity.User;
import org.example.eshopbackend.mapper.UserMapper;
import org.example.eshopbackend.repository.UserRepository;
import org.example.eshopbackend.security.CustomUserDetails;
import org.example.eshopbackend.security.JwtService;
import org.example.eshopbackend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService; // Změna z JwtUtil
    @Mock private AuthenticationManager authenticationManager; // Nová závislost
    @Mock private UserMapper userMapper;

    // CustomUserDetailsService už není v AuthService potřeba, volá ho AuthenticationManager interně

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private static User makeUser(Long id, String email, String phone, String password, Role role) {
        User u = new User();
        u.setUserID(id);
        u.setEmail(email);
        u.setPhoneNumber(phone);
        u.setPassword(password);
        u.setRole(role);
        return u;
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("úspěch: nový uživatel – normalizace emailu/telefonu, hash hesla, role USER")
        void register_success() {
            CreateUserRequestDTO req = new CreateUserRequestDTO();
            req.setEmail("  NewUser@Example.Com ");
            req.setPhoneNumber(" +420  777-888-999 ");
            req.setPassword("plainPass");
            req.setFirstName("John");
            req.setLastName("Doe");

            User mapped = new User();
            mapped.setPassword("plainPass");

            when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
            when(userRepository.findByPhoneNumber("+420777888999")).thenReturn(Optional.empty());
            when(userMapper.toUserEntity(req)).thenReturn(mapped);
            when(passwordEncoder.encode("plainPass")).thenReturn("HASHED");

            ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
            User savedReturn = makeUser(42L, "newuser@example.com", "+420777888999", "HASHED", Role.USER);
            when(userRepository.save(savedCaptor.capture())).thenReturn(savedReturn);

            User result = authService.register(req);

            User toSave = savedCaptor.getValue();
            assertEquals("newuser@example.com", toSave.getEmail());
            assertEquals("+420777888999", toSave.getPhoneNumber());
            assertEquals("HASHED", toSave.getPassword());
            assertEquals(Role.USER, toSave.getRole());

            assertNotNull(result);
        }

        @Test
        @DisplayName("fail: email je již použit")
        void register_duplicateEmail() {
            CreateUserRequestDTO req = new CreateUserRequestDTO();
            req.setEmail("EXISTING@EXAMPLE.com");
            req.setPhoneNumber("777 111 222");

            when(userRepository.findByEmail("existing@example.com"))
                    .thenReturn(Optional.of(makeUser(1L, "existing@example.com", null, "h", Role.USER)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(req));
            assertEquals("Email is already taken", ex.getMessage());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("úspěch: validní přihlášení → vygeneruje JWT")
        void login_success() {
            AuthenticationRequestDTO req = new AuthenticationRequestDTO();
            req.setEmail("  USER@Example.com ");
            req.setPassword("plain");

            // 1. Mock AuthenticationManageru (úspěch nevrací nic, jen nehodí výjimku)
            Authentication authMock = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authMock);

            // 2. Mock nalezení uživatele pro generování tokenu
            User user = makeUser(7L, "user@example.com", null, "HASHED", Role.USER);
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

            // 3. Mock JwtService
            when(jwtService.generateToken(anyMap(), any(CustomUserDetails.class))).thenReturn("JWT_TOKEN");

            AuthenticationResponseDTO resp = authService.login(req);

            assertNotNull(resp);
            assertEquals("JWT_TOKEN", resp.getJwtToken());

            // Ověření volání
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtService).generateToken(anyMap(), any(CustomUserDetails.class));
        }

        @Test
        @DisplayName("fail: špatné heslo (AuthenticationManager hodí BadCredentialsException)")
        void login_wrongPassword() {
            AuthenticationRequestDTO req = new AuthenticationRequestDTO();
            req.setEmail("user@example.com");
            req.setPassword("bad");

            // Simulujeme chování Spring Security při špatném hesle
            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

            assertThrows(BadCredentialsException.class, () -> authService.login(req));

            // Repozitář ani JWT by se neměly volat, pokud auth selže
            verify(userRepository, never()).findByEmail(anyString());
            verify(jwtService, never()).generateToken(anyMap(), any());
        }

        @Test
        @DisplayName("fail: uživatel nenalezen v DB (po úspěšném auth manageru - teoreticky by nemělo nastat)")
        void login_userNotFound() {
            // Tento test pokrývá edge case, kdy auth manager projde (mockovaně), ale user není v DB
            AuthenticationRequestDTO req = new AuthenticationRequestDTO();
            req.setEmail("ghost@example.com");
            req.setPassword("pass");

            when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> authService.login(req));
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUserTests {

        @Test
        @DisplayName("úspěch: update hesla a emailu")
        void updateUser_success() {
            Long id = 10L;
            User existing = makeUser(id, "old@example.com", "777111222", "OLD_HASH", Role.USER);

            UpdateUserRequestDTO dto = new UpdateUserRequestDTO();
            dto.setEmail("new@example.com");
            dto.setPassword("newPlain");

            when(userRepository.findById(id)).thenReturn(Optional.of(existing));
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("newPlain")).thenReturn("NEW_HASH");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            User result = authService.updateUser(dto, id);

            assertEquals("new@example.com", result.getEmail());
            assertEquals("NEW_HASH", result.getPassword());
            verify(userMapper).updateUser(existing, dto);
        }

        @Test
        @DisplayName("úspěch: update bez hesla")
        void updateUser_noPassword() {
            Long id = 20L;
            User existing = makeUser(id, "me@example.com", "123", "HASH", Role.USER);

            UpdateUserRequestDTO dto = new UpdateUserRequestDTO();
            dto.setPassword(null);

            when(userRepository.findById(id)).thenReturn(Optional.of(existing));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            User result = authService.updateUser(dto, id);

            assertEquals("HASH", result.getPassword());
            verify(passwordEncoder, never()).encode(any());
        }
    }
}