package com.fzm.chat.core.data.bean

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/01/20
 * Description:服务器节点列表
 */
class ServerNode(
    val servers: List<Node>,
    val nodes: List<Node>
) : Serializable {

    class Node(
        val name: String,
        val address: String,
    ) : Serializable {

        var id: Int = 0
        var selected: Boolean = false

        val custom: Boolean get() = id != 0

        /**
         * 服务器状态
         */
        var active = false
    }
}