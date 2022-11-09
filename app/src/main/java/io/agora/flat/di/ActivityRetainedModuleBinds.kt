package io.agora.flat.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import io.agora.flat.common.board.AgoraBoardRoom
import io.agora.flat.common.board.WhiteSyncedState
import io.agora.flat.di.interfaces.BoardRoom
import io.agora.flat.di.interfaces.SyncedClassState

@Module
@InstallIn(ActivityRetainedComponent::class)
abstract class ActivityRetainedModuleBinds {
    @Binds
    abstract fun providerBoardRoom(bind: AgoraBoardRoom): BoardRoom

    @Binds
    abstract fun providerSyncedState(bind: WhiteSyncedState): SyncedClassState
}