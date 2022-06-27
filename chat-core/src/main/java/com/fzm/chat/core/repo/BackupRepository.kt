package com.fzm.chat.core.repo

import com.fzm.chat.core.net.source.BackupDataSource

/**
 * @author zhengjy
 * @since 2021/01/13
 * Description:
 */
class BackupRepository(dataSource: BackupDataSource) : BackupDataSource by dataSource