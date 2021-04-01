package link.netless.flat.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import link.netless.flat.data.AppDataCenter
import link.netless.flat.data.api.UserService
import link.netless.flat.data.model.UserInfo
import link.netless.flat.di.AppModule
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userService: UserService,
    @AppModule.GlobalData private val appDataCenter: AppDataCenter
) {
    fun getUserInfo(): Flow<UserInfo> = flow {
        try {
            emit(userService.getUserInfo().data)
        } catch (e: HttpException) {
            // TODO Wrapper Server Error
            throw e
        } catch (e: Exception) {
            throw e
        }
    }

    fun login(): Flow<Boolean> = flow {
        try {
            appDataCenter.setUserLoggedIn(true)
            emit(true)
        } catch (e: HttpException) {
            throw e
        } catch (e: Exception) {
            throw e
        }
    }

    fun isLoggedIn(): Boolean {
        return appDataCenter.isUserLoggedIn(false)
    }

    fun setLoggedIn(loggedIn: Boolean) {
        return appDataCenter.setUserLoggedIn(loggedIn)
    }
}