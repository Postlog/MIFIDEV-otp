package com.promoit.otp.service;

import com.promoit.otp.ApiException;
import com.promoit.otp.dao.OtpCodeDao;
import com.promoit.otp.dao.UserDao;
import com.promoit.otp.model.Role;
import com.promoit.otp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Administrative user management: listing non-admin users and deleting users
 * together with their OTP codes.
 */
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserDao userDao;
    private final OtpCodeDao otpCodeDao;

    public UserService(UserDao userDao, OtpCodeDao otpCodeDao) {
        this.userDao = userDao;
        this.otpCodeDao = otpCodeDao;
    }

    /** Returns every user that is not an administrator. */
    public List<User> listNonAdmins() {
        return userDao.findAllExcept(Role.ADMIN);
    }

    /**
     * Deletes a user and all of their OTP codes.
     *
     * @throws ApiException 404 if the user does not exist, 400 if the user is an admin
     */
    public DeletionResult deleteUser(long id) {
        User user = userDao.findById(id)
                .orElseThrow(() -> ApiException.notFound("User " + id + " not found"));
        if (user.getRole() == Role.ADMIN) {
            throw ApiException.badRequest("Administrator accounts cannot be deleted");
        }

        int deletedCodes = otpCodeDao.deleteByUserId(id);
        userDao.deleteById(id);
        log.info("Deleted user '{}' (id={}) and {} OTP code(s)", user.getLogin(), id, deletedCodes);
        return new DeletionResult(id, user.getLogin(), deletedCodes);
    }
}
