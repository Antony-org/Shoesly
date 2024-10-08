package gov.iti.jets.services;

import gov.iti.jets.services.dtos.OrderDTO;
import gov.iti.jets.services.dtos.UserOrderDto;
import gov.iti.jets.models.Category;
import gov.iti.jets.models.Product;
import gov.iti.jets.models.User;
import gov.iti.jets.repositories.UserRepository;
import gov.iti.jets.system.exceptions.ObjectNotFoundException;
import gov.iti.jets.system.exceptions.ValidationException;
import gov.iti.jets.system.utils.encryption.PasswordEncryptionUtil;
import gov.iti.jets.system.utils.verification.EmailStatus;
import gov.iti.jets.system.utils.validators.UserValidator;

import org.hibernate.Hibernate;
import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UserService {
    UserRepository userRepository;
    UserValidator userValidator;
    public UserService() {
        userRepository = new UserRepository();
        userValidator = new UserValidator();
    }

    public Optional<User> findUserByUsername(String username) {
        return Optional.ofNullable(userRepository.findByUsername(username)
                .orElseThrow(() -> new ObjectNotFoundException(username)));
    }

    public User save(User user) {
        user.setPassword(PasswordEncryptionUtil.encryptPassword(user.getPassword()));
        return userRepository.save(user);
    }

    public Boolean delete(User user) {
        userRepository.delete(user);
        return true;
    }

    public Boolean delete(Long userId) {
        User foundUser = this.userRepository.findById(userId)
                .orElseThrow(() -> new ObjectNotFoundException("user", userId));
        this.userRepository.delete(userId);
        return true;
    }

    public void changePassword(User user, String oldPassword, String newPassword) {
        List<String> validationErrors = userValidator.validateChangePassword(user, oldPassword, newPassword); // Call the validatePassword() // Call the validateUserInput

        if (!validationErrors.isEmpty()) {
            throw new ValidationException(validationErrors); // Custom exception for validation failures
        }

        user.setPassword(PasswordEncryptionUtil.encryptPassword(newPassword));
        userRepository.update(user);
    }

    public void update(User user) {
        userRepository.update(user);
    }

    public User update(Long userId, User user) {
        // Find the user in the repository
        User foundUser = this.userRepository.findById(userId)
                .orElseThrow(() -> new ObjectNotFoundException("user", userId));

        List<String> validationErrors = validateUserInput(user); // Call the validateUserInput

        if (!validationErrors.isEmpty()) {
            throw new ValidationException(validationErrors); // Custom exception for validation failures
        }

        // Update the necessary fields
        foundUser.setFirstName(user.getFirstName());
        foundUser.setLastName(user.getLastName());
        foundUser.setCountry(user.getCountry());
        foundUser.setCity(user.getCity());
        foundUser.setStreet(user.getStreet());
        foundUser.setCreditLimit(user.getCreditLimit());
        foundUser.setBirthdate(user.getBirthdate());
        foundUser.setPhone(user.getPhone());
        /* update Rest of Attributes : Haroun */
        foundUser.setUsername(user.getUsername());
        foundUser.setEmail(user.getEmail());
        foundUser.clearCategories();
        foundUser.setCategories(user.getInterests());


        // Save the updated user
        return userRepository.update(foundUser);
    }

    public List<String> validateUserInput(User user) {
        // Perform validation at the service layer
        List<String> validationErrors = createUserValidation(user);

        return validationErrors;
    }

    public Set<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long userId) {
        return Optional.ofNullable(this.userRepository.findById(userId)
                .orElseThrow(() -> new ObjectNotFoundException("user", userId)));
    }

    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(this.userRepository.findByEmail(email)
                .orElseThrow(() -> new ObjectNotFoundException("user", email)));
    }


    // Login
    public Optional<User> login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ObjectNotFoundException("User", email));

        // Use BCrypt to check the password against the stored hash
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new ObjectNotFoundException("User", email);  // Password doesn't match, user unauthorized
        } else {
            return Optional.of(user);  // Password matches, return the user
        }
    }


    // Exists
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }


    // Interests
    public Set<Category> getInterests(Long userId) {
        return userRepository.findInterestsByUserId(userId);
    }

    public void updateInterests(Long userId, Set<Category> categories) {
        userRepository.addInterestsToUser(userId, categories);

    }

    public void removeInterest(Long userId, Category category) {
        userRepository.removeInterestFromUser(userId, category);
    }


    // Wishlist
    public Set<Product> getWishlist(Long userId) {
        return userRepository.findWishlistByUserId(userId);
    }

    public void addToWishlist(Long userId, Product product) {
        userRepository.addProductToWishlist(userId, product);
    }

    public void removeFromWishlist(Long userId, Product product) {
        userRepository.removeProductFromWishlist(userId, product);
    }


    // Validation
    public List<String> createUserValidation(User user){
        return userValidator.validateUserInput(user, false, true);
    }


    // Fetch user and orders and map to UserOrderDto
    public Optional<UserOrderDto> getUserOrderDto(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Initialize the lazy-loaded orders collection
            Hibernate.initialize(user.getOrders());
            
            // Map the user's orders to OrderDTO
            List<OrderDTO> orderDTOs = user.getOrders().stream()
            .map(order -> new OrderDTO(order.getId(), order.getOrderDetails()))
            .collect(Collectors.toList());


            // Map the User to UserOrderDto
            UserOrderDto userOrderDto = new UserOrderDto(user.getId(), user.getUsername(), orderDTOs);
            return Optional.of(userOrderDto);
        }
        return Optional.empty();
    }
}



    