//package com.picoding.first_project.security
//
//import com.picoding.first_project.database.model.RefreshToken
//import com.picoding.first_project.database.model.User
//import com.picoding.first_project.database.repoitory.RefreshTokenRepo
//import com.picoding.first_project.database.repoitory.UserRepo
//import com.picoding.fish.security.HashEncoder
//import com.picoding.fish.security.JWTService
//import org.bson.types.ObjectId
//import org.springframework.http.HttpStatus
//import org.springframework.http.HttpStatusCode
//import org.springframework.security.authentication.BadCredentialsException
//import org.springframework.stereotype.Service
//import org.springframework.transaction.annotation.Transactional
//import org.springframework.web.server.ResponseStatusException
//import java.security.MessageDigest
//import java.time.Instant
//import java.util.Base64
//
//@Service
//class AuthService(
//    private val jwtService: JWTService,
//    private val userRepo: UserRepo,
//    private val refreshTokenRepo: RefreshTokenRepo,
//    private val hashEncoder: HashEncoder
//) {
//
//    data class TokenPair(
//        val accessToken: String,
//        val refreshToken: String,
//    )
//
//
//    fun register(email: String, password: String): User {
//        val user = userRepo.findByEmail(email.trim())
//        if (user != null) {
//            throw ResponseStatusException(HttpStatus.CONFLICT, "User already exists")
//        }
//        return userRepo.save(User(
//            email = email,
//            hashedPassword = hashEncoder.encode(password)
//        ))
//    }
//
//    fun login(email: String, password: String): TokenPair {
//        val user = userRepo.findByEmail(email)
//            ?: throw BadCredentialsException("User not found.")
//
//        if (!hashEncoder.matches(password, user.hashedPassword)) {
//            throw BadCredentialsException("Invalid password.")
//        }
//
//        val newAccessToken = jwtService.generateAccessToken(user.id.toHexString())
//        val newRefreshToken = jwtService.generateRefreshToken(user.id.toHexString())
//
//        storeRefreshToken(user.id, newRefreshToken)
//
//        return TokenPair(
//            accessToken = newAccessToken,
//            refreshToken = newRefreshToken,
//        )
//    }
//
//    @Transactional
//    fun refresh(refreshToken: String): TokenPair {
//        if (!jwtService.validateRefreshToken(refreshToken)) {
//            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token.")
//        }
//
//        val userId = jwtService.getUserIdFromToken(refreshToken)
//        val user = userRepo.findById(ObjectId(userId)).orElseThrow {
//            ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token.")
//        }
//
//        val hashed = hashToken(refreshToken)
//        refreshTokenRepo.findByUserIdAndHashedToken(user.id, hashed)
//            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Refresh token not recognized.")
//
//        refreshTokenRepo.deleteByUserIdAndHashedToken(user.id, hashed)
//
//        val newAccessToken = jwtService.generateAccessToken(user.id.toHexString())
//        val newRefreshToken = jwtService.generateRefreshToken(user.id.toHexString())
//
//        storeRefreshToken(user.id, newRefreshToken)
//
//        return TokenPair(
//            accessToken = newAccessToken,
//            refreshToken = newRefreshToken,
//        )
//    }
//
//    private fun storeRefreshToken(userId: ObjectId, rawRefreshToken: String) {
//        val hashed = hashToken(rawRefreshToken)
//        val expiryMs = jwtService.refreshTokenValidMs
//        val expiryAt = Instant.now().plusMillis(expiryMs)
//
//        refreshTokenRepo.save(
//            RefreshToken(
//                userId = userId,
//                hashedToken = hashed,
//                expiresAt = expiryAt,
//            )
//        )
//    }
//
//    private fun hashToken(token: String): String {
//        val digest = MessageDigest.getInstance("SHA-256")
//        val hashBytes = digest.digest(token.encodeToByteArray())
//        return Base64.getEncoder().encodeToString(hashBytes)
//    }
//}