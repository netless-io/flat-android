package io.agora.flat.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import io.agora.flat.common.board.BoardRoomApiImpl
import io.agora.flat.di.interfaces.BoardRoomApi

@Module
@InstallIn(ActivityComponent::class)
abstract class ActivityModuleBinds {
    @Binds
    abstract fun providerBoardRoomApi(bind: BoardRoomApiImpl): BoardRoomApi
}