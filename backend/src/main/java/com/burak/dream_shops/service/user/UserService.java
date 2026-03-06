package com.burak.dream_shops.service.user;

import com.burak.dream_shops.dto.UserDto;
import com.burak.dream_shops.exceptions.AlreadyExistsException;
import com.burak.dream_shops.exceptions.ResourcesNotFoundException;
import com.burak.dream_shops.model.Role;
import com.burak.dream_shops.model.User;
import com.burak.dream_shops.repository.RoleRepository.RoleRepository;
import com.burak.dream_shops.repository.User.UserRepository;
import com.burak.dream_shops.request.CreateUserRequest.CreateUserRequest;
import com.burak.dream_shops.request.UpdateUserRequest.UpdateUserRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Kullanıcı (User) işlemlerini yöneten servis sınıfı.
 * Kullanıcı oluşturma, güncelleme, silme ve DTO dönüşümü işlemlerini gerçekleştirir.
 */
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * ID'ye göre ham User entity'sini döner (diğer servislerde ilişki kurmak için kullanılır).
     * Bulunamazsa ResourcesNotFoundException fırlatır.
     */
    @Override
    public User getUserEntityById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourcesNotFoundException("User not found!"));
    }

    /**
     * ID'ye göre kullanıcıyı UserDto olarak döner.
     * @Transactional(readOnly=true): lazy-loaded ilişkileri güvenle okumak için.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        return userRepository.findById(userId)
                .map(this::convertUserToDto)
                .orElseThrow(() -> new ResourcesNotFoundException("User not found!"));
    }

    /**
     * Yeni kullanıcı oluşturur.
     * - Email zaten kayıtlıysa AlreadyExistsException fırlatır.
     * - Şifreyi BCrypt ile şifreler.
     * - Kullanıcıya otomatik ROLE_USER rolü atar.
     */
    @Override
    public UserDto createUser(CreateUserRequest request) {
        return Optional.of(request)
                .filter(user -> !userRepository.existsByEmail(request.getEmail()))
                .map(req -> {
                    User user = new User();
                    user.setEmail(req.getEmail());
                    user.setPassword(passwordEncoder.encode(req.getPassword())); // şifreyi hash'le
                    user.setFirstName(req.getFirstName());
                    user.setLastName(req.getLastName());
                    Role userRole = roleRepository.findByName("ROLE_USER")
                            .orElseThrow(() -> new ResourcesNotFoundException("ROLE_USER not found!"));
                    user.setRoles(Set.of(userRole));
                    return convertUserToDto(userRepository.save(user));
                })
                .orElseThrow(() -> new AlreadyExistsException("Oops! " + request.getEmail() + " already exists!"));
    }

    /**
     * Kullanıcının ad ve soyadını günceller.
     * (Email ve şifre güncelleme bu metotta yapılmaz.)
     */
    @Override
    @Transactional
    public UserDto updateUser(UpdateUserRequest request, Long userId) {
        return userRepository.findById(userId).map(existingUser -> {
            existingUser.setFirstName(request.getFirstName());
            existingUser.setLastName(request.getLastName());
            return convertUserToDto(userRepository.save(existingUser));
        }).orElseThrow(() -> new ResourcesNotFoundException("User not found!"));
    }

    /** Kullanıcıyı siler. Bulunamazsa ResourcesNotFoundException fırlatır. */
    @Override
    public void deleteUser(Long userId) {
        userRepository.findById(userId).ifPresentOrElse(userRepository::delete, () -> {
            throw new ResourcesNotFoundException("User not found!");
        });
    }

    /**
     * User entity'sini UserDto'ya dönüştürür.
     * ModelMapper ile temel alanları kopyalar, ardından rol adlarını
     * Set<String> olarak DTO'ya manuel ekler (ModelMapper Role→String dönüşümünü yapamaz).
     */
    @Override
    public UserDto convertUserToDto(User user) {
        UserDto dto = modelMapper.map(user, UserDto.class);
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        dto.setRoles(roles);
        return dto;
    }
}