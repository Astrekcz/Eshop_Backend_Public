package org.example.eshopbackend.serviceTest;
// src/test/java/org/example/zeniqbackend/service/AuthServiceTest.java


import org.example.eshopbackend.dto.AuthenticationRequestDTO;
import org.example.eshopbackend.dto.AuthenticationResponseDTO;
import org.example.eshopbackend.dto.CreateUserRequestDTO;
import org.example.eshopbackend.dto.UpdateUserRequestDTO;
import org.example.eshopbackend.entity.Role;
import org.example.eshopbackend.entity.User;
import org.example.eshopbackend.mapper.UserMapper;
import org.example.eshopbackend.repository.UserRepository;
import org.example.eshopbackend.service.AuthService;
import org.example.eshopbackend.service.CustomUserDetailsService;
import org.example.eshopbackend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private UserMapper userMapper;

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
            mapped.setPassword("plainPass"); // mapper vrací raw heslo z DTO

            when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
            when(userRepository.findByPhoneNumber("+420777888999")).thenReturn(Optional.empty());
            when(userMapper.toUserEntity(req)).thenReturn(mapped);
            when(passwordEncoder.encode("plainPass")).thenReturn("HASHED");
            // zachytíme, co jde do save
            ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
            User saved = makeUser(42L, "newuser@example.com", "+420777888999", "HASHED", Role.USER);
            when(userRepository.save(savedCaptor.capture())).thenReturn(saved);

            User result = authService.register(req);

            // ověř hodnoty před uložením
            User toSave = savedCaptor.getValue();
            assertEquals("newuser@example.com", toSave.getEmail());
            assertEquals("+420777888999", toSave.getPhoneNumber());
            assertEquals("HASHED", toSave.getPassword());
            assertEquals(Role.USER, toSave.getRole());

            // výsledek z repo.save
            assertNotNull(result);
            assertEquals(42L, result.getUserID());
            assertEquals("newuser@example.com", result.getEmail());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("fail: email je již použit")
        void register_duplicateEmail() {
            CreateUserRequestDTO req = new CreateUserRequestDTO();
            req.setEmail("EXISTING@EXAMPLE.com");
            req.setPhoneNumber("777 111 222");
            req.setPassword("x");

            when(userRepository.findByEmail("existing@example.com"))
                    .thenReturn(Optional.of(makeUser(1L, "existing@example.com", null, "h", Role.USER)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(req));
            assertEquals("Email is already taken", ex.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("fail: telefon je již použit")
        void register_duplicatePhone() {
            CreateUserRequestDTO req = new CreateUserRequestDTO();
            req.setEmail("new@ex.com");
            req.setPhoneNumber("  777-111-222 ");
            req.setPassword("x");

            when(userRepository.findByEmail("new@ex.com")).thenReturn(Optional.empty());
            when(userRepository.findByPhoneNumber("777111222"))
                    .thenReturn(Optional.of(makeUser(2L, "a@b.c", "777111222", "h", Role.USER)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(req));
            assertEquals("Phone number is already taken", ex.getMessage());
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

            User user = makeUser(7L, "user@example.com", null, "HASHED", Role.USER);

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("plain", "HASHED")).thenReturn(true);

            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername("user@example.com")
                    .password("HASHED")
                    .roles("USER").build();
            when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
            when(jwtUtil.generateToken(userDetails, 7L)).thenReturn("JWT_TOKEN");

            AuthenticationResponseDTO resp = authService.login(req);

            assertNotNull(resp);
            assertEquals("JWT_TOKEN", resp.getJwtToken());
            verify(userDetailsService).loadUserByUsername("user@example.com");
            verify(jwtUtil).generateToken(userDetails, 7L);
        }

        @Test
        @DisplayName("fail: uživatel nenalezen")
        void login_userNotFound() {
            AuthenticationRequestDTO req = new AuthenticationRequestDTO();
            req.setEmail("no@no.com");
            req.setPassword("x");

            when(userRepository.findByEmail("no@no.com")).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(req));
            assertEquals("Invalid credentials", ex.getMessage());
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("fail: špatné heslo")
        void login_wrongPassword() {
            AuthenticationRequestDTO req = new AuthenticationRequestDTO();
            req.setEmail("user@example.com");
            req.setPassword("bad");

            User user = makeUser(5L, "user@example.com", null, "HASHED", Role.USER);

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("bad", "HASHED")).thenReturn(false);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(req));
            assertEquals("Invalid credentials", ex.getMessage());
            verify(userDetailsService, never()).loadUserByUsername(any());
            verify(jwtUtil, never()).generateToken(any(), anyLong());
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUserTests {

        @Test
        @DisplayName("úspěch: změna emailu/telefonu + hash nového hesla + delegace na mapper")
        void updateUser_success_all() {
            Long id = 10L;
            User existing = makeUser(id, "old@example.com", "777111222", "OLD_HASH", Role.USER);

            UpdateUserRequestDTO dto = new UpdateUserRequestDTO();
            dto.setEmail("  New@Example.com ");
            dto.setPhoneNumber(" +420  777-333-444 ");
            dto.setPassword("newPlain");
            // plus další volitelná pole… mapper si je doplní

            when(userRepository.findById(id)).thenReturn(Optional.of(existing));
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(userRepository.findByPhoneNumber("+420777333444")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("newPlain")).thenReturn("NEW_HASH");

            ArgumentCaptor<User> saveCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(saveCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            User result = authService.updateUser(dto, id);

            // ověř normalizaci a hash
            assertEquals("new@example.com", result.getEmail());
            assertEquals("+420777333444", result.getPhoneNumber());
            assertEquals("NEW_HASH", result.getPassword());
            // mapper byl volán pro kopii business polí
            verify(userMapper).updateUser(existing, dto);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("úspěch: bez změny hesla → nehashuje se")
        void updateUser_noPassword_noHash() {
            Long id = 20L;
            User existing = makeUser(id, "me@example.com", "123", "HASH", Role.USER);

            UpdateUserRequestDTO dto = new UpdateUserRequestDTO();
            dto.setEmail("me@example.com"); // beze změny
            dto.setPhoneNumber("123");      // beze změny
            dto.setPassword(null);          // žádné nové heslo

            when(userRepository.findById(id)).thenReturn(Optional.of(existing));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            User result = authService.updateUser(dto, id);

            assertEquals("HASH", result.getPassword());
            verify(passwordEncoder, never()).encode(any());
            verify(userMapper).updateUser(existing, dto);
        }

        @Test
        @DisplayName("fail: email koliduje s jiným uživatelem")
        void updateUser_conflictEmail() {
            Long id = 30L;
            User existing = makeUser(id, "old@example.com", "777111222", "HASH", Role.USER);

            UpdateUserRequestDTO dto = new UpdateUserRequestDTO();
            dto.setEmail("NEW@example.com");

            when(userRepository.findById(id)).thenReturn(Optional.of(existing));
            when(userRepository.findByEmail("new@example.com"))
                    .thenReturn(Optional.of(makeUser(999L, "new@example.com", null, "X", Role.USER)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.updateUser(dto, id));
            assertEquals("Email is already taken", ex.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("fail: telefon koliduje s jiným uživatelem")
        void updateUser_conflictPhone() {
            Long id = 31L;
            User existing = makeUser(id, "me@ex.com", "777111222", "HASH", Role.USER);

            UpdateUserRequestDTO dto = new UpdateUserRequestDTO();
            dto.setPhoneNumber("  777-555-666 ");

            when(userRepository.findById(id)).thenReturn(Optional.of(existing));
            when(userRepository.findByPhoneNumber("777555666"))
                    .thenReturn(Optional.of(makeUser(77L, "x@y.z", "777555666", "H", Role.USER)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.updateUser(dto, id));
            assertEquals("Phone number is already taken", ex.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("fail: uživatel neexistuje")
        void updateUser_userNotFound() {
            when(userRepository.findById(123L)).thenReturn(Optional.empty());
            UpdateUserRequestDTO dto = new UpdateUserRequestDTO();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.updateUser(dto, 123L));
            assertEquals("User not found", ex.getMessage());
        }
    }
}

