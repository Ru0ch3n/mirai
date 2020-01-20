package net.mamoe.mirai.utils.io

import kotlinx.io.core.*
import kotlinx.io.pool.useInstance
import net.mamoe.mirai.utils.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


object DebugLogger : MiraiLogger by DefaultLogger("Packet Debug").withSwitch()

fun Throwable.logStacktrace(message: String? = null) = DebugLogger.error(message, this)

@MiraiDebugAPI("Low efficiency.")
fun debugPrintln(any: Any?) = DebugLogger.debug(any)

@MiraiDebugAPI("Low efficiency.")
fun String.debugPrint(name: String): String {
    DebugLogger.debug("$name=$this")
    return this
}

@MiraiDebugAPI("Low efficiency.")
fun ByteArray.debugPrint(name: String): ByteArray {
    DebugLogger.debug(name + "=" + this.toUHexString())
    return this
}

@MiraiDebugAPI("Low efficiency.")
fun IoBuffer.debugPrint(name: String): IoBuffer {
    ByteArrayPool.useInstance {
        val count = this.readAvailable(it)
        DebugLogger.debug(name + "=" + it.toUHexString(offset = 0, length = count))
        return it.toIoBuffer(0, count)
    }
}

@MiraiDebugAPI("Low efficiency.")
inline fun IoBuffer.debugCopyUse(block: IoBuffer.() -> Unit): IoBuffer {
    ByteArrayPool.useInstance {
        val count = this.readAvailable(it)
        block(it.toIoBuffer(0, count))
        return it.toIoBuffer(0, count)
    }
}

@MiraiDebugAPI("Low efficiency.")
fun Input.debugDiscardExact(n: Number, name: String = "") {
    DebugLogger.debug("Discarded($n) $name=" + this.readBytes(n.toInt()).toUHexString())
}

@MiraiDebugAPI("Low efficiency.")
fun ByteReadPacket.debugPrint(name: String = ""): ByteReadPacket {
    ByteArrayPool.useInstance {
        val count = this.readAvailable(it)
        DebugLogger.debug("ByteReadPacket $name=" + it.toUHexString(offset = 0, length = count))
        return it.toReadPacket(0, count)
    }
}

/**
 * 备份数据, 并在 [block] 失败后执行 [onFail].
 *
 * 此方法非常低效. 请仅在测试环境使用.
 */
@MiraiDebugAPI("Low efficiency")
@UseExperimental(ExperimentalContracts::class)
inline fun <R> Input.debugIfFail(name: String = "", onFail: (ByteArray) -> ByteReadPacket = { it.toReadPacket() }, block: ByteReadPacket.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        callsInPlace(onFail, InvocationKind.UNKNOWN)
    }
    ByteArrayPool.useInstance {
        val count = this.readAvailable(it)
        try {
            return block(it.toReadPacket(0, count))
        } catch (e: Throwable) {
            DebugLogger.debug("Error in ByteReadPacket $name=" + it.toUHexString(offset = 0, length = count))
            throw e
        }
    }
}