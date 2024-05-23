package kpring.user.service

import kpring.core.global.exception.ServiceException
import kpring.user.dto.request.CreateUserRequest
import kpring.user.dto.request.UpdateUserProfileRequest
import kpring.user.dto.response.CreateUserResponse
import kpring.user.dto.response.GetUserProfileResponse
import kpring.user.dto.response.UpdateUserProfileResponse
import kpring.user.entity.User
import kpring.user.exception.UserErrorCode
import kpring.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

@Service
@Transactional
class UserServiceImpl(
  private val userRepository: UserRepository,
  private val passwordEncoder: PasswordEncoder,
  private val userValidationService: UserValidationService,
) : UserService {
  @Value("\${file.path.profile-dir}")
  private lateinit var profileImgDirPath: String

  override fun getProfile(userId: Long): GetUserProfileResponse {
    val user = getUser(userId)
    return GetUserProfileResponse(user.id, user.email, user.username)
  }

  override fun updateProfile(
    userId: Long,
    request: UpdateUserProfileRequest,
    multipartFile: MultipartFile,
  ): UpdateUserProfileResponse {
    var newPassword: String? = null
    val profileImgDir = Paths.get(System.getProperty("user.dir")).resolve(profileImgDirPath)
    val user = getUser(userId)

    request.email?.let { handleDuplicateEmail(it) }
    request.newPassword?.let {
      userValidationService.validateUserPassword(request.password, user.password)
      newPassword = passwordEncoder.encode(it)
    }

    if (!multipartFile.isEmpty) {
      saveUploadedFile(multipartFile, userId, profileImgDir)
    }
    user.updateInfo(request, newPassword)

    return UpdateUserProfileResponse(user.email, user.username)
  }

  override fun exitUser(userId: Long): Boolean {
    TODO("Not yet implemented")
  }

  override fun createUser(request: CreateUserRequest): CreateUserResponse {
    val password = passwordEncoder.encode(request.password)

    handleDuplicateEmail(request.email)

    val user =
      userRepository.save(
        User(
          email = request.email,
          password = password,
          username = request.username,
        ),
      )

    return CreateUserResponse(user.id, user.email)
  }

  private fun handleDuplicateEmail(email: String) {
    if (userRepository.existsByEmail(email)) {
      throw ServiceException(UserErrorCode.ALREADY_EXISTS_EMAIL)
    }
  }

  private fun getUser(userId: Long): User {
    return userRepository.findById(userId)
      .orElseThrow { throw ServiceException(UserErrorCode.USER_NOT_FOUND) }
  }

  private fun saveUploadedFile(
    multipartFile: MultipartFile,
    userId: Long,
    dirPath: Path,
  ) {
    if (Files.notExists(dirPath)) {
      Files.createDirectories(dirPath)
    }
    val extension = multipartFile.originalFilename!!.substringAfterLast('.')
    isFileExtensionSupported(extension)

    val uniqueFileName = generateUniqueFileName(userId, extension)
    val filePath = dirPath.resolve(uniqueFileName)
    multipartFile.transferTo(filePath.toFile())
  }

  private fun isFileExtensionSupported(extension: String) {
    val supportedExtensions = listOf("png", "jpg", "jpeg")
    if (extension !in supportedExtensions) {
      throw ServiceException(UserErrorCode.EXTENSION_NOT_SUPPORTED)
    }
  }

  private fun generateUniqueFileName(
    userId: Long,
    extension: String,
  ): String {
    val timeStamp = SimpleDateFormat("yyMMddHHmmss").format(Date())
    return "$timeStamp$userId.$extension"
  }
}
