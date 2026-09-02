package com.two_m.yourbarber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.dto.user.UpdateProfileDTO;
import com.two_m.yourbarber.dto.user.UserProfileDTO;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.model.Client;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserProfileServiceImpl service;

    private User client(long id) {
        User user =
                Client.builder()
                        .name("Ana")
                        .email("ana@example.com")
                        .password("encoded")
                        .role(UserRole.CLIENT)
                        .phone("111")
                        .build();
        user.setId(id);
        return user;
    }

    @Test
    void getMe_found_returnsDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(client(1L)));

        UserProfileDTO result = service.getMe(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("ana@example.com");
        assertThat(result.getRole()).isEqualTo("CLIENT");
    }

    @Test
    void getMe_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getMe(99L));
    }

    @Test
    void updateMe_updatesNamePhoneAndAvatar() {
        User user = client(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileDTO dto = new UpdateProfileDTO(" Ana Souza ", "  22999  ", "AAAA");
        UserProfileDTO result = service.updateMe(1L, dto);

        assertThat(result.getName()).isEqualTo("Ana Souza");
        assertThat(result.getPhone()).isEqualTo("22999");
        assertThat(result.getAvatarBase64()).isEqualTo("AAAA");
    }

    @Test
    void updateMe_blankOptionalFields_areStoredAsNull() {
        User user = client(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileDTO result = service.updateMe(1L, new UpdateProfileDTO("Ana", "   ", ""));

        assertThat(result.getPhone()).isNull();
        assertThat(result.getAvatarBase64()).isNull();
    }
}
